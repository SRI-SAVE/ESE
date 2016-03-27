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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.ActionExecutor;
import com.sri.pal.ActionInvocation;
import com.sri.pal.ActionInvocation.StepCommand;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.ActionStreamEvent.Status;
import com.sri.pal.Bridge;
import com.sri.pal.PALException;
import com.sri.pal.Struct;
import com.sri.pal.StructDef;
import com.sri.pal.common.ErrorInfo;
import com.sri.pal.common.TypeNameFactory;

public class TestExecutor implements ActionExecutor {
    private static final Logger log = LoggerFactory
            .getLogger(TestExecutor.class);

    public static final String NAMESPACE = "arda";
    private String output = null;
    private final Bridge bridge;
    
    public TestExecutor(Bridge bridge) {
        super();
        this.bridge = bridge; 
    }

    // Action names
    enum ArdaAction {
        CREATE_STRING("CreateString"),
        CREATE_ARDA_STRING("CreateArdaString"),
        CREATE_ARDA_EQUIVALENT_STRING("CreateEquivalentString"),
        CREATE_ARDA_CHILD_STRING("CreateArdaChildString"),
        CREATE_ARDA_GRANDCHILD_STRING("CreateArdaGrandChildString"),
        CREATE_STRINGS("CreateStrings"),
        CREATE_REAL("CreateReal"),
        CREATE_INTEGER("CreateInteger"),
        CREATE_ENUM("CreateEnum"),
        CREATE_BOOLEAN("CreateBoolean"),
        CREATE_SET("CreateSet"),
        CREATE_LIST("CreateList"),
        CREATE_BAG("CreateBag"),
        CREATE_STRUCT("CreateStruct"),
        CREATE_STRUCT_LIST("CreateListOfStruct"),
        CREATE_STRING_AND_OTHER_STUFF("CreateStringAndOtherStuff"),
        USE_STRING("UseString"),
        USE_ARDA_STRING("UseArdaString"),
        USE_ARDA_CHILD_STRING("UseArdaChildString"),
        USE_ARDA_GRANDCHILD_STRING("UseArdaGrandChildString"),
        USE_STRINGS("UseStrings"),
        USE_REAL("UseReal"),
        USE_INTEGER("UseInteger"),
        USE_ENUM("UseEnum"),
        USE_BOOLEAN("UseBoolean"),
        USE_SET("UseSet"),
        USE_LIST("UseList"),        
        USE_BAG("UseBag"),
        USE_STRUCT("UseStruct"),
        USE_PARENT_STRUCT("UseParentStruct"),
        USE_LIST_OF_INTEGER("UseListOfInteger"),
        USE_LIST_OF_LIST("UseListOfList"),
        USE_STRING_CREATE_STRING("UseStringCreateString"),
        USE_STRING_CREATE_SET("UseStringCreateSet"),
        USE_STRING_CREATE_LIST("UseStringCreateList"),
        USE_STRING_CREATE_BAG("UseStringCreateBag"),
        USE_STRING_CREATE_STRUCT("UseStringCreateStruct"),
        USE_CUSTOM_TYPES("UseCustomTypes"),
        USE_PRIMITIVE_TYPES("UsePrimitiveTypes"),
        USE_FOO("UseFoo"); 
        
        private String functor;
        
        ArdaAction(String functor) {
            this.functor = functor;    
        }
        
        public String getFunctor() {
            return functor; 
        }
        
        public static ArdaAction findByName(String name) {
            for (ArdaAction action : values())
                if (action.getFunctor().equals(name))
                    return action; 
            
            throw new RuntimeException("Unsupported action: " + name);
        }
    }

    @Override
    public void cancel(ActionStreamEvent arg0) {
    }

    @Override
    public void execute(ActionInvocation invoc) throws PALException {
        try {
            innerExecute(invoc);
        } catch (Exception e) {
            log.warn("Execution of " + invoc + " failed", e);
            ErrorInfo error = new ErrorInfo(NAMESPACE, 1, "execution failed",
                    e.toString(), null);
            invoc.error(error);
        }
    }

