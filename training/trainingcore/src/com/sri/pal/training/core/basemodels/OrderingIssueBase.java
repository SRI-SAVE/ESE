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
// Generated on: 2014.11.19 at 08:44:59 PM MST 
//


package com.sri.pal.training.core.basemodels;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OrderingIssue complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OrderingIssue">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="predecessor_index" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="successor_index" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="constraint" type="{}OrderingConstraint" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrderingIssue", propOrder = {
    "predecessorIndex",
    "successorIndex",
    "constraint"
})
public class OrderingIssueBase {

    @XmlElement(name = "predecessor_index")
    protected int predecessorIndex;
    @XmlElement(name = "successor_index")
    protected int successorIndex;
    protected OrderingConstraintBase constraint;

    /**
     * Gets the value of the predecessorIndex property.
     * 
     */
    public int getPredecessorIndex() {
        return predecessorIndex;
    }

    /**
     * Sets the value of the predecessorIndex property.
     * 
     */
    public void setPredecessorIndex(int value) {
        this.predecessorIndex = value;
    }

    /**
     * Gets the value of the successorIndex property.
     * 
     */
    public int getSuccessorIndex() {
        return successorIndex;
    }

    /**
     * Sets the value of the successorIndex property.
     * 
     */
    public void setSuccessorIndex(int value) {
        this.successorIndex = value;
    }

    /**
     * Gets the value of the constraint property.
     * 
     * @return
     *     possible object is
     *     {@link OrderingConstraintBase }
     *     
     */
    public OrderingConstraintBase getConstraint() {
        return constraint;
    }

    /**
     * Sets the value of the constraint property.
     * 
     * @param value
     *     allowed object is
     *     {@link OrderingConstraintBase }
     *     
     */
    public void setConstraint(OrderingConstraintBase value) {
        this.constraint = value;
    }

}
