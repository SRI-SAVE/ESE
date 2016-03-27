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

// $Id: TypeConverter.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.lumenpal;

import java.util.ArrayList;
import java.util.List;

import com.sri.ai.lumen.runtime.LumenStackTraceElement;
import com.sri.pal.common.ErrorInfo.PALStackFrame;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;

/**
 * Converts types between the Spine's format and Lumen's format.
 *
 * @author chris
 */
final class TypeConverter {
    /* There should never be an instance of this class. */
    private TypeConverter() {
    }

    static List<PALStackFrame> fromLumenStack(List<LumenStackTraceElement> stack) {
        List<PALStackFrame> result = new ArrayList<PALStackFrame>();
        for (LumenStackTraceElement lumenFrame : stack) {
            SimpleTypeName name = (SimpleTypeName) TypeNameFactory
                    .makeName(lumenFrame.getActionName());
            PALStackFrame palFrame = new PALStackFrame(name,
                    lumenFrame.getPreorderIndex());
            result.add(palFrame);
        }
        return result;
    }
}
