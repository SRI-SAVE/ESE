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

// $Id: ErrorFactory.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.util;

import java.util.ArrayList;
import java.util.List;

import com.sri.pal.common.ErrorInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for easily generating error objects. Use one of the {@code error}
 * methods to generate error objects.
 *
 * @author chris
 */
public class ErrorFactory {
    private static final Logger log = LoggerFactory
            .getLogger(ErrorFactory.class);

    private final String defaultSource;

    /**
     * Static method for generating error objects. If the error indicated by
     * {@code id} contains parameterized strings, then {@code params} must
     * contain the appropriate number of arguments to be passed to
     * {@link String.format}. The same {@code params} will be used to format
     * both strings, so the format strings may wish to use {@code %1$s} or
     * similar specifiers which use {@code argument_index}.
     *
     * @param source
     *            the module which originated this error, such as "Lumen" or
     *            "LAPDOG"
     * @param id
     *            the error which was encountered
     * @param stack
     *            the stack of PAL procedures and/or actions, representing where
     *            this error occurred
     * @param params
     *            optional arguments for parameterized error messages
     * @return a new error object with parameter substitution performed on its
     *         message strings
     */
    public static ErrorInfo error(String source,
                                  ErrorType id,
                                  List<ErrorInfo.PALStackFrame> stack,
                                  Object... params) {
        String terseMsg = format(id.getTerseMsg(), params);
        String detailMsg = format(id.getDetailMsg(), params);
        return new ErrorInfo(source, id.ordinal(), terseMsg, detailMsg, stack);
    }

    /**
     * Static method for generating error objects with no stack. This is
     * appropriate if nothing is being executed, for instance if an error occurs
     * during learning. This method is equivalent to calling
     * {@code #error(String, ErrorType, List, Object...)} with a 0-length stack.
     *
     * @param source
     *            the module which originated this error, such as "Lumen" or
     *            "LAPDOG"
     * @param id
     *            the error which was encountered
     * @param params
     *            optional arguments for parameterized error messages
     * @return a new error object with parameter substitution performed on its
     *         message strings
     */
    public static ErrorInfo error(String source,
                                  ErrorType id,
                                  Object... params) {
        return error(source, id, new ArrayList<ErrorInfo.PALStackFrame>(),
                params);
    }

    /**
     * Our custom format method calls String.format() but always returns
     * something rather than throwing an exception. Because this method is on
     * rarely-used error code paths, we don't want to depend on format strings
     * always matching their associated arguments.
     */
    private static String format(String format,
                                 Object... args) {
        String result;
        try {
            result = String.format(format, args);
        } catch (Exception e) {
            log.warn("Unable to format \"" + format + "\" with args: " + args,
                    e);
            result = format;
            for (Object o : args) {
                result += ", ";
                if (o == null) {
                    result += "(null)";
                } else {
                    result += o.toString();
                }
            }
        }

        return result;
    }

    /**
     * Creates a new factory with the specified source string. This string
     * should indicate which module the errors come from, such as "Lumen" or
     * "LAPDOG" -- it should not indicate which class generated the error.
     *
     * @param source
     *            the module which originates errors using this factory
     */
    public ErrorFactory(String source) {
        defaultSource = source;
    }

    /**
     * Generate a new error object using the configured source string. This is
     * equivalent to calling {@link #error(ErrorType, List, Object...)} with a
     * 0-length {@code stack}.
     *
     * @param id
     *            the error which was encountered
     * @param params
     *            optional arguments for parameterized error messages
     * @return a new error object with parameter substitution performed on its
     *         message strings
     */
    public ErrorInfo error(ErrorType id,
                           Object... params) {
        return error(id, new ArrayList<ErrorInfo.PALStackFrame>(), params);
    }

    /**
     * Generate a new error object using the configured source string.
     *
     * @param id
     *            the error which was encountered
     * @param stack
     *            the stack of PAL procedures and/or actions, representing where
     *            this error occurred
     * @param params
     *            optional arguments for parameterized error messages
     * @return a new error object with parameter substitution performed on its
     *         message strings
     */
    public ErrorInfo error(ErrorType id,
                           List<ErrorInfo.PALStackFrame> stack,
                           Object... params) {
        return error(defaultSource, id, stack, params);
    }
}
