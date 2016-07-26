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

import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.impl.CTRActionDeclaration;
import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.ai.lumen.atr.learning.ATRIdiomTemplate;
import com.sri.ai.lumen.atr.learning.ATRIdiomTemplateAction;
import com.sri.ai.lumen.atr.learning.impl.ATRIdiomTemplateImpl;
import com.sri.ai.lumen.atr.learning.impl.IdiomTemplateFactory;
import com.sri.ai.lumen.atr.term.ATRList;
import com.sri.ai.lumen.atr.term.ATRLiteral;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.ai.tasklearning.lapdog.*;
import com.sri.ai.tasklearning.lapdog.Lapdog.TypeError;
import com.sri.pal.training.core.exercise.Option;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Will Haines
 *
 *         Encapsulates access to LAPDOG.
 */
public class LapdogFacade {
    private static final Logger _logger = LoggerFactory
            .getLogger(LapdogFacade.class);

    public static final String AGAVE_CLASS = "com.sri.ai.tasklearning.lapdog.agave.Agave";
    private static final String CFG_INTERACT_STRAT = "lapdog.interaction-strategy";
    private static final String CFG_FILTER_INPUTS = "lapdog.filter-inputs";
    private static final String CFG_LEARN_MODE = "lapdog.learning-mode";

    private final LapdogLearner _lapdogLearner;
    private final CommonTaskRepresentation ctr;
    private final TrainingRepresentation train;

    /**
     * Create a the {@link LapdogFacade}.
     */
    LapdogFacade(Spine spine) {

        try {
            train = new TrainingRepresentation();
            ctr = new CommonTaskRepresentation();
            RemoteExecutor remoteExec = new RemoteExecutor(ctr, spine);
            IApplication lapdogApp = new ApplicationImpl(remoteExec);

            _lapdogLearner = new LapdogLearner(lapdogApp, false, new String[0]);
            train.setLearner(_lapdogLearner);

            // [CPOF 7.0] Respect system property for LAPDOG's output directory
            String outputDirectory = System.getProperty("lapdog.output.directory");
            if (_lapdogLearner.setDemonstrationLogDir(outputDirectory))
                _logger.info("LAPDOG output directory set to {}", outputDirectory);
            else
                _logger.info("Error occurred setting output directory of LAPDOG to ()", outputDirectory);

        } catch (final Exception e) {
            throw new LapdogFacadeException(e);
        }
    }

    /**
     * Publish a task type for lapdog's use.
     *
     * @param lapdogAction
     *            the {@link ATRActionDeclaration} to publish to lapdog
     * @throws LapdogFacadeException
     *             if the action cannot be defined
     */
    synchronized void publishAction(final ATRActionDeclaration lapdogAction)
            throws LapdogFacadeException {
        if (_logger.isDebugEnabled()) {
            _logger.debug("Pushing action to LAPDOG: {}",
                    ATRSyntax.toSource(lapdogAction));
        }
        if (ctr.isActionDefined(lapdogAction)) {
            _logger.warn("Already defined: "
                    + lapdogAction.getSignature().getFunctor());
            return;
        }
        try {
            ctr.defineAction(lapdogAction);
        } catch (final Exception e) {
            throw new LapdogFacadeException(e);
        }

        /* If it's collapsible, send that info also. */
        ATRMap props = lapdogAction.getProperties();
        ATRMap collapse = (ATRMap) props.get(TypeUtil.COLLAPSIBLE);
        if (collapse != null) {
            String inside = collapse
                    .getString(TypeUtil.COLLAPSIBLE_INSIDE_GESTURE);
            String outside = collapse
                    .getString(TypeUtil.COLLAPSIBLE_OUTSIDE_GESTURE);
            List<List<String>> paramIdKeep = new ArrayList<List<String>>();
            ATRSig sig = lapdogAction.getSignature();
            for (ATRParameter param : sig.getElements()) {
                String varName = param.getVariable().getVariableName();
                String keep = collapse.getString("$" + varName);
                if (keep != null) {
                    List<String> keepTuple = new ArrayList<String>();
                    keepTuple.add(varName);
                    keepTuple.add(keep);
                    paramIdKeep.add(keepTuple);
                }
            }
            String actName = TypeUtil.getName(lapdogAction).getFullName();
            _logger.debug("defineCollapsible({}, {}, {}, {})", new Object[] {
                    actName, inside, outside, paramIdKeep });
            try {
                ctr.defineCollapsible(actName, inside, outside, paramIdKeep);
            } catch (LapdogException e) {
                throw new LapdogFacadeException(e);
            }
        }
    }

    synchronized void removeAction(ATRActionDeclaration action) {
        try {
            ctr.undefineAction(action);
        } catch (Exception e) {
            _logger.error("Unable to remove action {}", action);
            throw new LapdogFacadeException(
                    "Unable to remove action " + action, e);
        }
    }

    synchronized void publishActionFamily(final ATRActionDeclaration actionFamily)
            throws LapdogFacadeException {
        if (_logger.isDebugEnabled()) {
            _logger.debug("Pushing action family to LAPDOG: {}",
                    ATRSyntax.toSource(actionFamily));
        }
        try {
            ctr.defineActionFamily(actionFamily);
        } catch (LapdogException e) {
            throw new LapdogFacadeException("Unable to add action family ("
                    + ATRSyntax.toSource(actionFamily), e);
        }
    }

