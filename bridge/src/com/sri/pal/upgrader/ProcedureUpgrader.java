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

// $Id: ProcedureUpgrader.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.upgrader;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.syntax.FormatUtil;
import com.sri.ai.lumen.atr.ATRParameter;
import com.sri.ai.lumen.atr.ATRParameter.Modality;
import com.sri.ai.lumen.atr.ATRSig;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.runtime.LumenConnection;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.ai.lumen.spark.SPARKLTranslator;
import com.sri.ai.lumen.spark.SPARK;
import com.sri.ai.lumen.core.IStructure;
import com.sri.pal.LumenProcedureDef;
import com.sri.pal.PALException;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.TaskType;

/**
 * Various routines for upgrading and modifying procedures while offline. These
 * methods all operate on the procedure source, without any PAL services
 * running.
 */
public class ProcedureUpgrader {
    private static final Logger log = LoggerFactory
            .getLogger(ProcedureUpgrader.class);

    private static final String SPARKL_VERSION = "0.1.4";
    private static Unmarshaller jaxbUnmarsh;

    /**
     * Use {@link #substituteConstant} instead.
     */
    @Deprecated
    public static String substitute(String xmlSource,
                                    String oldValue,
                                    String newValue)
            throws PALException {
        return substituteConstant(xmlSource, oldValue, newValue);
    }

    /**
     * Substitutes one constant for another in a procedure. Each occurrence of
     * the given string will be replaced, provided it is a constant string (not
     * a variable name, action name, or other language construct).
     *
     * @param xmlSource
     *            the (XML-wrapped) source of the procedure to perform
     *            substitution on
     * @param oldValue
     *            the constant to search for in the procedure's source
     * @param newValue
     *            the new value to replace the constant with
     * @return new (XML-wrapped) procedure source with the substitution
     *         performed
     * @throws PALException
     *             if the source cannot be parsed, substituted, or re-wrapped in
     *             XML
     */
    public static String substituteConstant(String xmlSource,
                                            String oldValue,
                                            String newValue)
            throws PALException {
        String ctrSource = LumenProcedureDef.unwrapXml(xmlSource);
        try {
            ctrSource = LumenConnection.substitute(ctrSource, oldValue,
                    newValue);
        } catch (LumenSyntaxError e) {
            throw new PALException("Lumen substitute failed", e);
        }

        // Make sure it's still valid CTR
        LumenProcedureDef.sourceToProc(ctrSource);

        try {
            return LumenProcedureDef.wrapXml(ctrSource);
        } catch (JAXBException e) {
            throw new PALException(
                    "Failed to build XML-source from Lumen source: "
                            + ctrSource, e);
        }
    }

    /**
     * Used to rename actions, types, or entire namespaces. This method performs
     * a regular expression substitution on every type and action name in the
     * provided procedure, including the procedure's own name.
     *
     * @param xmlSource
     *            the (XML-wrapped) source of the procedure to perform
     *            substitution on
     * @param regex
     *            the regular expression to search for in each encountered
     *            symbol
     * @param replacement
     *            the replacement pattern to use. References to capture groups
     *            are allowed here.
     * @return new (XML-wrapped) procedure source with the substitution
     *         performed
     * @throws PALException
     *             if the source cannot be parsed, or substituted
     * @throws JAXBException
     *             if the resultant procedure cannot be re-wrapped in XML
     * @see java.util.regex.Pattern
     */
    public static String substituteTypes(String xmlSource,
                                         String regex,
                                         String replacement)
            throws PALException,
            JAXBException {
        String ctrSource = LumenProcedureDef.unwrapXml(xmlSource, false);
        ATRActionDeclaration proc = LumenProcedureDef.sourceToProc(ctrSource);
        RegexRenamer rr = new RegexRenamer(regex, replacement);
        rr.run(proc);
        String newCtrSrc = LumenProcedureDef.procToSource(proc);
        String newXmlSrc = LumenProcedureDef.wrapXml(newCtrSrc);
        return newXmlSrc;
    }

