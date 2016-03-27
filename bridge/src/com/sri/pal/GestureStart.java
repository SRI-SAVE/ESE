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

// $Id: GestureStart.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.common.TypeName;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SerialNumberRequest;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

/**
 * A gesture is an instance of an idiom in the same sense that
 * {@code ActionInvocation} is an instance of {@code ActionDef}. However, a
 * gesture doesn't always have an idiom to define it; some gestures are created
 * before they are identified with a particular idiom, and some gestures can
 * exist which do not have an identifying idiom at all.
 */
public class GestureStart
        extends ActionStreamEvent {
    private final ArrayList<TypeName> paramTypeNames;
    private final String templateName;

    /**
     * Creates a new gesture start event for use in a demonstration. The new
     * event will have a {@code null} caller, meaning it's a top-level,
     * user-generated gesture.
     *
     * @param bridge
     *            the Bridge instance to which this gesture belongs
     * @param definition
     *            the idiom definition which this gesture is a part of; may be
     *            {@code null}
     * @param paramTypes
     *            the types of the declared parameters to this gesture's idiom.
     *            In the common case where the idiom is not known, this should
     *            be {@code null}.
     * @return a new gesture suitable for sending to either
     *         {@link Learner#learn} or {@link Learner#recognizeIdiom}
     * @throws PALException
     *             if a communication error occurs
     */
    public static GestureStart newInstance(Bridge bridge,
                                           IdiomDef definition,
                                           String templateName,
                                           List<TypeName> paramTypes)
            throws PALException {
        long serialNum;
        Spine spine = bridge.getSpine();
        try {
            TransactionUID uid = spine.getNextUid();
            Message msg = new SerialNumberRequest(spine.getClientId(),
                    uid);
            SerialNumberResponse result = bridge.getSerialGetter()
                    .sendAndGetReply(msg);
            serialNum = result.getSerialNumber();
        } catch (SpineException e) {
            throw new PALException("Unable to get serial number", e);
        }

        TransactionUID newUid = spine.getNextUid();

        return new GestureStart(definition, bridge, null, paramTypes,
                templateName, serialNum, newUid);
    }

    protected GestureStart(IdiomDef definition,
                           Bridge bridge,
                           ActionStreamEvent callingEvent,
                           List<TypeName> paramTypes,
                           String templName,
                           long serialNum,
                           TransactionUID myUid) {
        super(definition, bridge, callingEvent, serialNum, myUid);
        templateName = templName;
        if (paramTypes != null) {
            paramTypeNames = new ArrayList<TypeName>();
            paramTypeNames.addAll(paramTypes);
        } else {
            paramTypeNames = null;
        }
    }

    @Override
    public IdiomDef getDefinition() {
        return (IdiomDef) super.getDefinition();
    }

    /**
     * Provides the name of the idiom template which this gesture fulfills.
     *
     * @return this idiom gesture's template name, or {@code null}
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Provides the name of the type of a particular parameter. Types are only
     * known for idiom gestures, not for non-idiom gestures. In addition, types
     * are only known for the declared parameters of the idiom, not for
     * additional variables which get created inside the gesture due to unbound
     * parameters in the gesture's constituent actions. Finally, types are only
     * known during learning, not during execution. For the number of declared
     * parameters (maximum index of {@code i}), call
     * {@code getDefinition().size()}.
     *
     * @param i
     *            the index of the parameter to retrieve type information for
     * @return the type name of the given parameter, or {@code null}
     */
    public TypeName getParamTypeName(int i) {
        if (paramTypeNames == null) {
            return null;
        } else {
            return paramTypeNames.get(i);
        }
    }

    @Override
    public TypeDef getParamType(int i)
            throws PALException {
        TypeName name = getParamTypeName(i);
        if (name == null) {
            return null;
        } else {
            TypeDef type = (TypeDef) getDefinition().getActionModel().getType(
                    name);
            return type;
        }
    }

    @Override
    protected boolean isLocallyExecuted() {
        return isLocallyExecutedStatic(this);
    }

    static boolean isLocallyExecutedStatic(ActionStreamEvent e) {
        String clientId = e.getBridge().getSpine().getClientId();
        if (e.getUid().getOriginator().equals(clientId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((paramTypeNames == null) ? 0 : paramTypeNames.hashCode());
        result = prime * result
                + ((templateName == null) ? 0 : templateName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        GestureStart other = (GestureStart) obj;
        if (paramTypeNames == null) {
            if (other.paramTypeNames != null)
                return false;
        } else if (!paramTypeNames.equals(other.paramTypeNames))
            return false;
        if (templateName == null) {
            if (other.templateName != null)
                return false;
        } else if (!templateName.equals(other.templateName))
            return false;
        return true;
    }
}
