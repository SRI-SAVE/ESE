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

// $Id: ToStringFactory.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs instances of objects which have a reversible {@code toString()}
 * method. In other words, the return value of {@link Object#toString} can be
 * used as the argument to either a constructor or a
 * {@code static valueOf(String)} method. This is true for many Java primitive
 * wrapper classes, such as {@code Integer} and {@code Boolean}.
 */
public class ToStringFactory
        implements CustomTypeFactory {
    private static final Logger log = LoggerFactory
            .getLogger(ToStringFactory.class);

    private final String className;
    private final Set<String> reportedClasses;

    /**
     * Creates a new custom type factory which will convert between string
     * representation and object representation of the given class.
     *
     * @param className
     *            the class to convert instances of
     */
    public ToStringFactory(String className) {
        this.className = className;
        reportedClasses = new HashSet<String>();

        Class<?> javaClass = getJavaClass();
        if (javaClass == null) {
            return;
        }
        Constructor<?> con = null;
        try {
            con = javaClass.getConstructor(String.class);
        } catch (SecurityException e) {
            log.warn("Couldn't introspect " + javaClass + " for constructor", e);
        } catch (NoSuchMethodException e) {
            log.debug("No string-taking constructor for " + javaClass.getName()
                    + ": " + e);
        }
        Method meth = null;
        try {
            meth = javaClass.getMethod("valueOf", String.class);
            if (meth != null && !Modifier.isStatic(meth.getModifiers())) {
                meth = null;
            }
            if (meth != null && !meth.getReturnType().equals(javaClass)) {
                meth = null;
            }
        } catch (SecurityException e) {
            log.warn("Couldn't introspect " + javaClass + " for valueOf", e);
        } catch (NoSuchMethodException e) {
            log.debug("No static valueOf for " + javaClass.getName() + ": " + e);
        }
        if (con == null && meth == null) {
            throw new IllegalArgumentException(
                    "Implementation class "
                            + javaClass
                            + " must contain string-taking constructor or static valueOf method");
        }
    }

    private Class<?> getJavaClass() {
        Class<?> result;
        try {
            result = Class.forName(className);
        } catch (ClassNotFoundException e) {
            if (!reportedClasses.contains(className)) {
                log.debug("Couldn't load implementation class " + className
                        + ": " + e);
                reportedClasses.add(className);
            }
            result = null;
        }
        return result;
    }

    @Override
    public String makeString(Object value) {
        String result = null;
        if (value != null) {
            /*
            Class<?> javaClass = getJavaClass();
            if (javaClass == null) {
                String msg = "Couldn't load implementation class " + className
                        + " -- can't stringify if the object can't be resolved";
                log.warn(msg);
                throw new RuntimeException(msg);
            }
            if (!javaClass.isInstance(value)) {
                throw new ClassCastException("Cannot stringify object of "
                        + value.getClass() + "; expecting class " + className
                        + " (" + value + ")");
            }
            */
            result = value.toString();
        }
        return result;
    }

    @Override
    public Object makeObject(String strValue) {
        log.debug("Making {} from {}", className, strValue);
        Object result = null;
        if (strValue != null) {
            Class<?> javaClass = getJavaClass();
            if (javaClass == null) {
                return strValue;
            }
            try {
                Constructor<?> con = javaClass.getConstructor(String.class);
                result = con.newInstance(strValue);
            } catch (NoSuchMethodException e) {
                log.debug("Implementation class " + className
                        + " has no string-taking constructor: " + e);
            } catch (InvocationTargetException e) {
                String msg = "Constructor for " + className
                        + " threw exception (arg was " + strValue + ")";
                log.warn(msg, e);
                throw new IllegalArgumentException(msg, e);
            } catch (Exception e) {
                String msg = "Constructor failed for " + className;
                log.warn(msg, e);
                throw new RuntimeException(msg, e);
            }

            if (result == null) {
                try {
                    Method meth = javaClass.getMethod("valueOf", String.class);
                    result = meth.invoke(null, strValue);
                } catch (InvocationTargetException e) {
                    String msg = "Constructor for " + className
                            + " threw exception (arg was " + strValue + ")";
                    log.warn(msg, e);
                    throw new IllegalArgumentException(msg, e);
                } catch (NoSuchMethodException e) {
                    String msg = "Implementation class " + className
                            + " has no way to build instances";
                    log.warn(msg, e);
                    throw new RuntimeException(msg, e);
                } catch (Exception e) {
                    String msg = "valueOf failed for " + className;
                    log.warn(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
        }

        return result;
    }

}
