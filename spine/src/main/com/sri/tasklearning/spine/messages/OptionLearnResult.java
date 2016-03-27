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
package com.sri.tasklearning.spine.messages;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.SAXException;

import com.sri.pal.common.ErrorInfo;
import com.sri.pal.training.core.basemodels.ObjectFactory;
import com.sri.pal.training.core.basemodels.OptionBase;
import com.sri.pal.training.core.exercise.Option;
import com.sri.pal.training.core.storage.ExerciseFactory;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;

public class OptionLearnResult
        extends BroadcastMessage {
    private static final long serialVersionUID = 1L;

    private final String optionSrc;
    private final ErrorInfo error;

    public OptionLearnResult(String sender,
                             TransactionUID uid,
                             Option option)
            throws JAXBException {
        super(sender, uid, UserMessageType.LEARN_OPTION_RESULT);
        error = null;

        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<OptionBase> je = objectFactory.createOption(option);
        Marshaller marshaller = ExerciseFactory.getMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(je, sw);
        optionSrc = sw.toString();
    }

    public OptionLearnResult(String sender,
                             TransactionUID uid,
                             ErrorInfo error) {
        super(sender, uid, UserMessageType.LEARN_OPTION_RESULT);
        this.error = error;
        optionSrc = null;
    }

    public String getOptionSrc() {
        return optionSrc;
    }

    public Option getOption()
            throws JAXBException,
            SAXException {
        Unmarshaller unm = ExerciseFactory.getUnmarshaller();
        Reader in = new StringReader(optionSrc);
        JAXBElement<?> ele = (JAXBElement<?>) unm.unmarshal(in);
        Option option = (Option) ele.getValue();
        return option;
    }

    public ErrorInfo getError() {
        return error;
    }

    @Override
    public TransactionUID getUid() {
        return (TransactionUID) uid;
    }

    @Override
    public String toString() {
        return super.toString() + optionSrc;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        String str = toString();
        result = prime * result + ((str == null) ? 0 : str.hashCode());
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
        OptionLearnResult other = (OptionLearnResult) obj;
        String str = toString();
        String ostr = other.toString();
        if (str == null) {
            if (ostr != null)
                return false;
        } else if (!str.equals(ostr))
            return false;
        return true;
    }
}
