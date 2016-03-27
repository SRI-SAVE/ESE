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

package com.sri.tasklearning.ui.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows for the specification and retrieval of properties in the 
 * form of key/value pairs. Properties are persisted across processes by way of
 * a text file. 
 */
public final class Properties {
    private static final Logger log = LoggerFactory
            .getLogger(ProcedureEditController.class); 
    public static final String DEFAULT_FILE_NAME = "properties.Editor.txt";

    private HashMap<String, Serializable> props = new HashMap<String, Serializable>();
    private File file;

    public Properties(String fileName) {
        file = new File(fileName);
        loadProperties();
    }

    @SuppressWarnings("unchecked")
    public void loadProperties() {
        ObjectInputStream fileIn = null;
        try {
            fileIn = new ObjectInputStream(new FileInputStream(file));
            Object o = fileIn.readObject();
            if (o instanceof HashMap<?, ?>) {
                props = (HashMap<String, Serializable>) o;
            } else {
                throw new IOException("File " + file.getName()
                        + " not formatted correctly.");
            }
            fileIn.close();
        } catch (IOException e) {
            // File didn't exist, so create an empty properties hash
            props = new HashMap<String, Serializable>(); 
        } catch (ClassNotFoundException e) {
            log.error("Reading properties file " + file.getName() + " failed", e);
        } finally {
            if (fileIn != null) {
                try {
                    fileIn.close();
                } catch (IOException e) {
                    log.warn("Error closing file " + fileIn, e);
                }
            }
        }
    }

    public void saveProperties() {
        try {
            ObjectOutputStream fileOut = new ObjectOutputStream(
                    new FileOutputStream(file));
            fileOut.writeObject(props);
            fileOut.flush();
            fileOut.close();
        } catch (IOException e) {
            log.error("Writing to properties file " + file.getName()
                    + " failed" + e);
        }
    }

    public Serializable getValue(String key, Serializable defVal) {
        Serializable val = props.get(key);
        if (val == null) {
            return defVal;
        } else {
            return val;
        }
    }

    public void putValue(String key, Serializable value) {
        props.put(key, value);
    }
}
