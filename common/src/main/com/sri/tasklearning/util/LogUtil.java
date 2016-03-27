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

/* $Id: LogUtil.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $ */
package com.sri.tasklearning.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Vector;

/**
 * Used internally to manage logging configuration. Because we're using SLF4J
 * for our logging, we don't want to reference the log4j jar file or its
 * classes. This class uses reflection to load the log4j configurators, so if
 * log4j isn't on the classpath, this will fail without throwing an exception.
 *
 * @author chris
 */
public class LogUtil {
    private static final Logger log = LoggerFactory.getLogger(LogUtil.class);
    private static boolean configured = false;
    private static final String DEFAULT_BASE_FILE = "log4j";
    private static final String EXT_XML = ".xml";
    private static final String EXT_PROPERTIES = ".properties";
    private static final String EXT_INI = ".ini";

    /**
     * Configure the log4j system with the config file. A series of config file
     * names are searched for in the current directory, followed by looking for
     * resources on the classpath relative to <code>baseClass</code>. Config
     * files can be named <code>moduleName.ext</code>, where ext is one of xml,
     * properties, or ini. As a fallback, <code>log4j.ext</code> will be
     * searched for. If all else fails, this will try to use
     * <code>org.apache.log4j.BasicConfigurator</code>.
     *
     * @param moduleName
     *            base name of the config file to be searched for
     * @param baseClass
     *            class to search for resources relative to
     */
    public static void configureLogging(String moduleName,
                                        Class<?> baseClass) {
        List<URL> candidateFiles = new Vector<URL>();
        candidateFiles.addAll(getFilesForModule(moduleName));
        candidateFiles.addAll(getFilesForModule(null));
        candidateFiles.addAll(getUrlsForModule(moduleName, baseClass));
        candidateFiles.addAll(getUrlsForModule(null, baseClass));
        candidateFiles.addAll(getUrlsForModule(null, LogUtil.class));

        for (URL url : candidateFiles) {
            if (url != null) {
                try {
                    InputStream is = url.openStream();
                    is.close();
                } catch (IOException e) {
                    continue;
                }
                configure(url);
                break;
            }
        }

        if(!configured) {
            configureDefault();
        }
    }

    public static boolean isLoggingConfigured() {
        return configured;
    }

    private static List<URL> getFilesForModule(String moduleName) {
        List<URL> result = new Vector<URL>();

        List<String> filenames = getFileNamesForModule(moduleName);
        for (String filename : filenames) {
            File file = new File(filename);
            URI uri = file.toURI();
            URL url;
            try {
                url = uri.toURL();
            } catch (MalformedURLException e) {
                log.error("Unexpected exception converting " + filename, e);
                continue;
            }
            result.add(url);
        }

        return result;
    }

    private static List<URL> getUrlsForModule(String moduleName,
                                              Class<?> baseClass) {
        List<URL> result = new Vector<URL>();

        List<String> filenames = getFileNamesForModule(moduleName);
        for (String filename : filenames) {
            URL url = baseClass.getResource(filename);
            result.add(url);
        }

        return result;
    }

    private static List<String> getFileNamesForModule(String moduleName) {
        List<String> result = new Vector<String>();

        String name = moduleName;
        if (name == null || name.equals("")) {
            name = DEFAULT_BASE_FILE;
        }

        result.add(name + EXT_XML);
        result.add(name + EXT_PROPERTIES);
        result.add(name + EXT_INI);

        return result;
    }

    private static void configure(URL url) {
        if (url.getFile().endsWith(EXT_XML)) {
            // DOMConfigurator.configure(url);
            doConfigure("org.apache.log4j.xml.DOMConfigurator", url);
        } else {
            // PropertyConfigurator.configure(url);
            doConfigure("org.apache.log4j.PropertyConfigurator", url);
        }
    }

    private static void configureDefault() {
        // BasicConfigurator.configure();
        doConfigure("org.apache.log4j.BasicConfigurator", null);
    }

    private static void doConfigure(String className,
                                    URL url) {
        if (configured) {
            log.warn("Already configured! Not re-configuring using "
                    + className + " and " + url);
        }
        System.out.println("Configuring using " + className + " and URL " + url);
        Class<?> config;
        try {
            config = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.err.println("Could not load log4j config class " + className
                    + "; log4j probably isn't in the classpath: " + e);
            return;
        }

        Class<?> argTypes[];
        Object args[];
        if (url == null) {
            argTypes = new Class<?>[0];
            args = new Object[0];
        } else {
            argTypes = new Class<?>[] { URL.class };
            args = new Object[] { url };
        }

        try {
            Method method = config.getMethod("configure", argTypes);
            method.invoke(null, args);
        } catch (InvocationTargetException e) {
            System.err.println("Error in log4j configurator with config " + url);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Couldn't invoke configure method on class " + className);
            e.printStackTrace();
        }
        log.info("Configured log4j using {} and URL {}", className, url);
        configured = true;
    }
}