    /**
     * Upgrades a procedure from the old SPARK-L language to the new CTR-S
     * language.
     *
     * @param xmlSource
     *            the XML-wrapped source of the SPARK-L procedure
     * @return XML-wrapped source of the equivalent CTR-S procedure
     * @throws PALException
     *             if SPARK-L parsing fails
     * @throws JAXBException
     *             if the XML cannot be parsed, or if the new procedure cannot
     *             be wrapped in XML
     * @throws LumenSyntaxError
     *             if the old SPARK-L procedure cannot be parsed
     */
    public static String upgradeSPARKL(String xmlSource)
            throws JAXBException,
            PALException {
        String fixedXmlSrc = fixXmlBeansQuoting(xmlSource);

        JAXBElement<?> ele;
        initJaxb();
        Reader in = new StringReader(fixedXmlSrc);
        synchronized (jaxbUnmarsh) {
            ele = (JAXBElement<?>) jaxbUnmarsh.unmarshal(in);
        }

        TaskType taskXml = (TaskType) ele.getValue();
        String version = taskXml.getVersion();
        if (!SPARKL_VERSION.equals(version)) {
            String msg = "Wrong task format version: expected "
                    + SPARKL_VERSION + ", got " + version + ". Trying anyway.";
            log.warn(msg);
        }
        String bodySource = taskXml.getBodySource();
        String cueSource = taskXml.getCueSource();

        IStructure<?> defaction;
        try {
            defaction = (IStructure<?>) SPARK.parseSPARKLMultiple(cueSource)
                    .get(0);
        } catch (LumenSyntaxError e) {
            throw new PALException("Parsing defaction: " + cueSource, e);
        }
        IStructure<?> defprocedure;
        try {
            defprocedure = (IStructure<?>) SPARK.parseSPARKLOne(bodySource);
        } catch (LumenSyntaxError e) {
            throw new PALException("Parsing defprocedure: " + bodySource, e);
        }
        Object toLumen = SPARKLTranslator.translateDefactionDefprocedure(
                defaction, defprocedure);
        String ctrSrc = FormatUtil.format("%R;", toLumen);

        /*
         * The old procedure probably has no namespace. The same is true of
         * calls to sub-procedures. We need to traverse the procedure and look
         * for action calls with no namespace, and put them in SPARK.
         */
        ATRActionDeclaration proc = LumenProcedureDef.sourceToProc(ctrSrc);
        NamespaceAdder na = new NamespaceAdder();
        na.run(proc);
        String newCtrSrc = LumenProcedureDef.procToSource(proc);

        String xmlSrc = LumenProcedureDef.wrapXml(newCtrSrc);

        return xmlSrc;
    }

    /**
     * Allows an implementor to replace calls to one action with calls to
     * another, or rearrange the order of arguments. This method will parse the
     * provided procedure and extract calls to actions. For each action call in
     * the procedure, the provided visitor will be called with the corresponding
     * {@link ActionCall} object.
     *
     * @param xmlSource
     *            XML source of the procedure to modify
     * @param visitor
     *            a visitor which can change action calls in the provided
     *            procedure
     * @return XML source of the modified procedure
     * @throws PALException
     *             if the provided procedure cannot be parsed
     * @throws JAXBException
     *             if the new procedure cannot be re-wrapped in XML
     */
    public static String visitActions(String xmlSource,
                                      ActionVisitor visitor)
            throws PALException,
            JAXBException {
        String ctrSource = LumenProcedureDef.unwrapXml(xmlSource);
        ATRActionDeclaration proc = LumenProcedureDef.sourceToProc(ctrSource);
        VisitorDriver vd = new VisitorDriver(visitor);
        vd.run(proc);
        String newCtrSrc = LumenProcedureDef.procToSource(proc);
        String newXmlSrc = LumenProcedureDef.wrapXml(newCtrSrc);
        return newXmlSrc;
    }

