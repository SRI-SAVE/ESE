/*
 * Copyright 2016 SRI International
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sri.tasklearning.lapdogController;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.impl.CTRActionDeclaration;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.training.core.exercise.Option;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.MediatorsException;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.mediators.WithLockedTypes;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.LearnRequest;
import com.sri.tasklearning.spine.messages.LearnResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.OptionLearnRequest;
import com.sri.tasklearning.spine.messages.OptionLearnResult;
import com.sri.tasklearning.spine.messages.ProcessDemoMessage;
import com.sri.tasklearning.spine.messages.TypeListQuery;
import com.sri.tasklearning.spine.messages.TypeListQuery.Subset;
import com.sri.tasklearning.spine.messages.TypeListResult;
import com.sri.tasklearning.spine.messages.TypeQuery;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeUtil;
import com.sri.tasklearning.util.NamedThreadFactory;

/**
 * Implements the LAPDOG service, which is callable by other tasklearning
 * components.
 */
public class LapdogClientCallServiceImpl
        implements MessageHandler {
    private static final Logger log = LoggerFactory
            .getLogger(LapdogClientCallServiceImpl.class);

    public static final String AGAVE_NAMESPACE = "agave";
    public static final String AGAVE_VERSION = "1.0";

    private final LapdogClient lapdogClient;
    private final LockingActionModel actionModel;
    private final Spine spine;
    private final ExecutorService threadPool;
    private final ErrorFactory errorFactory;
    private final List<SimpleTypeName> agaveActions;
    private final ReplyWatcher<TypeResult> typeQueryWatcher;
    private final ReplyWatcher<TypeListResult> typeListWatcher;
    private final WithLockedTypes withLockedTypes;
    private final TypeFetcher typeFetcher;
    private boolean agaveDone = false;
    private boolean idiomsDone = false;

    LapdogClientCallServiceImpl(LapdogClient lc,
                                Spine spine)
            throws SpineException {
        lapdogClient = lc;
        actionModel = new LockingActionModel(lc.getTypeFacade());
        this.spine = spine;
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newFixedThreadPool(1, tf);
        typeQueryWatcher = new ReplyWatcher<TypeResult>(TypeResult.class, spine);
        spine.subscribe(typeQueryWatcher, UserMessageType.TYPE_RESULT);
        typeListWatcher = new ReplyWatcher<TypeListResult>(
                TypeListResult.class, spine);
        spine.subscribe(typeListWatcher, UserMessageType.TYPE_LIST_RESULT);
        typeFetcher = new TypeFetcher(spine, actionModel, typeQueryWatcher);
        withLockedTypes = new WithLockedTypes(actionModel);
        errorFactory = new ErrorFactory(spine.getClientId());

        agaveActions = new ArrayList<SimpleTypeName>();
        agaveActions.add((SimpleTypeName) TypeNameFactory.makeName("string",
                AGAVE_VERSION, AGAVE_NAMESPACE));
        agaveActions.add((SimpleTypeName) TypeNameFactory.makeName("url",
                AGAVE_VERSION, AGAVE_NAMESPACE));
        agaveActions.add((SimpleTypeName) TypeNameFactory.makeName(
                "stringlist", AGAVE_VERSION, AGAVE_NAMESPACE)); // NOTE Comment
// out if LAPDOG/Pecari performance problems ensue
        agaveActions.add((SimpleTypeName) TypeNameFactory.makeName(
                "findLinkRef", AGAVE_VERSION, AGAVE_NAMESPACE));
        agaveActions.add((SimpleTypeName) TypeNameFactory.makeName(
                "findLinkName", AGAVE_VERSION, AGAVE_NAMESPACE));
        agaveActions
                .add((SimpleTypeName) TypeNameFactory.makeName(
                        "createStringFromFormatString", AGAVE_VERSION,
                        AGAVE_NAMESPACE)); // NOTE Comment out if LAPDOG/Pecari
// performance problems ensue
        agaveActions
                .add((SimpleTypeName) TypeNameFactory.makeName(
                        "createFormatStringFromString", AGAVE_VERSION,
                        AGAVE_NAMESPACE)); // NOTE Comment out if LAPDOG/Pecari
// performance problems ensue
    }

    private synchronized void init()
            throws SpineException,
            MediatorsException {
        lapdogClient.waitForInit();
        loadAgaveActions();
        loadIdioms();
    }

    @Override
    public void handleMessage(Message message) {
        if (message instanceof LearnRequest) {
            LearnRequest learnMsg = (LearnRequest) message;
            threadPool.execute(new LearnThread(learnMsg));
        } else if (message instanceof ProcessDemoMessage) {
            if (message.getSender().equals(spine.getClientId())) {
                log.debug("Ignoring message from self: {}", message);
                return;
            }
            ProcessDemoMessage idiomMsg = (ProcessDemoMessage) message;
            threadPool.execute(new IdiomThread(idiomMsg));
        } else if (message instanceof OptionLearnRequest) {
            OptionLearnRequest learnMsg = (OptionLearnRequest) message;
            threadPool.execute(new OptionLearnThread(learnMsg));
        } else {
            log.warn("Unexpected message ({}): {}", message.getClass(), message);
            return;
        }
    }

    /**
     * Used by the tests only.
     */
    String learn(ATRDemonstration demonstration,
                 Properties learnProps,
                 Set<TypeName> extraTypes,
                 String taskName)
            throws SpineException,
            MediatorsException {
        init();
        LearnAction action = new LearnAction(taskName, learnProps);        
        DemonstrationDependencyFinder finder = new DemonstrationDependencyFinder(
                typeFetcher);
        finder.addTypes(extraTypes);
        withLockedTypes.lockedAction(action, demonstration, finder);
        return action.result();
    }

    /**
     * Used by the tests only.
     */
    Option learnOption(ATRDemonstration demo)
            throws SpineException,
            MediatorsException {
        init();
        OptionLearnAction action = new OptionLearnAction();
        DemonstrationDependencyFinder finder = new DemonstrationDependencyFinder(
                typeFetcher);
        withLockedTypes.lockedAction(action, demo, finder);
        return action.result();
    }

    // Before learning, try to load the Agave actions into the action model.
    // Don't bother unloading them.
    private synchronized void loadAgaveActions()
            throws SpineException,
            MediatorsException {
        if (!agaveDone) {
            DemonstrationDependencyFinder finder = new DemonstrationDependencyFinder(
                    typeFetcher);
            for (SimpleTypeName typeName : agaveActions) {
                List<ATRDecl> loaded = new ArrayList<ATRDecl>();
                finder.getRequiredTypes(loaded, typeName, false);
                for (ATRDecl type : loaded) {
                    if (actionModel.getRaw(typeName) == null && type != null) {
                        actionModel.add(type);
                    }
                }
            }
        }
        agaveDone = true;
    }

    private void loadIdioms()
            throws SpineException,
            MediatorsException {
        if(!idiomsDone) {
            TransactionUID uid = spine.getNextUid();
            TypeListQuery query = new TypeListQuery(spine.getClientId(), uid,
                    Subset.IDIOM);
            TypeListResult listResult = typeListWatcher.sendAndGetReply(query);
            DemonstrationDependencyFinder finder = new DemonstrationDependencyFinder(
                    typeFetcher);
            for(SimpleTypeName name : listResult.getTypeNames()) {
                /*
                 * If the idiom definition depends on other actions (or types),
                 * we need to send those to LAPDOG first. In this case we'll get
                 * locks on the dependent types and never release them, because
                 * we won't ever remove the idiom definitions.
                 */
                TypeQuery tq = new TypeQuery(spine.getClientId(), name, spine.getNextUid());
                TypeResult result = typeQueryWatcher.sendAndGetReply(tq);
                ATRDecl idiom = result.getTypeAtr();
                if (!TypeUtil.isIdiom(idiom))
                    continue;
                
                List<ATRDecl> reqTypes = new ArrayList<ATRDecl>();
                finder.getRequiredTypes(reqTypes, name, true);
                for (ATRDecl decl : reqTypes) {
                    actionModel.getReadLock(TypeUtil.getName(decl));
                    actionModel.add(decl);
                }
            }
            idiomsDone = true;
        }
    }

    private LapdogFacade getLapdogFacade() {
        return lapdogClient.getLapdogFacade();
    }

    /**
     * This thread is responsible for processing a learn request to turn a
     * demonstration into a procedure.
     */
    private class LearnThread
            implements Runnable {
        private final LearnRequest learnMsg;

        public LearnThread(LearnRequest learnMsg) {
            this.learnMsg = learnMsg;
        }

        @Override
        public void run() {
            ATRDemonstration demonstration = learnMsg.getDemonstration();
            String taskName = learnMsg.getName().getFullName();
            Properties learnProps = learnMsg.getLearnProps();            
            Set<TypeName> extraTypes = learnMsg.getExtraTypes();
            TransactionUID uid = learnMsg.getUid();
            LearnAction action = new LearnAction(taskName, learnProps);            
            DemonstrationDependencyFinder finder = new DemonstrationDependencyFinder(
                    typeFetcher);
            finder.addTypes(extraTypes);

            ErrorInfo error;
            try {
                init();
                withLockedTypes.lockedAction(action, demonstration, finder);
                error = action.getError();
            } catch (Exception e) {
                log.warn("Learning failed for " + taskName, e);
                error = errorFactory.error(ErrorType.LAPDOG, e);
            } catch (Error e) {
                log.error("Fatal error in LAPDOG thread", e);
                System.exit(1);
                throw e;
            }

            LearnResult reply;
            if (error != null) {
                reply = new LearnResult(spine.getClientId(), uid, error);
            } else {
                String result = action.result();
                reply = new LearnResult(spine.getClientId(), uid, result);
            }

            try {
                spine.send(reply);
            } catch (SpineException e) {
                log.warn("Couldn't send learn result message: " + reply, e);
            }
        }
    }

    /**
     * This thread handles an idiom recognition request to process a
     * demonstration and return a possibly modified one.
     */
    private class IdiomThread
            implements Runnable {
        private final ProcessDemoMessage idiomMsg;

        public IdiomThread(ProcessDemoMessage msg) {
            idiomMsg = msg;
        }

        @Override
        public void run() {
            ATRDemonstration demonstration = idiomMsg.getDemonstration();
            TransactionUID uid = idiomMsg.getUid();
            IdiomAction action = new IdiomAction();
            DemonstrationDependencyFinder finder = new DemonstrationDependencyFinder(
                    typeFetcher);

            ErrorInfo error;
            try {
                init();
                withLockedTypes.lockedAction(action, demonstration, finder);
                error = action.getError();
            } catch (Exception e) {
                log.warn("Idiom recognition failed for " + demonstration, e);
                error = errorFactory.error(ErrorType.LAPDOG, e);
            }

            ProcessDemoMessage reply;
            if (error != null) {
                reply = new ProcessDemoMessage(spine.getClientId(), error,
                        uid);
            } else {
                ATRDemonstration newDemo = action.result();
                reply = new ProcessDemoMessage(spine.getClientId(),
                        newDemo, uid);
            }

            try {
                spine.send(reply);
            } catch (SpineException e) {
                log.warn("Couldn't send learn result message: " + reply, e);
            }
        }
    }

    /**
     * Processes an OptionLearnRequest to turn a demonstration into a task
     * solution (AKA gold standard).
     */
    private class OptionLearnThread
            implements Runnable {
        private final OptionLearnRequest request;

        public OptionLearnThread(OptionLearnRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            ATRDemonstration demo = request.getDemonstration();
            TransactionUID uid = request.getUid();
            OptionLearnAction action = new OptionLearnAction();
            DemonstrationDependencyFinder finder = new DemonstrationDependencyFinder(
                    typeFetcher);

            ErrorInfo error;
            try {
                init();
                withLockedTypes.lockedAction(action, demo, finder);
                error = action.getError();
            } catch (Exception e) {
                log.warn("Option learning failed", e);
                error = errorFactory.error(ErrorType.LAPDOG, e);
            } catch (Error e) {
                log.error("Fatal error in LAPDOG thread", e);
                System.exit(1);
                throw e;
            }

            OptionLearnResult reply;
            if (error != null) {
                reply = new OptionLearnResult(spine.getClientId(), uid, error);
            } else {
                Option option = action.result();
                try {
                    reply = new OptionLearnResult(spine.getClientId(), uid,
                            option);
                } catch (JAXBException e) {
                    log.info("Couldn't build result", e);
                    error = errorFactory.error(ErrorType.LAPDOG, e);
                    reply = new OptionLearnResult(spine.getClientId(), uid,
                            error);
                }
            }

            try {
                spine.send(reply);
            } catch (SpineException e) {
                log.warn("Couldn't send option learn result message: " + reply,
                        e);
            }
        }
    }

    /**
     * This action is called by {@link WithLockedTypes} to handle the details of
     * procedure learning.
     */
    private class LearnAction
            extends WithLockedTypes.Action<String, ATRDemonstration> {
        private final String taskName;
        private final Properties learnProps;
        private String result;
        private ErrorInfo error;

        public LearnAction(String name,
                           Properties learnProps) {
            taskName = name;
            this.learnProps = learnProps;
        }

        @Override
        public void run(ATRDemonstration demo,
                        List<ATRDecl> requiredTypes,
                        Runnable cleanup) {
            try {
                CTRActionDeclaration learnedProc = getLapdogFacade().learn(
                         demo, learnProps, taskName);
               result = ATRSyntax.toSource(learnedProc);
            } catch (Exception e) {
                log.warn("Learning failed for " + taskName, e);
                error = errorFactory.error(ErrorType.LAPDOG, e);
            } finally {
                cleanup.run();
            }
        }

        @Override
        public String result() {
            return result;
        }

        @Override
        public ErrorInfo getError() {
            return error;
        }
    }

    /**
     * This action is called by {@link WithLockedTypes} to handle the details of
     * idiom recognition.
     */
    private class IdiomAction
            extends WithLockedTypes.Action<ATRDemonstration, ATRDemonstration> {
        private ATRDemonstration result;
        private ErrorInfo error;

        @Override
        public void run(ATRDemonstration demo,
                        List<ATRDecl> requiredTypes,
                        Runnable cleanup) {
            try {
                result = lapdogClient.getLapdogFacade().recognizeIdiom(demo);
            } catch (Exception e) {
                log.warn("Idiom recognition failed", e);
                error = errorFactory.error(ErrorType.LAPDOG, e);
            } finally {
                cleanup.run();
            }
        }

        @Override
        public ATRDemonstration result() {
            return result;
        }

        @Override
        public ErrorInfo getError() {
            return error;
        }
    }

    /**
     * This action is called by {@link WithLockedTypes} to handle the details of
     * option learning.
     */
    private class OptionLearnAction
            extends
            WithLockedTypes.Action<Option, ATRDemonstration> {
        private Option result;
        private ErrorInfo error;

        @Override
        public void run(ATRDemonstration demo,
                        List<ATRDecl> requiredTypes,
                        Runnable cleanupTask) {
            try {
                result = getLapdogFacade().learnOption(demo);
            } catch (Exception e) {
                log.warn("Option learning failed", e);
                error = errorFactory.error(ErrorType.LAPDOG, e);
            } finally {
                cleanupTask.run();
            }
        }

        @Override
        public ErrorInfo getError() {
            return error;
        }

        @Override
        public Option result() {
            return result;
        }
    }

    void shutdown() {
        try {
            spine.unsubscribe(UserMessageType.TYPE_RESULT);
            spine.unsubscribe(UserMessageType.TYPE_LIST_RESULT);
        } catch (Exception e) {
            // Do nothing.
        }
        threadPool.shutdown();
        actionModel.shutdown();
        typeQueryWatcher.shutdown();
        typeListWatcher.shutdown();
        typeFetcher.shutdown();
    }
}
