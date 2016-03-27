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

// $Id: ActionModel.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.sri.ai.lumen.atr.ATR;
import com.sri.ai.lumen.atr.ATRSyntax;
import com.sri.ai.lumen.atr.decl.ATRActionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRFunctionDeclaration;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.ai.lumen.atr.decl.impl.CTRTypeDeclaration;
import com.sri.ai.lumen.atr.impl.CTRConstructor;
import com.sri.ai.lumen.atr.term.ATRMap;
import com.sri.ai.lumen.atr.term.ATRNoEvalTerm;
import com.sri.ai.lumen.atr.term.ATRTerm;
import com.sri.pal.PrimitiveTypeDef.Predefined;
import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeName;
import com.sri.pal.common.TypeNameExpr;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.jaxb.ActionModelType;
import com.sri.pal.jaxb.ActionType;
import com.sri.pal.jaxb.ConstraintDeclarationType;
import com.sri.pal.jaxb.ConstraintsType;
import com.sri.pal.jaxb.FamilyType;
import com.sri.pal.jaxb.IdiomType;
import com.sri.pal.jaxb.MetadataType;
import com.sri.pal.jaxb.RequireType;
import com.sri.pal.jaxb.TypeType;
import com.sri.tasklearning.lapdogController.LapdogClientCallServiceImpl;
import com.sri.tasklearning.lapdogController.LapdogFacade;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.ExecutorListQuery;
import com.sri.tasklearning.spine.messages.ExecutorListResult;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.contents.TransactionUID;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.TypeCache;
import com.sri.tasklearning.spine.util.TypeUtil;
import com.sri.tasklearning.util.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Provides access to the set of types and actions which are available.
 *
 * @author chris
 */
public class ActionModel {
    private static final Logger log = LoggerFactory
            .getLogger(ActionModel.class);

    static final String XSD_NAME = "ActionModel.xsd";
    private static final String AGAVE_CLASS = LapdogFacade.AGAVE_CLASS;
    private static final String AGAVE_AM = "agave.xml";
    static final String AGAVE_NAMESPACE = LapdogClientCallServiceImpl.AGAVE_NAMESPACE;
    static final String AGAVE_VERSION = LapdogClientCallServiceImpl.AGAVE_VERSION;
    private static final String METADATA_NAME = "_";

    private Unmarshaller unmarshaller;

    private final Bridge bridge;
    private final Map<SimpleTypeName, CustomTypeFactory> customFactories;
    private final Executor threadPool;

    ActionModel(Bridge bridge) {
        this.bridge = bridge;
        customFactories = new HashMap<SimpleTypeName, CustomTypeFactory>();
        ThreadFactory tf = new NamedThreadFactory(getClass());
        threadPool = Executors.newCachedThreadPool(tf);
    }

    private TypeLoaderPublisher getLoaderPublisher() {
        return bridge.getActionLoaderPublisher();
    }

    private ExecutorMap getExecutorMap() {
        return bridge.getExecutorMap();
    }

    private TypeCache getTypeCache() {
        return bridge.getTypeCache();
    }

    private ActionModelFactory getFactory() {
        return bridge.getActionModelFactory();
    }

