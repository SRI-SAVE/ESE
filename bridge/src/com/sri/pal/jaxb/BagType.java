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
// Generated on: 2016.04.26 at 11:34:50 AM MDT 
//


package com.sri.pal.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Holds an unordered collection of 0 or more objects of the same type. These
 *                 objects may be atoms, lists, or tuples. A bag allows duplicate entries.
 *             
 * 
 * <p>Java class for BagType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BagType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="generalizeUnsupported" type="{}GeneralizeUnsupportedType" minOccurs="0"/>
 *         &lt;element name="ref" type="{}MemberType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BagType", propOrder = {
    "generalizeUnsupported",
    "ref"
})
public class BagType {

    protected GeneralizeUnsupportedType generalizeUnsupported;
    @XmlElement(required = true)
    protected MemberType ref;

    /**
     * Gets the value of the generalizeUnsupported property.
     * 
     * @return
     *     possible object is
     *     {@link GeneralizeUnsupportedType }
     *     
     */
    public GeneralizeUnsupportedType getGeneralizeUnsupported() {
        return generalizeUnsupported;
    }

    /**
     * Sets the value of the generalizeUnsupported property.
     * 
     * @param value
     *     allowed object is
     *     {@link GeneralizeUnsupportedType }
     *     
     */
    public void setGeneralizeUnsupported(GeneralizeUnsupportedType value) {
        this.generalizeUnsupported = value;
    }

    /**
     * Gets the value of the ref property.
     * 
     * @return
     *     possible object is
     *     {@link MemberType }
     *     
     */
    public MemberType getRef() {
        return ref;
    }

    /**
     * Sets the value of the ref property.
     * 
     * @param value
     *     allowed object is
     *     {@link MemberType }
     *     
     */
    public void setRef(MemberType value) {
        this.ref = value;
    }

}
