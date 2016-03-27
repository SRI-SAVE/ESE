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

// $Id: BadModels.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.actionmodels.bad;

public enum BadModels {
    DUP_PREDEF,
    DUP_NAME,
    CUSTOM_INHERIT_NONE,
    CUSTOM_INHERIT_PREDEF,
    ENUM_DUP_VALUE,
    LIST_UNKNOWN_MEMBER,
    ACTION_DUP_PARAM,
    CONSTRAINT_DUP_PARAM,
    FAMILY_DUP_PARAM,
    IDIOM_DUP_PARAM,
    STRUCT_DUP_FIELD,
    ACTION_UNKNOWN_FAMILY,
    ACTION_UNKNOWN_COLLAPSE,
    ACTION_UNKNOWN_CONSTRAINT,
    ACTION_UNKNOWN_PARAM_TYPE,
    ACTION_UNKNOWN_PARAM_FAMILY,
    ACTION_UNKNOWN_PARAM_ROLE,
    TEMPLATE_UNKNOWN_FAMILY,
    TEMPLATE_UNKNOWN_ACTION,
    TEMPLATE_UNKNOWN_IDIOM_PARAM,
    TEMPLATE_UNKNOWN_FAMILY_ROLE,
    TEMPLATE_UNKNOWN_ACTION_IDIOM_PARAM,
    TEMPLATE_UNKNOWN_ACTION_PARAM,
}
