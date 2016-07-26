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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.05.13 at 04:29:47 PM MDT 
//


package com.sri.pal.training.core.basemodels;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.sri.pal.training.core.exercise.Atom;
import com.sri.pal.training.core.exercise.OptionSet;


/**
 * <p>Java class for Step complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Step">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="atom" type="{}Atom"/>
 *           &lt;element name="option_set" type="{}OptionSet"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="subtask" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="optional" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Step", propOrder = {
    "atom",
    "optionSet"
})
public class StepBase {

    protected Atom atom;
    @XmlElement(name = "option_set")
    protected OptionSet optionSet;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "subtask")
    protected String subtask;
    @XmlAttribute(name = "optional")
    protected Boolean optional;

    /**
     * Gets the value of the atom property.
     * 
     * @return
     *     possible object is
     *     {@link Atom }
     *     
     */
    public Atom getAtom() {
        return atom;
    }

    /**
     * Sets the value of the atom property.
     * 
     * @param value
     *     allowed object is
     *     {@link Atom }
     *     
     */
    public void setAtom(Atom value) {
        this.atom = value;
    }

    /**
     * Gets the value of the optionSet property.
     * 
     * @return
     *     possible object is
     *     {@link OptionSet }
     *     
     */
    public OptionSet getOptionSet() {
        return optionSet;
    }

    /**
     * Sets the value of the optionSet property.
     * 
     * @param value
     *     allowed object is
     *     {@link OptionSet }
     *     
     */
    public void setOptionSet(OptionSet value) {
        this.optionSet = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the subtask property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubtask() {
        return subtask;
    }

    /**
     * Sets the value of the subtask property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubtask(String value) {
        this.subtask = value;
    }

    /**
     * Gets the value of the optional property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isOptional() {
        return optional;
    }

    /**
     * Sets the value of the optional property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setOptional(Boolean value) {
        this.optional = value;
    }

}
