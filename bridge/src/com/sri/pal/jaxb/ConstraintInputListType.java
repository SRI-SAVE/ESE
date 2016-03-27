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
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.07.10 at 05:21:16 PM MDT 
//


package com.sri.pal.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ConstraintInputListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConstraintInputListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;group ref="{}paramInput"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConstraintInputListType", propOrder = {
    "inputListOrInputRefOrInputUnknown"
})
public class ConstraintInputListType {

    @XmlElements({
        @XmlElement(name = "inputFunc", type = ConstraintInputFuncType.class),
        @XmlElement(name = "inputConstant", type = ConstraintInputConstantType.class),
        @XmlElement(name = "inputRef", type = ConstraintInputRefType.class),
        @XmlElement(name = "inputUnknown", type = ConstraintInputUnknownType.class),
        @XmlElement(name = "inputList", type = ConstraintInputListType.class)
    })
    protected List<Object> inputListOrInputRefOrInputUnknown;

    /**
     * Gets the value of the inputListOrInputRefOrInputUnknown property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inputListOrInputRefOrInputUnknown property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInputListOrInputRefOrInputUnknown().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ConstraintInputFuncType }
     * {@link ConstraintInputConstantType }
     * {@link ConstraintInputRefType }
     * {@link ConstraintInputUnknownType }
     * {@link ConstraintInputListType }
     * 
     * 
     */
    public List<Object> getInputListOrInputRefOrInputUnknown() {
        if (inputListOrInputRefOrInputUnknown == null) {
            inputListOrInputRefOrInputUnknown = new ArrayList<Object>();
        }
        return this.inputListOrInputRefOrInputUnknown;
    }

}