    /**
     * Factory method used to get an action model object when the PAL system is
     * offline. The resulting action model will be crippled because it can't
     * interact with the rest of the PAL system; however, it will still be
     * useful for purposes such as loading action model XML files and accessing
     * their contents.
     *
     * @return an offline action model
     */
    public static ActionModel offlineInstance() {
        try {
            Bridge bridge = Bridge.offlineInstance();
            return bridge.getActionModel();
        } catch (SpineException e) {
            String msg = "Unexpected error building ActionModel";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private synchronized void initJaxb() {
        if (unmarshaller == null) {
            try {
                JAXBContext jc = JAXBContext.newInstance(ActionModelType.class
                        .getPackage().getName());
                unmarshaller = jc.createUnmarshaller();
            } catch (JAXBException e) {
                String msg = "Cannot create JAXB context or unmarshaller";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
            SchemaFactory schemaFactory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL schemaUrl = ActionModel.class.getResource(XSD_NAME);
            try {
                Schema schema = schemaFactory.newSchema(schemaUrl);
                unmarshaller.setSchema(schema);
            } catch (SAXException e) {
                log.warn(
                        "Cannot load ActionModel.xsd schema from " + schemaUrl,
                        e);
            }
        }
    }

    /**
     * Loads a set of types and actions from an XML-formatted file. This method
     * will also add those types and actions to the action model, so it is not
     * necessary to call <code>addType</code>.
     *
     * @param source
     *            the location from which to load the new definitions
     * @param namespace
     *            the namespace in which to load the actions. Typically this
     *            will be named after the application, but it can also be
     *            <code>null</code>.
     * @return the new types and actions which were loaded from the given
     *         resource
     * @throws PALException
     *             if an error occurs in parsing the XML
     */
    public Set<ActionModelDef> load(URL source,
                                    String namespace)
            throws PALException {
        return load(source, namespace, new HashSet<String>());
    }

    private Set<ActionModelDef> load(URL source,
                                     String namespace,
                                     Set<String> loadedUrls)
            throws PALException {
        initJaxb();
        JAXBElement<?> ele;
        synchronized (unmarshaller) {
            ValidationEventCollector vec = new ValidationEventCollector();
            try {
                unmarshaller.setEventHandler(vec);
            } catch (JAXBException e) {
                log.warn("Cannot set validation event handler for schema", e);
            }

            try {
                ele = (JAXBElement<?>) unmarshaller.unmarshal(source);
            } catch (JAXBException e) {
                String msg = "XML parse error in " + source
                        + " (see log for details)";
                log.info(msg, e);
                throw new PALException(msg, e);
            } finally {
                for (ValidationEvent ve : vec.getEvents()) {
                    String msg = ve.getMessage();
                    ValidationEventLocator vel = ve.getLocator();
                    int line = vel.getLineNumber();
                    int column = vel.getColumnNumber();
                    log.warn("XML parse error detail: line " + line + ", col "
                            + column + ": " + msg);
                }
            }
        }
        ActionModelType amXml = (ActionModelType) ele.getValue();
        return load(source, amXml, namespace, true, loadedUrls);
    }

    /**
     * Non-public load method which doesn't try to store the loaded types into
     * persistent storage. This is intended to be used by persistent storage
     * implementations that need to read XML-formatted type information without
     * calling back to themselves to store what they're reading.
     *
     * @param sourceStr
     *            string containing XML action model definitions
     * @param namespace
     *            the namespace to put these definitions in
     * @return the definitions in the provided XML, plus any other definitions
     *         which are required by those
     * @throws PALException
     *             for XML parse errors
     */
    Set<ActionModelDef> read(String sourceStr,
                             String namespace)
            throws PALException {
        initJaxb();
        JAXBElement<?> ele;

        synchronized (unmarshaller) {
            ValidationEventCollector vec = new ValidationEventCollector();
            try {
                unmarshaller.setEventHandler(vec);
            } catch (JAXBException e) {
                log.warn("Cannot set validation event handler for schema", e);
            }

            Reader in = new StringReader(sourceStr);
            try {
                ele = (JAXBElement<?>) unmarshaller.unmarshal(in);
            } catch (Exception e) {
                String msg = "XML parse error in " + sourceStr
                        + " (see log for details)";
                log.info(msg, e);
                throw new PALException(msg, e);
            } finally {
                for (ValidationEvent ve : vec.getEvents()) {
                    String msg = ve.getMessage();
                    ValidationEventLocator vel = ve.getLocator();
                    int line = vel.getLineNumber();
                    int column = vel.getColumnNumber();
                    log.info("XML parse error detail: line " + line + ", col "
                            + column + ": " + msg);
                }

                try {
                    in.close();
                } catch (IOException e) {
                    log.warn("Unable to close StringReader " + in, e);
                }
            }
        }
        ActionModelType amXml = (ActionModelType) ele.getValue();
        return load(null, amXml, namespace, false, new HashSet<String>());
    }

    /**
     * Loads a set of types and actions from an XML-formatted string definition.
     * This method will also add those types and actions to the action model, so
     * it is not necessary to call <code>addType</code>.

     * @param source
     *            the XML string which defines the new definitions
     * @param namespace
     *            the namespace in which to load the actions. Typically this
     *            will be named after the application, but it can also be
     *            <code>null</code>.
     * @return the new types and actions which were loaded
     * @throws PALException
     *             if an XML parsing error occurs
     */
    public Set<ActionModelDef> load(String source,
                                    String namespace)
            throws PALException {
        initJaxb();
        JAXBElement<?> ele;
        synchronized (unmarshaller) {
            ValidationEventCollector vec = new ValidationEventCollector();
            try {
                unmarshaller.setEventHandler(vec);
            } catch (JAXBException e) {
                log.warn("Cannot set validation event handler for schema", e);
            }

            Reader in = new StringReader(source);
            try {
                ele = (JAXBElement<?>) unmarshaller.unmarshal(in);
            } catch (JAXBException e) {
                String msg = "XML parse error in supplied string";
                log.info(msg + ": " + source, e);
                throw new PALException(msg, e);
            } finally {
                for (ValidationEvent ve : vec.getEvents()) {
                    String msg = ve.getMessage();
                    ValidationEventLocator vel = ve.getLocator();
                    int line = vel.getLineNumber();
                    int column = vel.getColumnNumber();
                    log.warn("XML parse error detail: line " + line + ", col "
                            + column + ": " + msg);
                }
            }
        }
        ActionModelType amXml = (ActionModelType) ele.getValue();
        return load(amXml, namespace);
    }

    /**
     * Loads a set of types and actions from an XML structure. This method will
     * also add those types and actions to the action model, so it is not
     * necessary to call <code>addType</code>.
     *
     * @param source
     *            a JAXB structure representing the new actions and types to
     *            load
     * @param namespace
     *            the namespace in which to load the actions. Typically this
     *            will be named after the application, but it can also be
     *            <code>null</code>.
     * @return any new types or actions which were loaded
     * @throws PALException
     *             if an error occurs in parsing the XML
     */
    public Set<ActionModelDef> load(ActionModelType source,
                                    String namespace)
            throws PALException {
        return load(null, source, namespace, true, new HashSet<String>());
    }

    private Set<ActionModelDef> load(URL contextUrl,
                                     ActionModelType source,
                                     String namespace,
                                     boolean storeTypes,
                                     Set<String> loadedUrls)
            throws PALException {
        List<ActionModelDef> result = new ArrayList<ActionModelDef>();

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace must not be null");
        }

        String version = source.getVersion();
        log.debug("Loading action model {} version {} with context {}",
                new Object[] { namespace, version, contextUrl });

        /* Handle required (#included) action model files. */
        for (RequireType require : source.getRequire()) {
            String relativeUrl = require.getUrl();
            String requiredNameStr = require.getName();

            if (relativeUrl == null && requiredNameStr == null) {
                throw new IllegalArgumentException(
                        "Must specify either url or name in <require/>");
            }
            if (relativeUrl != null && requiredNameStr != null) {
                throw new IllegalArgumentException("Cannot specify both url ("
                        + relativeUrl + ") and name (" + requiredNameStr
                        + ") in <require/>");
            }

            if (relativeUrl != null) {
                URL url;
                try {
                    url = new URL(contextUrl, relativeUrl);
                } catch (MalformedURLException e) {
                    String msg = "URL " + relativeUrl
                            + " can't be resolved relative to " + contextUrl;
                    log.info(msg, e);
                    throw new PALException(msg, e);
                }
                if (loadedUrls.contains(url.toExternalForm())) {
                    log.debug("Skipping already-loaded file {}", url);
                } else {
                    loadedUrls.add(url.toExternalForm());
                    Set<ActionModelDef> subTypes = load(url, namespace, loadedUrls);
                    log.debug("Loaded {} types from context URL {}",
                            subTypes.size(), relativeUrl);
                    result.addAll(subTypes);
                }
            }

            if (requiredNameStr != null) {
                TypeName requiredName = TypeNameFactory
                        .makeName(requiredNameStr);
                if (!(requiredName instanceof SimpleTypeName)) {
                    throw new PALException(
                            "Cannot require a complex type name: "
                                    + requiredName);
                }
                ActionModelDef type = getSimpleType((SimpleTypeName) requiredName);
                if (type == null) {
                    throw new PALException("Unable to load type "
                            + requiredName);
                } else {
                    result.add(type);
                }
            }
        }

        /* Action model metadata */
        Map<String, String> metadata = new HashMap<String, String>();
        for (MetadataType metaXml : source.getMetadata()) {
            String key = metaXml.getKey();
            String value = metaXml.getValue();
            metadata.put(key, value);
        }
        if (!metadata.isEmpty()) {
            addNamespaceMetadata(namespace, version, metadata);
        }

        /* Types */
        for (TypeType typeXml : source.getType()) {
            TypeDef type = loadType(typeXml, version, namespace);
            result.add(type);
            if (storeTypes) {
                storeType((SimpleTypeName) type.getName(), type);
            }
            getTypeCache().add(type.getAtr());
        }

        /* Check these types for equivalence problems. */
        for (ActionModelDef def : result) {
            if (!(def instanceof TypeDef)) {
                continue;
            }
            TypeDef type = (TypeDef) def;
            for (TypeName equivTypeName : type.getEquivalentTypeNames()) {
                ActionModelDef equivType = getType(equivTypeName);
                if (equivType == null) {
                    log.warn("Type {} is equivalent to unknown type {}",
                            type.getName(), equivTypeName);
                } else {
                    if (!type.getClass().equals(equivType.getClass())) {
                        log.warn("Type {} ({}) is equivalent"
                                + " to {} ({}) -- expect"
                                + " execution errors converting between types",
                                new Object[] { type.getName(), type.getClass(),
                                        equivTypeName, equivType.getClass() });
                    }
                }
            }
        }

        /* Constraint declarations */
        for (ConstraintDeclarationType cdt : source.getConstraintDecl()) {
            ATRFunctionDeclaration atr = getFactory().toAtr(cdt, version,
                    namespace);
            ConstraintDef cd = (ConstraintDef) getFactory().makeActionModelDef(
                    atr, version, namespace);
            result.add(cd);
            if (storeTypes) {
                storeType(cd.getName(), cd);
            }
            getTypeCache().add(cd.getAtr());
        }

        /* Action families */
        for (FamilyType ft : source.getFamily()) {
            ATRActionDeclaration atr = getFactory().toAtr(ft, version,
                    namespace);
            ActionFamilyDef fam = (ActionFamilyDef) getFactory()
                    .makeActionModelDef(atr, version, namespace);
            result.add(fam);
            if (storeTypes) {
                storeType(fam.getName(), fam);
            }
            getTypeCache().add(atr);
        }

        /* Actions */
        for (ActionType actionXml : source.getAction()) {
            ATRActionDeclaration atr = getFactory().toAtr(actionXml, version,
                    namespace);
            ActionDef action = (ActionDef) getFactory().makeActionModelDef(atr,
                    version, namespace);
            result.add(action);
            if (storeTypes) {
                storeType(action.getName(), action);
            }
            getTypeCache().add(action.getAtr());
        }

        /* Idioms */
        for (IdiomType it : source.getIdiom()) {
            ATRActionDeclaration atr = getFactory().toAtr(it, version,
                    namespace);
            IdiomDef idiom = (IdiomDef) getFactory().makeActionModelDef(atr,
                    version, namespace);
            result.add(idiom);
            if (storeTypes) {
                storeType(idiom.getName(), idiom);
            }
            getTypeCache().add(idiom.getAtr());
        }

        Set<TypeName> names = new HashSet<TypeName>();
        for (ActionModelDef def : result) {
            TypeName name = def.getName();
            if (names.contains(name)) {
                throw new PALException("Duplicate definitions named " + name
                        + " in " + contextUrl);
            }
            names.add(name);
        }

        log.debug("Added {} types: {}", result.size(), result);
        Set<ActionModelDef> resultSet = new HashSet<ActionModelDef>();
        resultSet.addAll(result);
        return resultSet;
    }

    private TypeDef loadType(TypeType xml,
                             String version,
                             String namespace)
            throws PALException {

        // Check for use of incomplete features
        if (xml.getNullable() != null)
            log.warn("Currently all types in task learning are implicitly " +
                     "nullable. Therefore, explicitly 'nullable' types are " +
                     "not fully implemented or tested. Use of nullable type " +
                     "declarations is highly discouraged.");
        if (xml.getStruct() != null && xml.getStruct().getInherit() != null)
            log.warn("Structure type inheritance is not yet fully supported " +
                     "in the task learning system. Its use may result in " +
                     "system instability and is highly discouraged.");

        ATRTypeDeclaration atr = getFactory().toAtr(xml, version, namespace);

        if (TypeUtil.isCustom(atr) && !TypeUtil.isCustomSubType(atr)) {
            SimpleTypeName name = TypeUtil.getName(atr);
            synchronized (this) {
                if (getCustomTypeFactory(name) == null) {
                    log.info("Automatically registering ToStringFactory to handle custom type {}", name);
                    String className = TypeUtil.getRepresentationClassName(atr);
                    ToStringFactory fact = new ToStringFactory(className);
                    registerCustomTypeFactory(name, fact);
                }
            }
        }
        try {
            TypeDef type = (TypeDef) getFactory().makeActionModelDef(atr,
                    version, namespace);
            return type;
        } catch (Exception e) {
            throw new PALException("Unable to create type from "
                    + ATRSyntax.toSource(atr), e);
        }
    }

    /**
     * Provides all the currently known, non-transient types and actions in the
     * action model. This generally means the contents of the action model which
     * was loaded using {@link #load}.
     *
     * @return all types and non-transient actions which are registered at this
     *         time
     * @throws PALException
     *             if one or more types cannot be loaded
     * @see ActionDef#isTransient
     * @see #listTypes
     */
    public Set<ActionModelDef> getTypes()
            throws PALException {
        Set<ActionModelDef> result = new HashSet<ActionModelDef>();
        for (SimpleTypeName name : listTypes()) {
            ActionModelDef type = getType(name);
            result.add(type);
        }
        return result;
    }

    /**
     * Retrieves type information from remote {@link TypeStorage} instances
     * which are connected to the PAL system.
     *
     * @param subset
     *            which subset(s) of the known types to return. {@code null}
     *            means return everything.
     * @return all applicable known types from all known remote type loaders
     * @throws PALException
     *             if an error occurs
     * @see #getTypes
     */
    public Set<SimpleTypeName> listTypes(TypeStorage.Subset... subset)
            throws PALException {
        Set<SimpleTypeName> types = getLoaderPublisher().list(subset);

        /*
         * We need to remove the METADATA_NAME entry. It would be nice to simply
         * remove it, but we might be working with an immutable set. So do a
         * selective copy instead.
         */
        Set<SimpleTypeName> result = new HashSet<SimpleTypeName>();
        for (SimpleTypeName name : types) {
            if (name.getSimpleName().equals(METADATA_NAME)) {
                continue;
            }
            result.add(name);
        }

        log.debug("Results: {}", result);
        return result;
    }

    /**
     * Stores or removes a type into a (possibly) remote {@link TypeStorage}
     * instance. If the type already exists, it will be overwritten. If no
     * {@code TypeStorage} is registered, this method will throw an exception.
     * The {@code TypeStorage} may refuse to store or remove the type, which
     * will be indicated by the return value of this method.
     *
     * @param name
     *            the name of the type to store or remove
     * @param type
     *            the type to store, or {@code null} to remove the named type
     * @return {@code false} if the {@code TypeStorage} refused to make the
     *         requested change
     * @throws PALException
     *             if no {@code TypeStorage} is registered, or a communication
     *             error occurs.
     */
    public synchronized void storeType(SimpleTypeName name,
                                       ActionModelDef type)
            throws PALException {
        if (TypeNameFactory.isPrimitive(name)) {
            return;
        }
        String typeStr;
        if (type == null) {
            if (getExecutorMap() != null) {
                getExecutorMap().remove(name);
            }
            getTypeCache().remove(name);
            typeStr = null;
        } else {
            if (!name.equals(type.getName())) {
                throw new IllegalArgumentException("New type's name "
                        + type.getName() + " doesn't match supplied name "
                        + name);
            }

            /* Should we try to store all its dependent types also? */

            getTypeCache().add(type.getAtr());
            typeStr = type.getXml();
        }

        getLoaderPublisher().put(name, typeStr);
    }

    /**
     * Retrieve a specified type (or action or constraint) from the action
     * model. This will query the local type cache, any local action loaders,
     * and any other components of the PAL system which are available. This
     * method may block waiting for a response.
     *
     * @param typeName
     *            the name of the type to retrieve
     * @return the requested type, or null
     */
    /*
     * TODO Exclude any other threads from calling this method to fetch the same
     * type. When this thread finishes, the desired item will be in the cache
     * and any blocked threads can pick it up from there.
     */
    public ActionModelDef getType(TypeName typeName)
            throws PALException {

        // If it's a simple name, retrieve it.
        if (typeName instanceof SimpleTypeName) {
            return getSimpleType((SimpleTypeName) typeName);
        }

        // It's a name expression like list<foo>. Retrieve foo, then build list.
        TypeNameExpr nameExpr = (TypeNameExpr) typeName;
        TypeDef innerType = (TypeDef) getType(nameExpr.getInner());
        if (innerType == null) {
            throw new PALException("Unable to retrieve " + nameExpr.getInner()
                    + " for " + nameExpr);
        }
        return getFactory().makeCollectionType(nameExpr.getOuter(), innerType);
    }

    /**
     * Retrieve a specified simple type (or action or constraint) from the
     * action model. A simple type is one which is not constructed on demand,
     * such as {@code list<MyType>}. In other words, this method retrieves a
     * type which is referred to using a {@link SimpleTypeName} rather than a
     * {@link TypeNameExpr}.
     *
     * @param typeName
     *            the name of the type to retrieve
     * @return the requested type, or {@code null}
     * @throws PALException
     *             if an error occurs retrieving the type
     */
    public ActionModelDef getSimpleType(SimpleTypeName typeName)
            throws PALException {
        log.debug("Getting {}", typeName);

        // If it's a predefined primitive, return it.
        if (TypeNameFactory.isPrimitive(typeName)) {
            Predefined predef = Predefined.valueOf(typeName.getFullName()
                    .toUpperCase());
            return PrimitiveTypeDef.getPrimitive(predef, bridge);
        }

        ATR atr = getTypeCache().get(typeName);
        try {
            if (atr != null) {
                return getFactory().makeActionModelDef(atr,
                        typeName.getVersion(), typeName.getNamespace());
            }
        } catch (PALActionMissingException e) {
            throw new RuntimeException("Unable to create type " + typeName, e);
        }
        ActionModelDef type = getLoaderPublisher().getType(typeName);

        /*
         * If it's an idiom name with template, try to retrieve just the base
         * idiom name.
         */
        if (type == null) {
            SimpleTypeName baseName = typeName.getIdiomBaseName();
            if (!baseName.equals(typeName)) {
                type = getSimpleType(baseName);
            }
        }

        /* Add it to the cache and return it. */
        if (type != null && type.getAtr() != null) {
            getTypeCache().add(type.getAtr());
        }
        return type;
    }

    /**
     * Asynchronous version of {@link #getType}.
     *
     * @param callbackHandler
     *            callback handler to receive the retrieved type
     * @param typeName
     *            the name of the type to retrieve
     * @return a canceler which can be used to abort the retrieval
     */
    public RequestCanceler getType(final CallbackHandler<ActionModelDef> callbackHandler,
                                   TypeName typeName) {
        if (typeName instanceof SimpleTypeName) {
            return getSimpleType(callbackHandler, (SimpleTypeName) typeName);
        }

        /*
         * Asynchronously fetch the inner type and assemble the desired type
         * from it.
         */
        final TypeNameExpr collName = (TypeNameExpr) typeName;
        AsyncChain<ActionModelDef, ActionModelDef> chain = new AsyncChain<ActionModelDef, ActionModelDef>(
                callbackHandler) {
            @Override
            public void results(ActionModelDef result) {
                TypeDef innerType = (TypeDef) result;
                TypeDef collType = getFactory().makeCollectionType(
                        collName.getOuter(), innerType);
                callbackHandler.result(collType);
            }
        };
        chain.addCanceler(getType(chain, collName.getInner()));
        return chain;
    }

    private RequestCanceler getSimpleType(final CallbackHandler<ActionModelDef> callbackHandler,
                                          final SimpleTypeName typeName) {
        log.debug("(Async) Getting {}", typeName);

        // If it's a predefined primitive, return it.
        if (TypeNameFactory.isPrimitive(typeName)) {
            Predefined predef = Predefined.valueOf(typeName.getFullName()
                    .toUpperCase());
            PrimitiveTypeDef result = PrimitiveTypeDef.getPrimitive(predef, bridge);
            callbackHandler.result(result);
            return new RequestCanceler() {
                @Override
                public void cancel() {
                }
            };
        }

        ATR atr = getTypeCache().get(typeName);
        try {
            if (atr != null) {
                ActionModelDef result = getFactory().makeActionModelDef(atr,
                        typeName.getVersion(), typeName.getNamespace());
                callbackHandler.result(result);
                return new RequestCanceler() {
                    @Override
                    public void cancel() {
                    }
                };
            }
        } catch (PALException e) {
            throw new RuntimeException("Unable to create type " + typeName, e);
        }

        final AsyncChain<ActionModelDef, ActionModelDef> chain = new AsyncChain<ActionModelDef, ActionModelDef>(
                callbackHandler) {
            @Override
            public void results(ActionModelDef result) {
                /* Add it to the cache and return it. */
                if (result != null) {
                    getTypeCache().add(result.getAtr());
                }
                callbackHandler.result(result);
            }

            @Override
            public void error(ErrorInfo err) {
                /*
                 * If it's an idiom name with template, try to retrieve just the
                 * base idiom name.
                 */
                if (err.getErrorId() == ErrorType.NOT_ALL_LOADED.ordinal()) {
                    SimpleTypeName baseName = typeName.getIdiomBaseName();
                    if (!baseName.equals(typeName)) {
                        try {
                            ActionModelDef result = getSimpleType(baseName);
                            /* Null check: */
                            result.getActionModel();
                            callbackHandler.result(result);
                        } catch (Exception e) {
                            log.debug("Retrieving idiom base " + baseName, e);
                            ErrorInfo error = ErrorFactory.error(bridge
                                    .getSpine().getClientId(),
                                    ErrorType.ACTION_MODEL, baseName);
                            callbackHandler.error(error);
                        }
                    } else {
                        callbackHandler.error(err);
                    }
                } else {
                    callbackHandler.error(err);
                }
            }
        };
        getLoaderPublisher().load(chain, typeName);
        return chain;
    }

    RequestCanceler getTypes(CallbackHandler<Set<ActionModelDef>> callbackHandler,
                             Set<SimpleTypeName> typeNames) {
        log.debug("Will load {}", typeNames);
        AsyncTypeGetter getter = new AsyncTypeGetter(callbackHandler, typeNames);
        threadPool.execute(getter);
        return getter;
    }

    /**
     * Registers an executor which will handle the named action. If an executor
     * for this action is already registered, this method will throw a
     * PALDuplicateExecutorException.
     *
     * @param name
     *            the name of the action which the executor agrees to handle
     * @param executor
     *            the executor which will be tasked with execution requests
     * @throws PALDuplicateExecutorException
     *             if an executor for this action is already registered
     * @throws PALException
     *             if a communication error occurs trying to determine if an
     *             executor already is registered
     * @see #unregisterExecutor
     * @see #getExecutor
     */
    public void registerExecutor(SimpleTypeName name,
                                 ActionExecutor executor)
            throws PALDuplicateExecutorException,
            PALException {
        try {
            getExecutorMap().put(name, executor);
        } catch (SpineException e) {
            throw new PALException(
                    "Unable to determine if an executor already exists for "
                            + name);
        }
    }

    /**
     * Reverses a registration made with {@link #registerExecutor}.
     *
     * @param name
     *            the name of the action to unregister for
     * @see #getExecutor
     */
    public void unregisterExecutor(SimpleTypeName name) {
        getExecutorMap().remove(name);
    }

    /**
     * Retrieves the executor which is assigned to handle a particular action.
     * Note that the action may belong to an executor in another JVM or Bridge
     * instance, in which case this method will return {@code null}. If the name
     * represents a Lumen procedure, this method will return a reference to the
     * PAL executor, also available via {@link Bridge#getPALExecutor}.
     *
     * @param name
     *            the name of the action to query
     * @return the executor for the given action, or {@code null}
     * @see #registerExecutor
     * @see #unregisterExecutor
     */
    public ActionExecutor getExecutor(SimpleTypeName name) {
        return getExecutorMap().get(name);
    }

    Set<ActionExecutor> getExecutors() {
        return getExecutorMap().getAll();
    }

    boolean hasRemoteExecutor(SimpleTypeName name)
            throws SpineException {
        Spine spine = bridge.getSpine();
        TransactionUID uid = spine.getNextUid();
        ExecutorListQuery execQuery = new ExecutorListQuery(
                spine.getClientId(), uid, name);
        Message[] responses = spine.gather(execQuery, Spine.DEFAULT_TIMEOUT);

        for (Message message : responses) {
            ExecutorListResult result = (ExecutorListResult) message;
            if (result.isExecutor()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Registers a factory which will be responsible for translating instances
     * of custom types between their normal object representations and their
     * string representations.
     *
     * @param name
     *            the name of the type which this factory will be responsible
     *            for
     * @param factory
     *            the factory which will convert objects of this type
     * @see #getCustomTypeFactory
     */
    public void registerCustomTypeFactory(SimpleTypeName name,
                                          CustomTypeFactory factory) {
        CustomTypeFactory oldFactory = getCustomTypeFactory(name);
        if (oldFactory != null) {
            log.error(
                    "Changing custom type factory for {} from {} to {} -- something bad will probably happen",
                    new Object[] { name, oldFactory, factory });
        }
        customFactories.put(name, factory);
    }

    /**
     * Retrieves the factory for a given custom type.
     *
     * @param name
     *            the name of the type to retrieve the factory for
     * @return the factory associated with the given type
     * @see #registerCustomTypeFactory
     */
    public CustomTypeFactory getCustomTypeFactory(SimpleTypeName name) {
        return customFactories.get(name);
    }

    /**
     * Add new namespace metadata entries. Each version of each namespace can
     * have its own set of string-string key-value metadata entries. Any new
     * entries given to this method will be added to entries that already exist,
     * if any.
     *
     * @param ns
     *            the namespace to which the entries will be added
     * @param vers
     *            the version of the namespace to add the entries to
     * @param metadata
     *            new metadata entries to add
     * @throws PALException
     *             if an error occurs
     * @see #getNamespaceMetadata
     */
    public void addNamespaceMetadata(String ns,
                                     String vers,
                                     Map<String, String> metadata)
            throws PALException {
        Map<String, String> newMeta = new HashMap<String, String>();
        newMeta.putAll(metadata);
        Map<String, String> oldMeta = getNamespaceMetadata(ns, vers);
        if (oldMeta != null) {
            newMeta.putAll(oldMeta);
        }

        /*
         * Build an ATR structure representing an alias type that will hold our
         * metadata.
         */
        CTRConstructor ctrBuilder = CTRConstructor.getInstance();
        SimpleTypeName name = new SimpleTypeName(METADATA_NAME, vers, ns);
        String nameStr = name.getFullName();
        List<String> equivTypes = new ArrayList<String>();
        equivTypes.add("string");
        Map<String, ATRTerm> propMap = new HashMap<String, ATRTerm>();
        for (String key : newMeta.keySet()) {
            String value = newMeta.get(key);
            ATRTerm term = ctrBuilder.createLiteral(value, null);
            propMap.put(key, term);
        }
        propMap.put(ActionModelDef.DESCRIPTION,
                ctrBuilder.createLiteral("Action model metadata", null));
        ATRMap props = ctrBuilder.createMap(propMap);
        ATRTypeDeclaration type = CTRTypeDeclaration.createAliasType(nameStr,
                equivTypes, props);
        ActionModelDef def = getFactory().makeActionModelDef(type, vers, ns);
        storeType(name, def);
    }

    /**
     * Retrieves the metadata for a given namespace. This metadata is specified
     * in top-level <code>&lt;metadata/&gt;</code> tags in the action model XML
     * file.
     *
     * @param ns
     *            the namespace to retrieve metadata for
     * @param vers
     *            the version of the given namespace to retrieve metadata for
     * @return the metadata for the requested namespace, or <code>null</code> if
     *         the namespace is unknown
     * @throws PALException
     *             if an error occurs
     */
    public Map<String, String> getNamespaceMetadata(String ns,
                                                    String vers)
            throws PALException {
        SimpleTypeName name = new SimpleTypeName(METADATA_NAME, vers, ns);
        ActionModelDef def = getType(name);
        Map<String, String> result = new HashMap<String, String>();
        if (def != null) {
            for (String key : def.listMetadataKeys()) {
                if (key.equals(ActionModelDef.DESCRIPTION)) {
                    continue;
                }
                String value = def.getMetadata(key);
                result.put(key, value);
            }
        }
        result = Collections.unmodifiableMap(result);
        return result;
    }

    /**
     * Attempts to register the Agave actions and types. Agave will try to
     * complete procedures during the learning process by finding relationships
     * between data values. A full explanation is beyond the scope of this
     * Javadoc.
     *
     * @return {@code true} if the Agave actions were successfully registered
     * @throws PALException
     *             if an unexpected error occurs
     */
    public boolean registerAgave()
            throws PALException {
        try {
            Class.forName(AGAVE_CLASS);
        } catch (Exception e) {
            log.debug("Not loading agave: " + e);
            return false;
        }

        /*
         * The Agave class exists on our classpath, so let's try to load Agave's
         * action model.
         */
        Set<ActionModelDef> loaded = load(getClass().getResource(AGAVE_AM),
                AGAVE_NAMESPACE);
        if (loaded.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Translate from an XML representation of constraint binding to the ATRTerm
     * representation. Because the XML can contain unqualified names -- names
     * not prefixed by namespace or version -- and ATR always contains qualified
     * names, this method needs to know how to resolve unqualified names.
     *
     * @param constraints
     *            the XML representation of a constraint binding
     * @param version
     *            the version of the action model to use for unqualified
     *            constraints
     * @param namespace
     *            the namespace of the action model to use for unqualified
     *            constraints
     * @return an ATR representation of the same constraint binding
     * @throws PALException
     *             if a referenced constraint can't be retrieved from the action
     *             model
     */
    public ATRNoEvalTerm constraintsToAtr(String constraints,
                                          String version,
                                          String namespace)
            throws PALException {
        initJaxb();
        JAXBElement<?> ele;

        synchronized (unmarshaller) {
            ValidationEventCollector vec = new ValidationEventCollector();
            try {
                unmarshaller.setEventHandler(vec);
            } catch (JAXBException e) {
                log.warn("Cannot set validation event handler for schema", e);
            }

            Reader in = new StringReader(constraints);
            try {
                ele = (JAXBElement<?>) unmarshaller.unmarshal(in);
            } catch (Exception e) {
                String msg = "XML parse error in " + constraints
                        + " (see log for details)";
                log.info(msg, e);
                throw new PALException(msg, e);
            } finally {
                for (ValidationEvent ve : vec.getEvents()) {
                    String msg = ve.getMessage();
                    ValidationEventLocator vel = ve.getLocator();
                    int line = vel.getLineNumber();
                    int column = vel.getColumnNumber();
                    log.info("XML parse error detail: line " + line + ", col "
                            + column + ": " + msg);
                }

                try {
                    in.close();
                } catch (IOException e) {
                    log.warn("Unable to close StringReader " + in, e);
                }
            }
        }
        ConstraintsType jaxb = (ConstraintsType) ele.getValue();

        return getFactory().atrConstraints(jaxb, version, namespace);
    }

    private class AsyncTypeGetter
            implements Runnable, RequestCanceler,
            CallbackHandler<ActionModelDef> {
        private final Set<SimpleTypeName> typeNames;
        private final Set<ActionModelDef> loadedTypes;
        private final CallbackHandler<Set<ActionModelDef>> callbackHandler;
        private final Set<RequestCanceler> cancelers;
        private boolean cancel = false;

        public AsyncTypeGetter(CallbackHandler<Set<ActionModelDef>> callbackHandler,
                               Set<SimpleTypeName> typeNames) {
            this.callbackHandler = callbackHandler;
            this.typeNames = new HashSet<SimpleTypeName>();
            for (SimpleTypeName name : typeNames) {
                name = name.getIdiomBaseName();
                this.typeNames.add(name);
            }
            cancelers = new HashSet<RequestCanceler>();
            loadedTypes = new HashSet<ActionModelDef>();
        }

        @Override
        public void cancel() {
            cancel = true;
            for (RequestCanceler rc : cancelers) {
                rc.cancel();
            }
        }

        @Override
        public void run() {
            if (typeNames.isEmpty()) {
                callbackHandler.result(loadedTypes);
            } else {
                for (SimpleTypeName name : typeNames) {
                    if (cancel) {
                        log.debug("Cancelled");
                        return;
                    }

                    RequestCanceler canceler = getType(this, name);
                    cancelers.add(canceler);
                }
            }
        }

        @Override
        public void result(ActionModelDef result) {
            if (typeNames.contains(result.getName())) {
                loadedTypes.add(result);
                if (loadedTypes.size() == typeNames.size()) {
                    callbackHandler.result(loadedTypes);
                }
            }
        }

        @Override
        public void error(ErrorInfo error) {
            callbackHandler.error(error);
        }
    }
}
