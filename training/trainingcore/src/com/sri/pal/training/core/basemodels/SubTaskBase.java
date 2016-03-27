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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.sri.pal.training.core.exercise.HintSequence;
import com.sri.pal.training.core.exercise.Link;


/**
 * <p>Java class for SubTask complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SubTask">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="help_link" type="{}Link" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="hint_sequence" type="{}HintSequence" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="hints_delay" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="prompt" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubTask", propOrder = {
    "helpLinks",
    "hintSequences",
    "hintsDelay",
    "prompt"
})
public class SubTaskBase {

    @XmlElement(name = "help_link")
    protected List<Link> helpLinks;
    @XmlElement(name = "hint_sequence")
    protected List<HintSequence> hintSequences;
    @XmlElement(name = "hints_delay")
    protected Integer hintsDelay;
    @XmlElement(required = true)
    protected String prompt;
    @XmlAttribute(name = "id", required = true)
    protected String id;

    /**
     * Gets the value of the helpLinks property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the helpLinks property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHelpLinks().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Link }
     * 
     * 
     */
    public List<Link> getHelpLinks() {
        if (helpLinks == null) {
            helpLinks = new ArrayList<Link>();
        }
        return this.helpLinks;
    }

    /**
     * Gets the value of the hintSequences property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the hintSequences property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHintSequences().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HintSequence }
     * 
     * 
     */
    public List<HintSequence> getHintSequences() {
        if (hintSequences == null) {
            hintSequences = new ArrayList<HintSequence>();
        }
        return this.hintSequences;
    }

    /**
     * Gets the value of the hintsDelay property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getHintsDelay() {
        return hintsDelay;
    }

    /**
     * Sets the value of the hintsDelay property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setHintsDelay(Integer value) {
        this.hintsDelay = value;
    }

    /**
     * Gets the value of the prompt property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the value of the prompt property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrompt(String value) {
        this.prompt = value;
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

}