    synchronized void publishIdiom(ATRActionDeclaration idiom)
            throws LapdogFacadeException {
        /*
         * Templates attached to an idiom, in ATR form: [[priority, [action,
         * action, ...]], [priority, [action, action, ...]], ...]. The actions
         * are ATRLiterals containing the string representation of an
         * ATRIdiomTemplateAction.
         */
        ATRMap props = idiom.getProperties();
        ATRList templatesTerm = (ATRList) props.get(TypeUtil.IDIOM_TEMPLATES);
        List<ATRIdiomTemplate> templates = new ArrayList<ATRIdiomTemplate>();
        for (ATRTerm templateTerm : templatesTerm.getTerms()) {
            ATRList templateList = (ATRList) templateTerm;
            ATRLiteral idTerm = (ATRLiteral) templateList.get(0);
            String templId = idTerm.getString();
            ATRLiteral prioTerm = (ATRLiteral) templateList.get(1);
            int precedence = prioTerm.getInt();
            ATRList actionList = (ATRList) templateList.get(2);
            List<ATRIdiomTemplateAction> actions = new ArrayList<ATRIdiomTemplateAction>();
            for (ATRTerm actionTerm : actionList.getTerms()) {
                ATRLiteral actionLit = (ATRLiteral) actionTerm;
                String actionStr = actionLit.getString();
                try {
                    ATRIdiomTemplateAction action = (ATRIdiomTemplateAction) IdiomTemplateFactory
                            .parseStatement(actionStr);
                    actions.add(action);
                } catch (LumenSyntaxError e) {
                    throw new LapdogFacadeException(
                            "Cannot parse as ATRIdiomTemplateAction: "
                                    + actionStr);
                }
            }
            ATRIdiomTemplate template = new ATRIdiomTemplateImpl(actions,
                    precedence, templId);
            templates.add(template);
        }
        if (_logger.isDebugEnabled()) {
            _logger.debug("Pushing idiom to LAPDOG: {}\ntemplates {}",
                    ATRSyntax.toSource(idiom), templates);
        }
        try {
            ctr.defineIdiom(idiom, templates);
        } catch (LapdogException e) {
            throw new LapdogFacadeException("Unable to define idiom: ("
                    + ATRSyntax.toSource(idiom) + ", " + templates + ")", e);
        }
    }

    synchronized void publishType(ATRTypeDeclaration type)
            throws TypeError {
        if (_logger.isDebugEnabled()) {
            _logger.debug("Pushing type to LAPDOG: {}", ATRSyntax.toSource(type));
        }
        if (!_lapdogLearner.isTypeDefined(TypeUtil.getName(type).getFullName())) {
            ctr.defineType(type);
        }
    }

    synchronized void publishCollection(ATRTypeDeclaration type)
            throws TypeError {
        if (_logger.isDebugEnabled()) {
            _logger.debug("Pushing collection to LAPDOG: {}",
                    ATRSyntax.toSource(type));
        }
        String eleName = TypeUtil.getElementType(type).getFullName();
        String collClass = TypeUtil.getCollectionType(type);
        ctr.defineCollectionType(type, eleName, collClass);
    }

    boolean isPublished(String typeName) {
        boolean f = _lapdogLearner.isActionDefined(typeName);
        return f;
    }

    /**
     * Synchronized delegate to
     * {@link CommonTaskRepresentation#learn}
     */
    synchronized CTRActionDeclaration learn(ATRDemonstration demo,
                                            Properties learnProps,                                            
                                            String taskName)
            throws LapdogException {

        if (learnProps == null) {
            learnProps = new Properties();
        }

        if (demo.getMetadata().get(AtrInputLanguage.taskInputPropertyName) != null) {
            learnProps.setProperty(CFG_INTERACT_STRAT,
                    LapdogConfiguration.InteractionStrategy.PROGRAMMATIC.toString());
            learnProps.setProperty(CFG_LEARN_MODE, LapdogConfiguration.LearningMode.LAPDOG.toString());
            learnProps.setProperty(CFG_FILTER_INPUTS, Boolean.TRUE.toString());
        }

        if (!learnProps.containsKey(CFG_INTERACT_STRAT)) {
            learnProps.setProperty(CFG_INTERACT_STRAT,
                    LapdogConfiguration.InteractionStrategy.AUTONOMOUS_WITH_SEARCH.toString());
        }

        LapdogConfiguration learnCfg = new LapdogConfiguration(learnProps, true);
        learnCfg = new LapdogConfiguration(learnCfg, System.getProperties(), false);

        _logger.debug("Learning procedure: name {}, config {}, actions {}", taskName, learnCfg,
                demo);
        CTRActionDeclaration p = (CTRActionDeclaration) ctr.learn(demo, taskName, learnCfg);
        _logger.debug("Lapdog returned: {}", ATRSyntax.toSource(p));
        return p;
    }

    synchronized ATRDemonstration recognizeIdiom(ATRDemonstration demo)
            throws LapdogException {
        _logger.debug("Recognizing idioms: {}", demo);
        ATRDemonstration newDemo = ctr.preprocessActionBlock(demo);
        _logger.debug("LAPDOG returned: {}", newDemo);
        return newDemo;
    }

    synchronized Option learnOption(ATRDemonstration demo)
            throws LapdogException {
        return train.learnOption(demo);
    }

    @Override
    public String toString() {
        return "LapdogFacade";
    }

    public void cancel() {
        _lapdogLearner.cancel();
    }

    void shutdown() {
        // Do nothing.
    }
}
