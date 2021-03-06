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
import com.sri.pal.training.core.exercise.EqualityConstraint;
import com.sri.pal.training.core.exercise.OrderingConstraint;
import com.sri.pal.training.core.exercise.QueryConstraint;
import com.sri.pal.training.core.exercise.StateConstraint;
import com.sri.pal.training.core.exercise.Step;
import com.sri.pal.training.core.exercise.TypeConstraint;
import com.sri.pal.training.core.exercise.ValueConstraint;


/**
 * <p>Java class for Option complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Option">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="demonstration" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="step" type="{}Step" maxOccurs="unbounded"/>
 *         &lt;element name="equality_constraint" type="{}EqualityConstraint" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ordering_constraint" type="{}OrderingConstraint" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="query_constraint" type="{}QueryConstraint" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="state_constraint" type="{}StateConstraint" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="value_constraint" type="{}ValueConstraint" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="type_constraint" type="{}TypeConstraint" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Option", propOrder = {
    "demonstration",
    "steps",
    "equalityConstraints",
    "orderingConstraints",
    "queryConstraints",
    "stateConstraints",
    "valueConstraints",
    "typeConstraints"
})
public class OptionBase {

    protected List<String> demonstration;
    @XmlElement(name = "step", required = true)
    protected List<Step> steps;
    @XmlElement(name = "equality_constraint")
    protected List<EqualityConstraint> equalityConstraints;
    @XmlElement(name = "ordering_constraint")
    protected List<OrderingConstraint> orderingConstraints;
    @XmlElement(name = "query_constraint")
    protected List<QueryConstraint> queryConstraints;
    @XmlElement(name = "state_constraint")
    protected List<StateConstraint> stateConstraints;
    @XmlElement(name = "value_constraint")
    protected List<ValueConstraint> valueConstraints;
    @XmlElement(name = "type_constraint")
    protected List<TypeConstraint> typeConstraints;

    /**
     * Gets the value of the demonstration property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the demonstration property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDemonstration().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getDemonstration() {
        if (demonstration == null) {
            demonstration = new ArrayList<String>();
        }
        return this.demonstration;
    }

    /**
     * Gets the value of the steps property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the steps property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSteps().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Step }
     * 
     * 
     */
    public List<Step> getSteps() {
        if (steps == null) {
            steps = new ArrayList<Step>();
        }
        return this.steps;
    }

    /**
     * Gets the value of the equalityConstraints property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the equalityConstraints property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEqualityConstraints().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EqualityConstraint }
     * 
     * 
     */
    public List<EqualityConstraint> getEqualityConstraints() {
        if (equalityConstraints == null) {
            equalityConstraints = new ArrayList<EqualityConstraint>();
        }
        return this.equalityConstraints;
    }

    /**
     * Gets the value of the orderingConstraints property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the orderingConstraints property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrderingConstraints().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OrderingConstraint }
     * 
     * 
     */
    public List<OrderingConstraint> getOrderingConstraints() {
        if (orderingConstraints == null) {
            orderingConstraints = new ArrayList<OrderingConstraint>();
        }
        return this.orderingConstraints;
    }

    /**
     * Gets the value of the queryConstraints property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the queryConstraints property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getQueryConstraints().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link QueryConstraint }
     * 
     * 
     */
    public List<QueryConstraint> getQueryConstraints() {
        if (queryConstraints == null) {
            queryConstraints = new ArrayList<QueryConstraint>();
        }
        return this.queryConstraints;
    }

    /**
     * Gets the value of the stateConstraints property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the stateConstraints property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStateConstraints().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StateConstraint }
     * 
     * 
     */
    public List<StateConstraint> getStateConstraints() {
        if (stateConstraints == null) {
            stateConstraints = new ArrayList<StateConstraint>();
        }
        return this.stateConstraints;
    }

    /**
     * Gets the value of the valueConstraints property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the valueConstraints property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getValueConstraints().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ValueConstraint }
     * 
     * 
     */
    public List<ValueConstraint> getValueConstraints() {
        if (valueConstraints == null) {
            valueConstraints = new ArrayList<ValueConstraint>();
        }
        return this.valueConstraints;
    }

    /**
     * Gets the value of the typeConstraints property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the typeConstraints property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTypeConstraints().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TypeConstraint }
     * 
     * 
     */
    public List<TypeConstraint> getTypeConstraints() {
        if (typeConstraints == null) {
            typeConstraints = new ArrayList<TypeConstraint>();
        }
        return this.typeConstraints;
    }

}
