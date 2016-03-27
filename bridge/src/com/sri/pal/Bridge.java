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
package com.sri.pal;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.decl.impl.CTRDeclUtil;
import com.sri.pal.ShellService.Modules;
import com.sri.pal.common.JFXProcessController;
import com.sri.pal.common.JFXProcessLauncher;
import com.sri.pal.common.JFXProcessListener;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.upgrader.MemoryTypeStorage;
import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.ConstraintResult;
import com.sri.tasklearning.spine.messages.CustomBridgeMessage;
import com.sri.tasklearning.spine.messages.CustomShellMessage;
import com.sri.tasklearning.spine.messages.ExprEvalResult;
import com.sri.tasklearning.spine.messages.Goodbye;
import com.sri.tasklearning.spine.messages.LearnResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.OptionLearnResult;
import com.sri.tasklearning.spine.messages.PingReply;
import com.sri.tasklearning.spine.messages.PingRequest;
import com.sri.tasklearning.spine.messages.PrivilegedMessageType;
import com.sri.tasklearning.spine.messages.ProcessDemoMessage;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.TypeListResult;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.messages.TypeStoreResult;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.spine.util.ReplyWatcher;
import com.sri.tasklearning.spine.util.TypeCache;

/**
 * Entry point for PAL functionality. This class provides access to instances of
 * the other classes which comprise the interface to the PAL system.
 *
 * @author chris
 */
public class Bridge {
    private static final Logger log = LoggerFactory.getLogger(Bridge.class);

    private static final String DEFAULT_CLIENT_NAME = "PALBridge";

    public static final String STORAGE_DIR_PROP = "PAL.storageDir";

    private static final String PAL_DIR = "AdeptTaskLearning";

    /**
     * If ITL isn't available when we start, how long to we keep trying to make
     * contact? (ms)
     */
    private static long connectAttemptTime = 15 * 60 * 1000;
    /**
     * At what interval will we make our connection attempts? If an attempt
     * takes less than this time, we'll wait the remainder. (ms)
     */
    private static long connectRetryTime = 15 * 1000;

    /**
     * Maximum stringified size of actions. In other words, this is the maximum
     * string length we can send via ITL to another JVM. Anything larger is
     * likely to cause an OutOfMemoryException. 0 means no limit.
     */
    private static int maxStringSize = 0;

    private ActionModel actionModel;
    private ActionModelFactory actionModelFactory;
    private BridgeServiceImpl callHandler;
    private ExecutorMap executorMap;
    private GlobalActionNotifier globalNotifier;
    private InvocationCache invocationCache;
    private Learner learner;
    private TypeLoaderPublisher loaderPublisher;
    private PALInstrumentationControl palInstrumentation;
    private ListRunningModulesListener runningModulesListener;
    private ReplyWatcher<SerialNumberResponse> serialGetter;
    private LumenProcedureExecutor sparkExecutor;
    private Spine spine;
    private ReplyWatcher<TypeStoreResult> storageWatcher;
    private TypeCache typeCache;
    private ReplyWatcher<TypeListResult> typeListWatcher;
    private ReplyWatcher<TypeResult> typeQueryWatcher;
    private ReplyWatcher<LearnResult> learnReceiver;
    private ReplyWatcher<OptionLearnResult> learnOptionReceiver;
    private ReplyWatcher<ConstraintResult> constraintReceiver;
    private ReplyWatcher<ProcessDemoMessage> idiomReceiver;
    private ReplyWatcher<ExprEvalResult> exprEvalReceiver;
    private TypeStorage typeStorage;
    private CancelReceiver cancelReceiver;

    private boolean storageAssigned = false;

    /**
     * Starts the PAL learning components, if they are not already running.
     *
     * @throws PALException
     *             if any of the configured components cannot be started
     */
    public static void startPAL()
            throws PALException {
        TLSystemManager.ensureStarted();
    }

    /**
     * Creates and initializes an instance of the PAL Bridge. This does not
     * start the PAL learning components; it only initializes a connection to
     * them.
     * <p>
     * This method is deprecated because it uses a default client name, making
     * name collisions likely.
     *
     * @return a newly initialized Bridge
     */
    @Deprecated
    public static Bridge newInstance()
            throws PALException {
        return newInstance(DEFAULT_CLIENT_NAME);
    }

