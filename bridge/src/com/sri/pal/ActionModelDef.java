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

// $Id: ActionModelDef.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRCommon;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.pal.common.TypeName;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thing that's stored in the action model. This could be a type, an action,
 * or a constraint.
 */
public abstract class ActionModelDef {
    private static final Logger log = LoggerFactory
            .getLogger(ActionModelDef.class);
    // Static variable for JAXB Marshaller - drastically improves performance
    private static Marshaller jaxbMarsh = null;
    private static Unmarshaller jaxbUnmarsh;

    private final ATR atr;
    private final Bridge bridge;
    private String atrStr;

    /**
     * Metadata key for the description of a type definition. This corresponds
     * to the <code>description</code> XML elements in the action model file(s).
     */
    public static final String DESCRIPTION = TypeUtil.DESCRIPTION;

    protected ActionModelDef(ATR atr,
                             Bridge bridge) {
        this.atr = atr;
        this.bridge = bridge;
    }

    /**
     * Provides the name of this definition. It may be a simple name or a name
     * expression.
     *
     * @return this object's name
     */
    public abstract TypeName getName();

    /**
     * Returns the ATR representation of this definition.
     *
     * @return the ATR object which this objects reflects. If this is a
     *         predefined or implicit type, then it has a name but no
     *         definition, and this method will return {@code null}.
     * @see #getAtrStr
     */
    public ATR getAtr() {
        return atr;
    }

    protected Bridge getBridge() {
        return bridge;
    }

    protected ActionModel getActionModel() {
        return bridge.getActionModel();
    }

    protected Spine getSpine() {
        return bridge.getSpine();
    }

    protected ActionModelFactory getActionModelFactory() {
        return bridge.getActionModelFactory();
    }

    /**
     * The string representation (in CTR-S syntax) of this object's ATR
     * representation.
     *
     * @return the string representation of this object's ATR
     * @see #getAtr
     */
    public String getAtrStr() {
        if (atrStr == null) {
            atrStr = ATRSyntax.toSource(getAtr());
        }
        return atrStr;
    }

    /**
     * Provides the XML representation of this definition. This is equivalent to
     * what appears in the action model file originally used to define this
     * object.
     *
     * @return this object's XML representation
     * @throws PALException
     *             if an error occurs
     */
    public abstract String getXml()
            throws PALException;

    /**
     * Provides the value of the requested metadata item for this definition.
     * Every definition can have arbitrary string-string metadata key-value
     * pairs associated with it.
     *
     * @param key
     *            the key to use to retrieve the metadata value for
     * @return the value associated with the given key
     * @see #listMetadataKeys
     */
    public abstract String getMetadata(String key);

    /**
     * Lists all the known keys for this definition's metadata.
     *
     * @return this object's metadata keys
     * @see #getMetadata
     */
    public abstract Set<String> listMetadataKeys();

    /**
     * Returns all of the types which this one is dependent on. Recursively
     * includes required types of required types. Does not recurse on equivalent
     * types, since that would create infinite recursion.
     *
     * @return all the things this one depends on.
     */
    protected abstract Set<ActionModelDef> getRequiredDefs()
            throws PALException;

    protected static String marshal(JAXBElement<?> jaxb)
            throws JAXBException {
        initJaxb();
        StringWriter writer = new StringWriter();
        try {
            synchronized (jaxbMarsh) {
                jaxbMarsh.marshal(jaxb, writer);
            }
            return writer.toString();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                log.warn("Unable to close " + writer);
            }
        }
    }

    protected static JAXBElement<?> unmarshal(String xmlText)
            throws JAXBException {
        initJaxb();
        Reader in = new StringReader(xmlText);
        try {
            synchronized (jaxbUnmarsh) {
                JAXBElement<?> ele = (JAXBElement<?>) jaxbUnmarsh.unmarshal(in);
                return ele;
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log.warn("Couldn't close " + in, e);
            }
        }
    }

    protected static synchronized void initJaxb()
            throws JAXBException {
        if (jaxbMarsh == null) {
            JAXBContext jc = JAXBContext.newInstance(ActionModelType.class
                    .getPackage().getName());
            jaxbMarsh = jc.createMarshaller();
            jaxbMarsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    Boolean.TRUE);
            jaxbUnmarsh = jc.createUnmarshaller();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName().getFullName() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((getName() == null) ? 0 : getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ActionModelDef other = (ActionModelDef) obj;
        ATR atr = getAtr();
        ATR otherAtr = other.getAtr();
        if (atr == null && otherAtr == null) {
            return getName().equals(other.getName());
        } else {
            return ATRCommon.isEquivalent(atr, otherAtr);
        }
    }
}
