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

package com.sri.pal.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Stack;
import java.util.Vector;

import com.sri.pal.ActionDef;
import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.CollectionTypeDef;
import com.sri.pal.StructDef;
import com.sri.pal.TypeDef;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;

/**
 * @author Valerie Wagner
 *         Date: Nov 6, 2008
 */
public class ScrapeDemoLog {


    public static final String EVENT_PREFIX = "Received action: com.sri.pal.bridge.backend.ActionImpl ";
    private BufferedWriter out;

    public static final String HEADER = "package com.sri.pal.events;\n" +
            "\n" +
            "import com.sri.pal.*;\n" +
            "import com.sri.pal.util.*;\n" +
            "import org.junit.Test;\n" +
            "\n" +
            "import java.util.ArrayList;\n" +
            "import java.util.List;\n" +
            "import java.net.URL;\n" +
            "import java.io.File;\n" +
            "\n" +
            "public class Bug_FuncTest extends PALBridgeTestCase {\n" +
            "\n" +
            "    private static final String NAMESPACE = \"TESTNS\";\n" +
            "\n" +
            "    @Test\n" +
            "    public void reproduceBug() throws Exception {\n" +
            "\n" +
            "            List<ActionInvocation> events = new ArrayList<ActionInvocation>();";

    private static final String TAB = "        ";
    private static final String TWO_TAB = TAB + TAB;
    private static final String THREE_TAB = TWO_TAB + TAB;

    private int listCount = 1;

