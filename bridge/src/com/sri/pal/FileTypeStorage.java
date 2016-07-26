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

// $Id: FileTypeStorage.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.pal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.sri.pal.common.CallbackHandler;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.RequestCanceler;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.spine.util.ErrorFactory;
import com.sri.tasklearning.spine.util.ErrorType;
import com.sri.tasklearning.spine.util.TypeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@code TypeStorage} that stores procedures in a directory,
 * with one file per stored procedure.
 *
 * @author chris
 */
public class FileTypeStorage
        implements TypeStorage {
    private static final Logger log = LoggerFactory
            .getLogger(FileTypeStorage.class);

    /* file extensions for procedures, actions, and types */
    private static final String PROC_EXT = ".procedure";
    private static final String ACTION_EXT = ".action";
    private static final String TYPE_EXT = ".type";
    private static final String FAMILY_EXT = ".family";
    private static final String IDIOM_EXT = ".idiom";
    private static final String CONSTRAINT_EXT = ".constraint";

    /**
     * Truncate names longer than this.
     */
    private static final int TRUNCATE_LEN = 20;

    /* Name of the properties file to write: */
    private static final String PROPERTIES = "storage.properties";

    /* Keys in the properties file: */
    private static final String NAMESPACE = "namespace";
    private static final String VERSION = "version";
    private static final String NAME = "name";

    private static final String COMMENTS = "This file was automatically generated by Task Learning's FileTypeStorage.";

    private static final String STORAGE_VERSION_KEY = "storage_version";
    private static final String STORAGE_VERSION = LumenProcedureDef.SERIALIZATION_FORMAT_VERSION;

    private final File rootDir;
    private final ErrorFactory errorFactory;

    public FileTypeStorage(File storageDir,
                           String clientId)
            throws PALException {
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        if (!storageDir.isDirectory()) {
            throw new PALException("Not a directory: " + storageDir);
        }
        rootDir = storageDir;
        errorFactory = new ErrorFactory(clientId);
    }

    /**
     * Provides access to the directory being used for the root of the
     * persistence storage. The files in this hierarchy are very sensitive to
     * modification, so modifying them is not recommended.
     *
     * @return the root directory used for persistence
     */
    public File getStorageDir() {
        return rootDir;
    }

    @Override
    public void putType(SimpleTypeName name,
                        String typeStr)
            throws PALException {
        log.debug("Got request for {}: {}", name, typeStr);
        File file = getExistingFile(name);
        if(file == null) {
            // What file extension will this file use?
            String extension;
            if (TypeUtil.isProcedureString(typeStr)) {
                extension = PROC_EXT;
            } else if (TypeUtil.isActionString(typeStr)) {
                extension = ACTION_EXT;
            } else if (TypeUtil.isActionFamilyString(typeStr)) {
                extension = FAMILY_EXT;
            } else if (TypeUtil.isIdiomString(typeStr)) {
                extension = IDIOM_EXT;
            } else if (TypeUtil.isConstraintString(typeStr)) {
                extension = CONSTRAINT_EXT;
            } else {
                extension = TYPE_EXT;
            }
            file = getNewFile(name, extension);
        }
        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new PALException("Unable to create directory " + dir
                    + " to hold " + name);
        }
        if (typeStr == null) {
            boolean result = false;
            synchronized (this) {
                try {
                    result = file.delete();
                    File propFile = new File(file.getParent(), PROPERTIES);
                    Properties props = readProps(propFile);
                    props.remove(NAME + "." + file.getName());
                    writeProps(propFile, props);
                } catch (IOException e) {
                    log.warn("Unable to delete " + file + " for " + name, e);
                }
            }
            if (result) {
                log.debug("Deleted {} for {}", file, name);
            } else {
                log.warn("Unable to delete {} for {}", file, name);
            }
        } else {
            try {
                writeFile(file, name, typeStr);
            } catch (IOException e) {
                throw new PALException("Unable to write file " + file, e);
            }
        }
    }

    @Override
    public RequestCanceler loadType(CallbackHandler<String> callbackHandler,
                                    SimpleTypeName name) {
        try {
            File file = getExistingFile(name);
            if (file == null) {
                ErrorInfo err = errorFactory.error(ErrorType.NOT_ALL_LOADED,
                        name.getFullName());
                callbackHandler.error(err);
            } else {
                String contents = readFile(file);
                callbackHandler.result(contents);
            }
        } catch (Exception e) {
            log.warn("Failed to load " + name, e);
            ErrorInfo error = errorFactory
                    .error(ErrorType.INTERNAL_FILE_LOADER);
            callbackHandler.error(error);
        }

        return new RequestCanceler() {
            @Override
            public void cancel() {
                /* Don't cancel, since we're already done. */
            }
        };
    }

    @Override
    public Set<SimpleTypeName> listTypes(Subset... subsets) throws PALException {
        Set<SimpleTypeName> result = new HashSet<SimpleTypeName>();

        Set<Subset> subsetsSet = new HashSet<Subset>();
        if (subsets == null || subsets.length == 0) {
            for (Subset item : Subset.values()) {
                subsetsSet.add(item);
            }
        } else {
            subsetsSet.addAll(Arrays.asList(subsets));
        }

        for(File nsDir : rootDir.listFiles()) {
            if (!nsDir.isDirectory())
                continue;
            for(File versDir : nsDir.listFiles()) {
                Properties props = null;
                File propsFile = new File(versDir, PROPERTIES);
                try {
                    props = readProps(propsFile);
                } catch (Exception e) {
                    throw new PALException("Couldn't read properties file "
                            + propsFile.getAbsolutePath(), e);
                }
                for(File file : versDir.listFiles()) {
                    String fname = file.getName();
                    boolean include = false;
                    if (fname.endsWith(PROC_EXT)) {
                        if (subsetsSet.contains(Subset.PROCEDURE)) {
                            include = true;
                        }
                    } else if (fname.endsWith(ACTION_EXT)) {
                        if (subsetsSet.contains(Subset.ACTION)) {
                            include = true;
                        }
                    } else if (fname.endsWith(TYPE_EXT)) {
                        if (subsetsSet.contains(Subset.TYPE)) {
                            include = true;
                        }
                    } else if (fname.endsWith(FAMILY_EXT)) {
                        if (subsetsSet.contains(Subset.FAMILY)) {
                            include = true;
                        }
                    } else if (fname.endsWith(IDIOM_EXT)) {
                        if (subsetsSet.contains(Subset.IDIOM)) {
                            include = true;
                        }
                    } else if (fname.endsWith(CONSTRAINT_EXT)) {
                        if (subsetsSet.contains(Subset.CONSTRAINT)) {
                            include = true;
                        }
                    } else if (fname.equals(PROPERTIES)) {
                        /* Do nothing. */
                    } else {
                        log.warn("Unknown file in storage directory: {}",
                                file.getAbsolutePath());
                    }

                    if (include) {
                        String simpleName = props.getProperty(NAME + "."
                                + file.getName());
                        String version = props.getProperty(VERSION);
                        String namespace = props.getProperty(NAMESPACE);
                        if (simpleName == null || version == null
                                || namespace == null) {
                            throw new PALException("Consistency error with "
                                    + file.getAbsolutePath());
                        }
                        SimpleTypeName name = new SimpleTypeName(simpleName,
                                version, namespace);
                        result.add(name);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Given the name of a TypeDef that is known to be stored in a file, return
     * the name of that file.
     *
     * @param name
     * @return the file, or {@code null} if none exists
     * @throws PALException
     * @throws IOException
     */
    private File getExistingFile(SimpleTypeName name)
            throws PALException {
        // Find the dir this file exists in.
        File nsDir = new File(rootDir, mangle(name.getNamespace()));
        System.out.println("Loading " + rootDir + " name " + name); 
        File versDir = new File(nsDir, mangle(name.getVersion()));
        if (!versDir.exists()) {
            return null;
        }

        // Get the truncated name of the file.
        String truncName = name.getSimpleName();
        if (truncName.length() > TRUNCATE_LEN) {
            truncName = truncName.substring(0, TRUNCATE_LEN);
        }

        // Look for files that start with truncName.
        for (File candidate : versDir.listFiles()) {
            if (candidate.getName().startsWith(truncName)) {
                SimpleTypeName candidateName = getName(candidate);
                if (candidateName.equals(name)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * Given a TypeName which is known to NOT be stored in a file, return the
     * name of a file that can be created to store it.
     *
     * @param name
     * @return
     */
    File getNewFile(SimpleTypeName name,
                    String extension) {
        // Find the dir this file exists in.
        File nsDir = new File(rootDir, mangle(name.getNamespace()));
        File versDir = new File(nsDir, mangle(name.getVersion()));

        // Get the truncated name of the file.
        String truncName = name.getSimpleName();
        if (truncName.length() > TRUNCATE_LEN) {
            truncName = truncName.substring(0, TRUNCATE_LEN);
        }

        // Find an unused name starting with truncName.
        for (int i = 0; true; i++) {
            String suffix;
            if (i == 0) {
                suffix = "";
            } else {
                suffix = "_" + i;
            }
            String filename = truncName + suffix + extension;
            File file = new File(versDir, filename);
            if (!file.exists()) {
                return file;
            }
            i++;
        }
    }

    /**
     * Given a file containing a serialized TypeDef, return the name of the
     * thing it contains.
     *
     * @param file
     * @return
     * @throws PALException
     */
    private SimpleTypeName getName(File file)
            throws PALException {
        File propFile = new File(file.getParent(), PROPERTIES);
        Properties props;
        try {
            props = readProps(propFile);
        } catch (IOException e) {
            throw new PALException("Couldn't read properties file "
                    + propFile.getAbsolutePath(), e);
        }
        String namespace = props.getProperty(NAMESPACE);
        String version = props.getProperty(VERSION);
        String name = props.getProperty(NAME + "." + file.getName());
        if(name == null) {
            throw new PALException("No name for " + file);
        }

        SimpleTypeName result = (SimpleTypeName) TypeNameFactory.makeName(name,
                version, namespace);
        return result;
    }

    private String mangle(String name) {
        return name.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private synchronized String readFile(File file)
            throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            String line = in.readLine();
            while (line != null) {
                buffer.append(line);
                buffer.append("\n");
                line = in.readLine();
            }
        } finally {
            in.close();
        }

        return buffer.toString();
    }

    private synchronized void writeFile(File file,
                                        SimpleTypeName typeName,
                                        String contents)
            throws IOException,
            PALException {
        FileWriter out = new FileWriter(file);
        try {
            out.write(contents);
        } finally {
            out.close();
        }

        /* Now update the properties file so we have accurate name info. */
        File propFile = new File(file.getParent(), PROPERTIES);
        Properties props = readProps(propFile);
        String namespace = props.getProperty(NAMESPACE);
        String version = props.getProperty(VERSION);

        if (namespace != null && !namespace.equals(typeName.getNamespace())) {
            throw new PALException("Namespace for " + file + " is " + namespace
                    + ", expected " + typeName.getNamespace());
        }
        if (version != null && !version.equals(typeName.getVersion())) {
            throw new PALException("Version for " + file + " is " + version
                    + ", expected " + typeName.getVersion());
        }

        props.setProperty(NAMESPACE, typeName.getNamespace());
        props.setProperty(VERSION, typeName.getVersion());
        props.setProperty(NAME + "." + file.getName(), typeName.getSimpleName());

        writeProps(propFile, props);
    }

    private synchronized Properties readProps(File propFile)
            throws IOException,
            PALSerializationVersionException {
        InputStream is = null;
        Properties props = new Properties();
        try {
            is = new FileInputStream(propFile);
            props.load(is);
        } catch (FileNotFoundException e) {
            log.debug("Properties file " + propFile + " not found");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("Error closing InputStream for " + propFile, e);
                }
            }
        }
        String vers = (String) props.get(STORAGE_VERSION_KEY);
        if (vers != null && !STORAGE_VERSION.equals(vers)) {
            throw new PALSerializationVersionException(
                    "Persisted data is version " + vers + "; expected "
                            + STORAGE_VERSION);
        }
        return props;
    }

    private synchronized void writeProps(File propFile,
                                         Properties props)
            throws IOException {
        props.put(STORAGE_VERSION_KEY, STORAGE_VERSION);
        OutputStream os = null;
        try {
            os = new FileOutputStream(propFile);
            props.store(os, COMMENTS);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    log.warn("Error closing OutputStream for " + propFile, e);
                }
            }
        }
    }
}
