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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Value constraint contains one or more values that the parameter must conform to. A value element is a
 *                 literal value, and a ref element contains an expression that can be evaluated by Lumen to produce a
 *                 value. If multiple value and/or ref elements are present, the parameter may be equal to any of the set.
 *                 If min and max value or ref elements are present, this constraint represents a range of acceptable
 *                 values which the parameter must conform to.
 *             
 * 
 * <p>Java class for ValueConstraint complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ValueConstraint">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="parameter" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;choice>
 *           &lt;sequence>
 *             &lt;element name="value" type="{}Value" maxOccurs="unbounded" minOccurs="0"/>
 *             &lt;element name="ref" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *           &lt;/sequence>
 *           &lt;sequence>
 *             &lt;choice>
 *               &lt;element name="min_value" type="{}Value"/>
 *               &lt;element name="min_ref" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *             &lt;/choice>
 *             &lt;choice>
 *               &lt;element name="max_value" type="{}Value"/>
 *               &lt;element name="max_ref" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *             &lt;/choice>
 *           &lt;/sequence>
 *         &lt;/choice>
 *         &lt;element name="reason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="editable" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="negated" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ValueConstraint", propOrder = {
    "parameter",
    "values",
    "refs",
    "minValue",
    "minRef",
    "maxValue",
    "maxRef",
    "reason"
})
public class ValueConstraintBase {

    @XmlElement(required = true)
    protected String parameter;
    @XmlElement(name = "value")
    protected List<com.sri.pal.training.core.exercise.Value> values;
    @XmlElement(name = "ref")
    protected List<String> refs;
    @XmlElement(name = "min_value")
    protected com.sri.pal.training.core.exercise.Value minValue;
    @XmlElement(name = "min_ref")
    protected String minRef;
    @XmlElement(name = "max_value")
    protected com.sri.pal.training.core.exercise.Value maxValue;
    @XmlElement(name = "max_ref")
    protected String maxRef;
    protected String reason;
    @XmlAttribute(name = "editable")
    protected Boolean editable;
    @XmlAttribute(name = "negated")
    protected Boolean negated;

    /**
     * Gets the value of the parameter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * Sets the value of the parameter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParameter(String value) {
        this.parameter = value;
    }

    /**
     * Gets the value of the values property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the values property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getValues().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link com.sri.pal.training.core.exercise.Value }
     * 
     * 
     */
    public List<com.sri.pal.training.core.exercise.Value> getValues() {
        if (values == null) {
            values = new ArrayList<com.sri.pal.training.core.exercise.Value>();
        }
        return this.values;
    }

    /**
     * Gets the value of the refs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the refs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRefs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getRefs() {
        if (refs == null) {
            refs = new ArrayList<String>();
        }
        return this.refs;
    }

    /**
     * Gets the value of the minValue property.
     * 
     * @return
     *     possible object is
     *     {@link com.sri.pal.training.core.exercise.Value }
     *     
     */
    public com.sri.pal.training.core.exercise.Value getMinValue() {
        return minValue;
    }

    /**
     * Sets the value of the minValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link com.sri.pal.training.core.exercise.Value }
     *     
     */
    public void setMinValue(com.sri.pal.training.core.exercise.Value value) {
        this.minValue = value;
    }

    /**
     * Gets the value of the minRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMinRef() {
        return minRef;
    }

    /**
     * Sets the value of the minRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMinRef(String value) {
        this.minRef = value;
    }

    /**
     * Gets the value of the maxValue property.
     * 
     * @return
     *     possible object is
     *     {@link com.sri.pal.training.core.exercise.Value }
     *     
     */
    public com.sri.pal.training.core.exercise.Value getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the value of the maxValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link com.sri.pal.training.core.exercise.Value }
     *     
     */
    public void setMaxValue(com.sri.pal.training.core.exercise.Value value) {
        this.maxValue = value;
    }

    /**
     * Gets the value of the maxRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMaxRef() {
        return maxRef;
    }

    /**
     * Sets the value of the maxRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMaxRef(String value) {
        this.maxRef = value;
    }

    /**
     * Gets the value of the reason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the value of the reason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReason(String value) {
        this.reason = value;
    }

    /**
     * Gets the value of the editable property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEditable() {
        if (editable == null) {
            return true;
        } else {
            return editable;
        }
    }

    /**
     * Sets the value of the editable property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEditable(Boolean value) {
        this.editable = value;
    }

    /**
     * Gets the value of the negated property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isNegated() {
        if (negated == null) {
            return false;
        } else {
            return negated;
        }
    }

    /**
     * Sets the value of the negated property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNegated(Boolean value) {
        this.negated = value;
    }

}