    private static final String NAMESPACE = "CPOF";


    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: ScrapeLog <log file> <action model>");
            System.exit(1);
        }

        new ScrapeDemoLog(args[0], args[1]);
    }

    public ScrapeDemoLog(String logName,
                         String amName)
            throws Exception {

        URL url = new File(amName).toURI().toURL();
        Bridge.startPAL();
        Bridge bridge = Bridge.newInstance("scrape-log");
        ActionModel am = bridge.getActionModel();
        am.load(url, NAMESPACE);

        File file = new File(logName);
        BufferedReader in = new BufferedReader(new FileReader(file));

        File outFile = new File("Bug_FuncTest.java");
        out = new BufferedWriter(new FileWriter(outFile));

        output(HEADER);
        output(TAB + "URL url = new File(\"" + amName + "\").toURI().toURL();");
        output(TAB + "setup(url, NAMESPACE);");

        String line = in.readLine();
        int eventCount = 1;

        while (line != null) {

            // Line is:
            // ns.action1( param1 = "value1" param2 = [ "value2" "value3" ]
            // param3 = [ (positionalTupleGen "value4" "value5")
            // (positionalTupleGen "value6" "value7") ] param4 = [ ] )

            output("\n\n// OBSERVED EVENT:");
            output("// " + line);
            output("//");

            // Name is text up to first paren
            String eventClassName = line.substring(0, line.indexOf('(')).trim();
            SimpleTypeName eventTypeName = (SimpleTypeName) TypeNameFactory
                    .makeName(eventClassName);
            output(TAB + "TypeName name" + eventCount
                    + " = new TypeName(\"" + eventTypeName.getSimpleName()
                    + "\", NAMESPACE);");
            String actDefVariable = "actDef" + eventCount;
            output(TAB + "ActionDef " + actDefVariable
                    + " = (ActionDef) actionModel.getType(name" + eventCount
                    + ");");
            String eventVariable = "event" + eventCount++;

            ActionDef actDef = (ActionDef) am.getType(eventTypeName);

            output("// " + actDef);

            line = stripOffParens(line);

            String[] parts = line.split(" = ");

            Vector<String> args = new Vector<String>();

            for (int i = 1; i < parts.length; i++) {

                String lastPart = parts[i - 1];
                String thisPart = parts[i];

                int lastIndex = spaceIndex(lastPart);
                String paramName = lastPart.substring(lastIndex).trim();
                int fieldNum = actDef.getParamNum(paramName);

                output("\n// field " + fieldNum + ": " + paramName);

                int thisIndex = spaceIndex(thisPart);
                String paramValue = thisPart.substring(0, thisIndex + 1).trim();

                TypeDef paramType = actDef.getParamType(fieldNum);

                System.out.println(paramName + ":" + paramType.getName() + " = " + paramType.getClass());

                if (paramType instanceof StructDef) {
                    String listName = handleTuple(paramValue);
                    args.add(listName);
                } else if (paramType instanceof CollectionTypeDef) {
                    String listName = handleList(paramValue);
                    args.add(listName);
                } else {
                    output(TAB + "// ATOM: " + paramValue);
                    args.add(paramValue);
                }
            }

            output(TWO_TAB + "ActionInvocation " + eventVariable + " = "
                    + actDefVariable + " .bindAll(null,");
            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                if (i < args.size() - 1) {
                    output(THREE_TAB + arg + ",");
                } else {
                    output(THREE_TAB + arg);
                }
            }
            output(TWO_TAB + ");");
            output(TWO_TAB + "events.add(" + eventVariable + ");");
            line = in.readLine();
        }

        output("\nProcedureLearner.learnAndInvokeProcedure(events, \"bugProcedure\");");
        output("\n    }\n}");
        out.flush();
        out.close();
        in.close();
        // bridge.shutdown();
    }

    private String handleTuple(String value) throws IOException {

        System.out.println("tuple = " + value);
        // todo this method lamely assumes that tuples contain only atoms
        String[] parts = value.split("\\[\\[\"0\" \"1\"\\]");

        for (String part : parts) {
            System.out.println("part = " + part);
        }

        output(TWO_TAB + "// Couldn't handle tuple: " + value);

        return "noTuple";
    }

    private int spaceIndex(String part) {
        int index = part.lastIndexOf(" ");
        if (index == -1) {
            index = 0;
        }
        return index;
    }


    private String handleList(String value) throws IOException {

        Stack<String> listStack = new Stack<String>();
        String listName;

        String topListName = null;
        String item43s = "";

        output(TWO_TAB + "// LIST: " + value);


        for (int i = 0; i < value.length(); i++) {
            char letter = value.charAt(i);

            // start list
            if (letter == '[') {
                listName = "list" + listCount++;
                listStack.push(listName);
                output(TWO_TAB + "List<Object> " + listName + " = new ArrayList<Object>();");
                item43s = "";
                if (topListName == null) {
                    topListName = listName;
                }
            }
            // end list
            else if (letter == ']') {

                String finishedListName = listStack.pop();

                // output list members
                String[] atoms = item43s.trim().split(" ");
                for (String atom : atoms) {
                    output(TWO_TAB + finishedListName + ".add(" + atom + ");");
                }

                // add to parent list
                if (!listStack.isEmpty()) {
                    String parentListName = listStack.peek();
                    output(TWO_TAB + parentListName + ".add(" + finishedListName + ");");
                }

            }
            // list atoms
            else {
                item43s += letter;
            }
        }


//        if (isListValue(value)) {
//            output(TWO_TAB + "// LIST: " + value);
//            String subList = handleList(value);
//            output(TWO_TAB + listName + ".add(" + subList + ");");
//        } else {
//            String[] values = value.split("[^\"] [^\"]");
//            for (String oneValue : values) {
//                System.out.println("oneValue = " + oneValue);
//                if (isListValue(oneValue)) {
//                    output("\n// " + oneValue);
//                    String subList = handleList(oneValue);
//                    output(TWO_TAB + listName + ".add(" + subList + ");");
//                } else {
//
//                    String [] atoms = oneValue.split(" ");
//
//                    for (String atom : atoms) {
//                        output(TWO_TAB + listName + ".add(" + atom + ");");
//                    }
//
//                }
//            }
//        }
        return topListName;
    }

    private String stripOffParens(String line) {
        return stripOff(line, '(', ')');
    }

    private String stripOff(String line, char firstChar, char lastChar) {
        return line.substring(line.indexOf(firstChar) + 1, line.lastIndexOf(lastChar));
    }

    private void output(String line) throws IOException {
        out.write(line + "\n");
    }
}
