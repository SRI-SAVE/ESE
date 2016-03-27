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

import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.tasklearning.mediators.TypeAdder;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Will Haines
 *
 *         Encapsulates the mechanism by which types are published to the
 *         learners.
 */
public class TypePublicationFacade
        implements TypeAdder {
    private static final Logger _logger = LoggerFactory
            .getLogger(TypePublicationFacade.class);

    private LapdogFacade lapdogFacade;

    TypePublicationFacade(LapdogFacade lapdogFacade) {
        this.lapdogFacade = lapdogFacade;
    }

    /**
     * Publishes a type for LAPDOG's use.
     *
     * @param type
     */
    @Override
    public void add(ATRDecl type) {
        if (TypeUtil.isType(type)) {
            publishDataType((ATRTypeDeclaration) type);
        } else if (TypeUtil.isActionFamily(type)) {
            publishActionFamily((ATRActionDeclaration) type);
        } else if (TypeUtil.isAction(type)) {
            publishActionType((ATRActionDeclaration) type);
        } else if (TypeUtil.isIdiom(type)) {
            publishIdiom((ATRActionDeclaration) type);
        } else if (TypeUtil.isConstraintDecl(type)) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("Not publishing constraint {}",
                        ATRSyntax.toSource(type));
            }
        } else {
            _logger.warn("Unknown type to publish: {}",
                    ATRSyntax.toSource(type));
        }
    }

    private void publishIdiom(ATRActionDeclaration idiom) {
        lapdogFacade.publishIdiom(idiom);
    }

    private void publishActionFamily(ATRActionDeclaration family) {
        _logger.debug("Attempting to publish family: "
                + TypeUtil.getName(family));
        try {
            lapdogFacade.publishActionFamily(family);
        } catch (Exception e) {
            String msg = "Unable to publish action family "
                    + ATRSyntax.toSource(family);
            _logger.error(msg, e);
            throw new TypePublicationFacadeException(msg, e);
        }
    }

    /**
     * Publishes an action type for use by LAPDOG.
     *
     * @param type
     *            the ITL type to publish
     * @throws Exception
     */
    private void publishActionType(final ATRActionDeclaration type) {
        _logger.debug("Attempting to publish action:" + TypeUtil.getName(type));

        // Publish the task type to LAPDOG
        try {
            lapdogFacade.publishAction(type);
        } catch (Exception e) {
            _logger.error("unable to publish task:" + TypeUtil.getName(type), e);
            throw new TypePublicationFacadeException(e);
        }
    }

    @Override
    public boolean remove(ATRDecl decl) {
        if (TypeUtil.isAction(decl)) {
            lapdogFacade.removeAction((ATRActionDeclaration) decl);
            return true;
        } else {
            _logger.debug("remove invoked for a Type that"
                    + " was not an action");
            return false;
        }
    }

    /**
     * Converts an ITL type into a SPARK type, publishing it LAPDOG and Tailor
     * as necessary.
     *
     * @param type
     *            the ITL Type to define
     */
    private void publishDataType(final ATRTypeDeclaration type) {
        _logger.debug("Attempting to publish dataType: " + TypeUtil.getName(type));

        try {
            if (TypeUtil.isCollection(type)) {
                lapdogFacade.publishCollection(type);
            } else {
                lapdogFacade.publishType(type);
            }
        } catch (Exception e) {
            _logger.error(
                    "Unable to publish type to LAPDOG: "
                            + ATRSyntax.toSource(type), e);
            throw new TypePublicationFacadeException(e);
        }
    }

    @Override
    public String toString() {
        return "PublicationFacade";
    }

    void shutdown() {
        // Do nothing.
    }
}