    private static StructDef StructDef = null;
    protected void innerExecute(ActionInvocation invocation) throws PALException {
        if (invocation.getCaller() == null) {
            log.info("No parent invocation for {}. ", invocation.getDefinition()
                    .getName());
        }        
        
        if (StructDef == null)
            StructDef = (StructDef) bridge.getActionModel().getType(TypeNameFactory.makeName("struct", "1.0", "arda"));

        int size = invocation.getDefinition().size();
        ArrayList<Object> items = new ArrayList<Object>();
        for (int i = 0; i < size; i++) {
            items.add(invocation.getValue(i));
        }

        String actionName = invocation.getDefinition().getName().getSimpleName();
        
        ArdaAction act = ArdaAction.findByName(actionName);
        
        invocation.setStatus(Status.RUNNING);
        
        String append = "";
        
        switch (act) {
        case CREATE_STRING:
        case CREATE_ARDA_STRING:
        case CREATE_ARDA_EQUIVALENT_STRING:
        case CREATE_ARDA_CHILD_STRING:
        case CREATE_ARDA_GRANDCHILD_STRING:             
            append = "CREATE STRING";
            invocation.setValue(0, new String("string"));
            break; 
        case CREATE_STRINGS:
            append = "CREATE STRINGS";
            invocation.setValue(0, new String("string 1"));
            invocation.setValue(1, new String("string 2"));
            break;
        case CREATE_REAL:
            append = "CREATE REAL";
            invocation.setValue(0, 3.14);
            break; 
        case CREATE_INTEGER:
            append = "CREATE INTEGER";
            invocation.setValue(0, -1);
            break;
        case CREATE_ENUM:
            append = "CREATE ENUM";
            invocation.setValue(0, "Option 2");
            break;  
        case CREATE_BOOLEAN:
            append = "CREATE BOOLEAN";
            invocation.setValue(0, new Boolean(true));
            break;
        case CREATE_SET:
            append = "CREATE SET";
            Set<Object> set = new TreeSet<Object>();
            set.add(new String("set value 1"));
            set.add(new String("set value 2"));
            invocation.setValue(0, set);
            break;
        case CREATE_LIST:
            append = "CREATE LIST";            
            List<Object> list = new ArrayList<Object>();
            list.add(new String("list value 1"));
            list.add(new String("list value 2"));
            invocation.setValue(0, list);
            break;    
        case CREATE_BAG:
            append = "CREATE BAG";
            List<Object> bag = new ArrayList<Object>();
            bag.add(new String("bag value 1"));
            bag.add(new String("bag value 1"));
            bag.add(new String("bag value 2"));
            invocation.setValue(0, bag);
            break; 
        case CREATE_STRUCT:
            append = "CREATE STRUCT";
            Struct Struct = new Struct(StructDef);
            Struct.setValue(0, new Integer(1));
            Struct.setValue(1, new String("text"));
            invocation.setValue(0, Struct);
            break; 
        case CREATE_STRUCT_LIST:
            append = "CREATE LIST OF STRUCT";
            Struct Struct1 = new Struct(StructDef);
            Struct1.setValue(0, new Integer(1));
            Struct1.setValue(1, new String("text 1"));
            
            Struct Struct2 = new Struct(StructDef);
            Struct2.setValue(0, new Integer(2));
            Struct2.setValue(1, new String("text 2"));
            
            List<Struct> structList = new ArrayList<Struct>();
            structList.add(Struct1);
            structList.add(Struct2);
            invocation.setValue(0, structList);
            break; 
        case CREATE_STRING_AND_OTHER_STUFF:
            append = "CREATE STRING AND OTHER STUFF";
            invocation.setValue(0, new String("string"));
            invocation.setValue(1, new Boolean(false));
            List<Object> ulist = new ArrayList<Object>();
            ulist.add(new String("list value 1"));
            ulist.add(new String("list value 2"));
            invocation.setValue(1, ulist);
            break;
        case USE_STRING:
        case USE_ARDA_STRING:
        case USE_ARDA_CHILD_STRING:
        case USE_ARDA_GRANDCHILD_STRING:
            append = "USE STRING: " + invocation.getValue(0);
            break; 
        case USE_STRINGS:
            append = "USE STRINGS: " + invocation.getValue(0) + ","
                   + invocation.getValue(1);
            break;
        case USE_REAL:
            append = "USE REAL " + (Double)invocation.getValue(0);
            break; 
        case USE_INTEGER:
            append = "USE INTEGER " + (Integer)invocation.getValue(0);
            break;
        case USE_ENUM:
            append = "USE ENUM " + invocation.getValue(0);
            break;
        case USE_BOOLEAN:
            append = "USE BOOLEAN " + (Boolean)invocation.getValue(0);
            break;
        case USE_SET:
            append = "USE SET: [";
            
            @SuppressWarnings("unchecked")
            Set<Object> useSet = (Set<Object>)invocation.getValue(0);
            boolean empty = true;
            for (Object obj : useSet) {
                empty = false;
                append += obj + ",";
            }
            if (!empty)
                append = append.substring(0, append.length() - 1);
            append += "]";
            break; 
        case USE_LIST:
        case USE_LIST_OF_INTEGER:
        case USE_BAG:
            if (act == ArdaAction.USE_BAG)
                append = "USE BAG: [";
            else
                append = "USE LIST: [";                        
            
            @SuppressWarnings("unchecked")
            List<Object> useList = (List<Object>)invocation.getValue(0);
            empty = true;
            for (Object obj : useList) {
                empty = false;
                append += obj + ",";
            }
            if (!empty)
                append = append.substring(0, append.length() - 1);
                append += "]";
            break;
        case USE_LIST_OF_LIST:
            append = "USE LIST OF LIST: [";
            @SuppressWarnings("unchecked")
            List<List<String>> l = (List<List<String>>)invocation.getValue(0);
            for (List<String> ll : l) {
                append += "[";
                for (String s : ll)
                    append += s + ",";
                append = append.substring(0, append.length() - 1) + "]";
            }
            append += "]";
            break;
        case USE_STRUCT:
            append = "USE STRUCT: [";
            Struct s = (Struct)invocation.getValue(0);
            append += s.getValue(0) + ",";
            append += s.getValue(1) + "]";
            break;
        case USE_PARENT_STRUCT:
            append = "USE PARENT STRUCT: [";
            s = (Struct)invocation.getValue(0);
            append += s.getValue(0) + ",";
            append += s.getValue(1) + "]";
            break;             
        case USE_STRING_CREATE_STRING:
        case USE_STRING_CREATE_SET:
        case USE_STRING_CREATE_LIST:
        case USE_STRING_CREATE_BAG:
        case USE_STRING_CREATE_STRUCT:
        case USE_CUSTOM_TYPES:
        case USE_PRIMITIVE_TYPES:
        case USE_FOO:
            break; 
        default:
            throw new RuntimeException("Unsupported action");
        }
        
        appendOutput(append);
        
        invocation.setStatus(Status.ENDED);      
    }
    
    private void appendOutput(String app) {
        output += app + "\n";
    }

    @Override
    public void continueStepping(ActionInvocation invocation,
                                 StepCommand command,
                                 List<Object> actionArgs)
            throws PALException {

    }

    @Override
    public void executeStepped(ActionInvocation arg0) throws PALException {

    }
        
    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
