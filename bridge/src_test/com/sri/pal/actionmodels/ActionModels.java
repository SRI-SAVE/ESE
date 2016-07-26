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

// $Id: ActionModels.java 7750 2016-07-26 16:53:01Z Chris Jones (E24486) $
package com.sri.pal.actionmodels;

/**
 * This class lives in the same package as the action model files which are used
 * for various tests. This class is used to construct resource URLs to load the
 * action model files.
 *
 * @author chris
 */
public class ActionModels {
    public static final String ACTIONS = "actions_test.xml";
    public static final String ACTIONS_DUPES = "actions_duplicates_test.xml";
    public static final String ALLTYPES = "alltypes.xml";
    public static final String ARDA = "arda.xml";
    public static final String BAGS = "bags.xml";
    public static final String COLLAPSE = "collapse.xml";
    public static final String COMPLETERS = "completers.xml";
    public static final String CONSTRAINTS = "constraints.xml";
    public static final String CPOF = "cpof_action_model.xml";
    public static final String CUSTOM = "custom_test.xml";
    public static final String DEBUG = "debug.xml";
    public static final String DEFAULTS = "defaults.xml";
    public static final String EQUIVALENCE = "equivalence.xml";
    public static final String IDIOMS = "idioms.xml";
    public static final String INHERIT = "inherit.xml";
    public static final String INHERITANCE = "inheritance.xml";
    public static final String INH_ENUMS = "inh_enums.xml";
    public static final String LEARNING_PREFS = "learningPrefs.xml";
    public static final String LISTS = "lists.xml";
    public static final String METADATA = "metadata.xml";
    public static final String PERMUTABLE_LISTS = "permutableLists.xml";
    public static final String PRIMITIVES = "primitives.xml";
    public static final String SETS = "sets.xml";
    public static final String SIMPLE = "simple.xml";
    public static final String STRESS = "stress.xml";
    public static final String TUPLE = "tuple_test.xml";
    public static final String TUPLES = "tuples.xml";
    public static final String TYPES = "types_test.xml";
    public static final String WHITESPACE = "whitespace.xml";

    public static final String[] ALL = new String[] { SIMPLE, PRIMITIVES,
            ALLTYPES, CUSTOM, METADATA, TUPLE, TUPLES, INHERIT, INHERITANCE,
            LISTS, TYPES, ACTIONS, BAGS, SETS, DEBUG, WHITESPACE,
            PERMUTABLE_LISTS, STRESS, CPOF, CONSTRAINTS, EQUIVALENCE,
            ACTIONS_DUPES, IDIOMS, LEARNING_PREFS, COLLAPSE, DEFAULTS,
            COMPLETERS, ARDA, INH_ENUMS
// "domain/action_model.xml"
    };

    public static String[] getAll() {
        return ALL;
    }
}