    /**
     * Parses a procedure and provides access to its parameters by name, type,
     * and default value.
     *
     * @param xmlSource
     *            XML-wrapped source of the procedure to parse
     * @return a list of structures representing the parameters of this
     *         procedure
     * @throws PALException
     *             if the procedure cannot be parsed
     */
    public static List<ProcedureParam> getProcedureParameters(String xmlSource)
            throws PALException {
        List<ProcedureParam> result = new ArrayList<ProcedureParam>();
        String ctrSource = LumenProcedureDef.unwrapXml(xmlSource);
        ATRActionDeclaration proc = LumenProcedureDef.sourceToProc(ctrSource);
        ATRSig sig = proc.getSignature();
        for (ATRParameter atrParam : sig.getElements()) {
            String name = atrParam.getVariable().getVariableName();
            String type = atrParam.getType().getString();
            String defaultValue = null;
            if (atrParam.getMode() == Modality.INPUT) {
                defaultValue = ATRSyntax.toSource(atrParam.getDefaultValue());
            }
            ProcedureParam param = new ProcedureParam(name, type, defaultValue);
            result.add(param);
        }
        return result;
    }

    /**
     * Old versions of the software used Apache XmlBeans instead of JAXB to
     * store procedures. If the procedure was demonstrated with a string which
     * is an XML document, we'll end up with something like
     * <TaskModel><cueSource>...$var="<foo>..."</cueSource>...</TaskModel>. JAXB
     * will choke on the embedded <foo>.
     * <p>
     * This method picks apart the XML doc and quotes some characters before
     * reassembling it.
     */
    private static String fixXmlBeansQuoting(final String xmlSource) {
        Matcher m;

        // The doc looks like
        // <TaskModel>
        // <cueSource>text</cueSource>
        // <bodySource>text</bodySource>
        // </TaskModel>
        // Find all the pieces.
        m = Pattern.compile("<TaskModel [^>]*>").matcher(xmlSource);
        m.find();
        String taskModelStart = xmlSource.substring(0, m.end());
        String sourceTail = xmlSource.substring(m.end());

        m = Pattern.compile("<cueSource>").matcher(sourceTail);
        m.find();
        String cueSourceStart = sourceTail.substring(0, m.end());
        sourceTail = sourceTail.substring(m.end());

        m = Pattern.compile("</cueSource>").matcher(sourceTail);
        m.find();
        String cueSource = sourceTail.substring(0, m.start());
        String cueSourceEnd = sourceTail.substring(m.start(), m.end());
        sourceTail = sourceTail.substring(m.end());

        m = Pattern.compile("<bodySource>").matcher(sourceTail);
        m.find();
        String bodySourceStart = sourceTail.substring(0, m.end());
        sourceTail = sourceTail.substring(m.end());

        m = Pattern.compile("</bodySource>").matcher(sourceTail);
        m.find();
        String bodySource = sourceTail.substring(0, m.start());
        String bodySourceEnd = sourceTail.substring(m.start(), m.end());
        String taskModelEnd = sourceTail.substring(m.end());

        // Now apply quoting to cueSource and bodySource.
        cueSource = cueSource.replace("<", "&lt;");
        bodySource = bodySource.replace("<", "&lt;");
        cueSource = cueSource.replace(">", "&gt;");
        bodySource = bodySource.replace(">", "&gt;");

        // Re-assemble the string.
        String result = taskModelStart + cueSourceStart + cueSource
                + cueSourceEnd + bodySourceStart + bodySource + bodySourceEnd
                + taskModelEnd;
        return result;
    }

    private static synchronized void initJaxb()
            throws JAXBException {
        if (jaxbUnmarsh == null) {
            JAXBContext jc = JAXBContext.newInstance(ActionModelType.class
                    .getPackage().getName());
            jaxbUnmarsh = jc.createUnmarshaller();
        }
    }
}
