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

// $Id: LearnRequest.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages;

import java.util.Properties;
import java.util.Set;

import com.sri.ai.lumen.atr.learning.ATRDemonstration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class LearnRequest
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final ATRDemonstration demonstration;
    private final SimpleTypeName name;
    private final Properties learnProps;
    private final Set<TypeName> extraTypes;

    public LearnRequest(String sender,
                        ATRDemonstration demonstration,
                        SimpleTypeName name,
                        Properties learnProps,
                        Set<TypeName> extraTypes,
                        TransactionUID uid) {
        super(sender, uid, UserMessageType.LEARN_REQUEST);
        this.demonstration = demonstration;
        this.name = name;
        this.learnProps = learnProps;
        this.extraTypes = extraTypes;
    }

    public ATRDemonstration getDemonstration() {
        return demonstration;
    }

    public SimpleTypeName getName() {
        return name;
    }

    public Properties getLearnProps() {
        return learnProps;
    }

    public Set<TypeName> getExtraTypes() {
        return extraTypes;
    }

    public TransactionUID getUid() {
        return (TransactionUID)uid;
    }

    @Override
    public String toString() {
        return super.toString() + " " + demonstration.getActions().size()
                + " actions, name: " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LearnRequest that = (LearnRequest) o;

        if (demonstration != null ? !demonstration.equals(that.demonstration) : that.demonstration != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (learnProps != null ? !learnProps.equals(that.learnProps) : that.learnProps != null) return false;
        return extraTypes != null ? extraTypes.equals(that.extraTypes) : that.extraTypes == null;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((demonstration == null) ? 0 : demonstration.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

}