    /**
     * Creates and initializes an instance of the PAL Bridge. This does not
     * start the PAL learning components; it only initializes a connection to
     * them.
     *
     * @param appName
     *            a string to uniquely identify this instance of the Bridge to
     *            the other PAL components. If <code>null</code>, a unique name
     *            will be chosen.
     * @return a newly initialized Bridge
     */
    public static Bridge newInstance(String appName)
            throws PALException {
        log.info("Starting PAL Bridge with client ID {}", appName);

        // Decl keeps a list of all declarations done through CTR if setCache is
        // true. Later it may give a warning if something is overwriting an
        // older declaration. We keep it off to avoid a memory leak.
        CTRDeclUtil.setCache(false);

        Spine spine = connectSpine(appName);
        Bridge bridge = new Bridge();

        bridge.spine = spine;
        bridge.globalNotifier = new GlobalActionNotifier();
        bridge.loaderPublisher = new TypeLoaderPublisher(bridge);
        bridge.palInstrumentation = new PALInstrumentationControl(spine);
        bridge.cancelReceiver = new CancelReceiver(bridge);

        bridge.invocationCache = new InvocationCache();

        bridge.serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, spine);
        bridge.storageWatcher = new ReplyWatcher<TypeStoreResult>(
                TypeStoreResult.class, spine);
        bridge.typeListWatcher = new ReplyWatcher<TypeListResult>(
                TypeListResult.class, spine);
        bridge.typeQueryWatcher = new ReplyWatcher<TypeResult>(
                TypeResult.class, spine);

        bridge.executorMap = new ExecutorMap(bridge);
        bridge.actionModel = new ActionModel(bridge);
        bridge.actionModelFactory = new ActionModelFactory(bridge);
        ActionExecAdapter actionExecAdapter = new ActionExecAdapter(bridge);
        bridge.sparkExecutor = new LumenProcedureExecutor(bridge);
        bridge.learnReceiver = new ReplyWatcher<LearnResult>(LearnResult.class,
                spine);
        bridge.learnOptionReceiver = new ReplyWatcher<OptionLearnResult>(
                OptionLearnResult.class, spine);
        bridge.constraintReceiver = new ReplyWatcher<ConstraintResult>(
                ConstraintResult.class, spine);
        bridge.idiomReceiver = new ReplyWatcher<ProcessDemoMessage>(
                ProcessDemoMessage.class, spine);
        bridge.exprEvalReceiver = new ReplyWatcher<ExprEvalResult>(ExprEvalResult.class, spine);
        bridge.learner = new Learner(bridge);
        Thread t = new Thread(bridge.globalNotifier);
        t.setDaemon(true);
        t.setName(bridge.globalNotifier.getClass().getSimpleName());
        t.start();
        ITLExecutionStatusListener statListener = new ITLExecutionStatusListener(
                bridge);
        try {
            bridge.typeCache = new TypeCache(spine);
            spine.subscribe(statListener,
                    UserMessageType.EXECUTION_STATUS);
            spine.subscribe(actionExecAdapter,
                    UserMessageType.EXECUTE_REQUEST);
            spine.subscribe(bridge.executorMap,
                    UserMessageType.EXECUTOR_LIST_QUERY);
            spine.subscribe(bridge.learnReceiver, UserMessageType.LEARN_RESULT);
            spine.subscribe(bridge.learnOptionReceiver,
                    UserMessageType.LEARN_OPTION_RESULT);
            spine.subscribe(bridge.constraintReceiver,
                    UserMessageType.CONSTRAINT_RESULT);
            spine.subscribe(bridge.idiomReceiver, UserMessageType.PROCESS_DEMO);
            spine.subscribe(bridge.typeListWatcher,
                    UserMessageType.TYPE_LIST_RESULT);
            spine.subscribe(bridge.typeQueryWatcher, UserMessageType.TYPE_RESULT);
            spine.subscribe(bridge.storageWatcher,
                    UserMessageType.TYPE_STORE_RESULT);
            spine.subscribe(bridge.serialGetter,
                    SystemMessageType.SERIAL_NUMBER_RESPONSE);
            spine.subscribe(bridge.palInstrumentation,
                    SystemMessageType.START_WATCHING,
                    SystemMessageType.STOP_WATCHING);
            spine.subscribe(bridge.cancelReceiver, UserMessageType.CANCEL);
            spine.subscribe(bridge.exprEvalReceiver, UserMessageType.EXPR_EVAL_RESULT);
        } catch (SpineException e) {
            throw new PALRemoteException("Task execution subscribe failed", e);
        }

