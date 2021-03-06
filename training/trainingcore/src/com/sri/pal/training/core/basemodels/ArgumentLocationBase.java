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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArgumentLocation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArgumentLocation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="atom_index" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="accessor" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArgumentLocation", propOrder = {
    "atomIndex",
    "accessors"
})
public class ArgumentLocationBase {

    @XmlElement(name = "atom_index")
    protected int atomIndex;
    @XmlElement(name = "accessor", required = true)
    protected List<String> accessors;

    /**
     * Gets the value of the atomIndex property.
     * 
     */
    public int getAtomIndex() {
        return atomIndex;
    }

    /**
     * Sets the value of the atomIndex property.
     * 
     */
    public void setAtomIndex(int value) {
        this.atomIndex = value;
    }

    /**
     * Gets the value of the accessors property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessors property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessors().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getAccessors() {
        if (accessors == null) {
            accessors = new ArrayList<String>();
        }
        return this.accessors;
    }

}