        bridge.runningModulesListener = new ListRunningModulesListener();

        bridge.callHandler = bridge.new BridgeServiceImpl();
        try {
            spine.subscribe(bridge.runningModulesListener,
                    UserMessageType.CUSTOM_BRIDGE_MESSAGE);
            spine.subscribe(bridge.callHandler,
                    UserMessageType.PING_REQUEST, UserMessageType.GOODBYE);
        } catch (SpineException e1) {
            throw new PALRemoteException("Failed to subscribe bridge to spine",
                    e1);
        }

        return bridge;
    }

    /**
     * Builds an offline Bridge which is suitable only for letting ActionModel
     * load action model XML files.
     */
    static Bridge offlineInstance()
            throws SpineException {
        Bridge bridge = new Bridge();
        bridge.actionModel = new ActionModel(bridge);
        bridge.actionModelFactory = new ActionModelFactory(bridge);
        bridge.typeCache = new TypeCache(null);
        bridge.typeStorage = new MemoryTypeStorage();
        bridge.loaderPublisher = new TypeLoaderPublisher(new MemoryTypeStorage());

        return bridge;
    }

    /**
     * Sets the interval at which PAL connection attempts will be made. The
     * Bridge API will repeatedly attempt to connect to the PAL backend process.
     * Connection attempts will be separated by this many seconds. If a failed
     * connection attempt took less time than this, the Bridge API will wait the
     * remainder before trying again.
     *
     * @param seconds
     *            the number of seconds between connection attempts
     * @see #setConnectAttemptDuration
     */
    public static void setConnectRetryInterval(int seconds) {
        connectRetryTime = seconds * 1000;
    }

    /**
     * Sets the total period of time the Bridge API will attempt to connect to
     * the PAL backend. Until this much time has passed, the Bridge API will
     * continue to retry; once this time has elapsed, the next failed connection
     * attempt will result in an exception being thrown by {@link #newInstance}.
     *
     * @param seconds
     *            the number of seconds to attempt to connect to PAL
     * @see #setConnectRetryInterval
     */
    public static void setConnectAttemptDuration(int seconds) {
        connectAttemptTime = seconds * 1000;
    }

    /**
     * Repeatedly try to connect to ITL (and the PAL components through it).
     * Keep trying until connectAttemptTime ms have passed. Try every
     * connectRetryTime ms, or less frequently if a connect attempt takes longer
     * than that. If we give up, throw the most recently caught exception.
     *
     * @return the ITLClient
     * @throws PALException
     *             if there is a problem connecting
     */
    private static Spine connectSpine(String clientName)
            throws PALException {
        long connectStart = System.currentTimeMillis();
        while (true) {
            Spine spine;
            long attemptStart = System.currentTimeMillis();
            Exception error;
            try {
                spine = new JmsSpine(JmsClient.REMOTE, clientName);
                return spine;
            } catch (Exception e) {
                log.debug("Unable to connect to Spine", e);
                error = e;
            }

            if (connectStart + connectAttemptTime < System.currentTimeMillis()) {
                log.error("Unable to connect to Spine", error);
                throw new PALException("Unable to connect to Spine", error);
            }

            long nextConnect = attemptStart + connectRetryTime;
            long sleepTime = nextConnect - System.currentTimeMillis();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    String msg = "Interrupted waiting for connect retry";
                    log.error(msg, e);
                    throw new PALException(msg, e);
                }
            }
        }
    }

    private Bridge() {
    }

    /**
     * Sets an upper limit on the size of data objects which can be passed from
     * the PAL Bridge to other PAL components. A size of 0, the default, means
     * no checking will be done. The intent of this facility is to prevent the
     * PAL components, which may be running in a JVM with a smaller heap size,
     * from experiencing an OutOfMemoryError.
     * <p>
     * This method can be called at any time, including before PAL is
     * initialized. The new value will be used for any future communication with
     * the PAL components.
     *
     * @param approxSize
     *            the size, in characters, to limit parameters to. Remember that
     *            a Java character is typically 16 bits, not 8.
     */
    public static void setMaxStringSize(int approxSize) {
        maxStringSize = approxSize;
    }

    static void checkStringSize(int size)
            throws PALSizeException {
        if (maxStringSize == 0) {
            return;
        }

        if (size > maxStringSize) {
            throw new PALSizeException("Actual size " + size
                    + " greater than max size " + maxStringSize);
        }
    }

    /**
     * Registers a listener to receive notifications of all actions performed in
     * the PAL system.
     *
     * @param listener
     *            the listener to register
     */
    public void addActionListener(GlobalActionListener listener) {
        globalNotifier.addListener(listener);
    }

    /**
     * Registers a persistence mechanism which can store types, actions, and/or
     * procedures. Only one {@code TypeStorage} may be registered at a time
     * across the PAL system.
     *
     * @param storage
     *            the new persistence mechanism to register
     * @return {@code true} if the registration was successful; {@code false} if
     *         not, probably due to another {@code TypeStorage} already being
     *         registered
     * @throws PALRemoteException
     *             if the registration generates an error
     * @see TypeStorage
     */
    public synchronized boolean setTypeStorage(TypeStorage storage)
            throws PALRemoteException {
        if (storageAssigned) {
            /*
             * It's already set locally, so there's no point in asking the
             * Spine; we've already registered ours.
             */
            return false;
        }
        log.debug("setting TypeStorage to {}", storage);
        TypeQueryReceiver tqr = new TypeQueryReceiver(spine, loaderPublisher);
        boolean result;
        try {
            result = spine.subscribe(tqr,
                    PrivilegedMessageType.TYPE_STORE_REQUEST);
            if (result) {
                spine.subscribe(tqr, UserMessageType.TYPE_QUERY);
                spine.subscribe(tqr, UserMessageType.TYPE_LIST_QUERY);
            }
        } catch (SpineException e) {
            throw new PALRemoteException(e);
        }
        if (result) {
            log.debug("Successfully set storage to {}", storage);
            typeStorage = storage;
        } else {
            log.debug("Failed to set storage to {}; falling back to remote",
                    storage);
            typeStorage = null;
        }
        storageAssigned  = true;
        return result;
    }

    /**
     * Retrieves and possibly sets the current type storage implementation. If
     * there is no type storage, this method will create a default
     * implementation of {@link FileTypeStorage}.
     *
     * @return the current storage implementation
     * @throws PALException
     *             if the registration generates an error
     * @see TypeStorage
     */
    public synchronized TypeStorage getTypeStorage()
            throws PALException {
        if (typeStorage == null) {
            log.debug("No type storage; building the default one");
            File storageDir;
            String storageDirStr = System.getProperty(STORAGE_DIR_PROP);
            if (storageDirStr != null) {
                log.debug("Got {} from reading {}", storageDirStr,
                        STORAGE_DIR_PROP);
                storageDir = new File(storageDirStr);
            } else {
                log.debug("{} not set", STORAGE_DIR_PROP);
                String userDirStr = System.getenv("APPDATA");
                if (userDirStr != null) {
                    log.debug("App data (Windows) is {}", userDirStr);
                } else {
                    log.debug("App data (Windows) is not set");
                    userDirStr = System.getProperty("user.home");
                    log.debug("User home is {}", userDirStr);
                }
                File userDir = new File(userDirStr);
                storageDir = new File(userDir, PAL_DIR);
            }
            TypeStorage storage = new FileTypeStorage(storageDir,
                    spine.getClientId());
            boolean success = setTypeStorage(storage);
            if (success) {
                log.info("Default type storage will use {}",
                        storageDir.getAbsolutePath());
            } else {
                log.info("Using remote type storage.");
            }
        }
        return typeStorage;
    }

    /**
     * Provides access to the action model, which in turn allows access to the
     * set of known types and actions.
     *
     * @return the action model which corresponds to this Bridge
     */
    public ActionModel getActionModel() {
        return actionModel;
    }

    ReplyWatcher<SerialNumberResponse> getSerialGetter() {
        return serialGetter;
    }

    /**
     * Provides access to the Bridge's internal communications component. This
     * is for use only by the Editor; other software should not use it.
     */
    public Spine getSpine() {
        return spine;
    }


    ActionModelFactory getActionModelFactory() {
        return actionModelFactory;
    }

    GlobalActionNotifier getGlobalNotifier() {
        return globalNotifier;
    }

    InvocationCache getInvocationCache() {
        return invocationCache;
    }

    ExecutorMap getExecutorMap() {
        return executorMap;
    }

    TypeCache getTypeCache() {
        return typeCache;
    }

    ReplyWatcher<TypeStoreResult> getTypeStoreWatcher() {
        return storageWatcher;
    }

    ReplyWatcher<TypeListResult> getTypeListWatcher() {
        return typeListWatcher;
    }

    ReplyWatcher<TypeResult> getTypeQueryWatcher() {
        return typeQueryWatcher;
    }

    ReplyWatcher<LearnResult> getLearnReceiver() {
        return learnReceiver;
    }

    ReplyWatcher<OptionLearnResult> getLearnOptionReceiver() {
        return learnOptionReceiver;
    }

    ReplyWatcher<ConstraintResult> getConstraintReceiver() {
        return constraintReceiver;
    }

    ReplyWatcher<ProcessDemoMessage> getIdiomReceiver() {
        return idiomReceiver;
    }

    ReplyWatcher<ExprEvalResult> getExprEvalReceiver() {
        return exprEvalReceiver;
    }

    /**
     * Used internally by the editor to get a reference to the publisher that
     * sends requests to {@link TypeStorage} objects.
     *
     * @return the publisher of action load requests
     */
    public TypeLoaderPublisher getActionLoaderPublisher() {
        return loaderPublisher;
    }

    /**
     * Provides the instrumentation used by the PAL system. This instrumentation
     * handles &quot;start watching&quot; and &quot;stop watching&quot; requests
     * from the application.
     *
     * @return this Bridge's instrumentation
     */
    public InstrumentationControl getPalInstrumentation() {
        return palInstrumentation;
    }

    /**
     * Adds an instrumentation listener to the set of those which will get
     * notified of instrumentation system state changes. For a longer
     * explanation, see {@link InstrumentationControl}.
     *
     * @param instr
     *            a new instrumentation event listener which will receive
     *            start/stop watching events
     */
    public void addApplicationInstrumentation(InstrumentationControl instr) {
        palInstrumentation.addApplicationInstrumentation(instr);
    }

    /**
     * Removes an instrumentation listener.
     *
     * @param instr
     *            the listener to remove
     * @see #addApplicationInstrumentation
     */
    public void removeApplicationInstrumentation(InstrumentationControl instr) {
        palInstrumentation.removeApplicationInstrumentation(instr);
    }

    /**
     * Provides the Learner component, which is capable of learning new
     * procedures.
     *
     * @return this Bridge's learner
     */
    public Learner getLearner() {
        return learner;
    }

    /**
     * Provides a reference to the PAL procedure executor (lumen), so
     * that it can be used to load CTR-S procedures.
     *
     * @return the PAL procedure executor
     */
    public ProcedureExecutor getPALExecutor() {
        return sparkExecutor;
    }

    /**
     * Provides a listing of the modules which are currently running in the PAL
     * Shell process.
     *
     * @return a list of running modules
     * @throws PALRemoteException
     *             if the PAL Shell cannot be contacted
     */
    @SuppressWarnings("unchecked")
    public List<Modules> listRunningModules()
            throws PALRemoteException {
        try {
            spine.send(new CustomShellMessage(spine.getClientId(),
                    spine.getNextUid(), "listRunningModules"));
        } catch (SpineException e) {
            throw new PALRemoteException(e);
        }

        CustomBridgeMessage message = null;
        int time = 0, maxTime = 5000, tick = 10;
        while (message == null && time < maxTime) {
            message = runningModulesListener.poll();
            try {
                Thread.sleep(tick);
            } catch (InterruptedException e) {
                // Ignore.
            }
            time += tick;
        }
        if (message == null || !List.class.isInstance(message.getPayload())) {
            throw new PALRemoteException(
                    "Unable to retrieve running modules list");
        }
        return (List<Modules>) message.getPayload();
    }

	/**
         * Starts the Adept Editor UI and requests it to edit the given procedure.
         *
         * @param jarPath
     *            the path to the ui-editor.jar
         * @param procToEdit
         *            the name of the procedure to edit
         * @param listener
         *            a callback listener for when the process ends
         * @return the process that was started
         */
	public JFXProcessController startAdeptEditor(final File jarPath, final SimpleTypeName procToEdit,
			final JFXProcessListener listener) {

		final JFXProcessLauncher pman = new JFXProcessLauncher(System.out, jarPath,
				procToEdit.getFullName());
		pman.addProcessListener(listener);
		pman.startProcess();
		return pman;
	}

    /**
     * Disconnects this Bridge instance from the rest of the PAL system. This
     * does not attempt to shut down the learning system.
     *
     * @throws PALRemoteException
     *             if the disconnect request cannot be sent
     * @see #shutdown
     */
    public void disconnect()
            throws PALRemoteException {
        String name = null;
        try {
            name = spine.getClientId();
        } catch (Exception e) {
            log.warn("Couldn't get Spine client id", e);
        }
        try {
            spine.shutdown(true);
        } catch (Exception e) {
            log.warn("Trouble shutting down JmsSpine for " + name, e);
        }
    }

    /**
     * Requests the PAL process to exit.
     *
     * @throws SpineException
     *             if the PAL process can't be contacted
     * @see #disconnect
     */
    public void shutdown()
            throws PALRemoteException {
        log.debug("About to shut down PALBridge");

        try {
            spine.shutdownMaster();
        } catch (Exception e) {
            log.warn(
                    "Shutting down the PALBridge JmsSpine Instance experienced difficulty: {}",
                    e);
        }
    }

    @Override
    public int hashCode() {
        return spine.getClientId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    private static class ListRunningModulesListener
            implements MessageHandler {
        private Queue<CustomBridgeMessage> modulesList = new LinkedList<CustomBridgeMessage>();

        @Override
        public void handleMessage(Message message)
                throws MessageHandlerException {
            if (CustomBridgeMessage.class.isInstance(message)) {
                CustomBridgeMessage customBridgeMessage = (CustomBridgeMessage) message;

                if (List.class.isInstance(customBridgeMessage.getPayload())) {
                    modulesList.add(customBridgeMessage);
                }
            }
        }

        public CustomBridgeMessage poll() {
            if (modulesList.isEmpty()) {
                return null;
            } else {
                return modulesList.poll();
            }
        }

    }

    private class BridgeServiceImpl
            implements MessageHandler {
        @Override
        public void handleMessage(Message message)
                throws MessageHandlerException {
            if (PingRequest.class.isInstance(message)) {
                PingRequest pingRequest = (PingRequest) message;

                // The ping message originates in the CpofWatchdog/ShellBridge and is used to
                // determine if cpof is still running. To provide this information, one of the
                // Bridge instances, the PALBridge intercepts the ping and sends back a ping
                // reply message with the same transactionUid as the ping request.
                PingReply pingReply = new PingReply(spine.getClientId(),
                        pingRequest.getUid());
                try {
                    boolean receiver = spine.send(pingReply);
                    if (!receiver) {
                        log.warn("No consumer for PING_REPLY subscribed");
                    }
                } catch (SpineException e) {
                    throw new MessageHandlerException(e);
                }
                PALStatusMonitor.currentStatus(PALStatusListener.Status.WATCHDOG_UP);
            } else if (Goodbye.class.isInstance(message)) {
                PALStatusMonitor.currentStatus(PALStatusListener.Status.DOWN);
            }
        }
    }
}
