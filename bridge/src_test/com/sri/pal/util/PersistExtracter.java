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

// $Id: PersistExtracter.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.util;

import com.sri.pal.PALException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads SPARK's <code>.persist</code> files and pulls out the SPARK
 * source. This can be used to build XML files suitable for use with the
 * Bridge's persistence mechanism. Note this parser is pretty lightweight, and
 * it's not always going to succeed.
 *
 * @author chris
 */
public class PersistExtracter {
    private static final Logger log = LoggerFactory
            .getLogger("TestSourceLogger");

    public static void main(String[] args) {
        for (String filename : args) {
            File srcFile = new File(filename);
            File destFile = new File(filename.replace(".persist", "")
                    + ".spark");
            try {
                String srcStr = readFile(srcFile);
                String destStr = convert(srcStr);
                writeFile(destFile, destStr);
            } catch (Exception e) {
                log.warn("Unable to convert file " + filename + ": "
                        + e.getMessage());
            }
        }
    }

    private static String convert(String input)
            throws PALException {
        // Our regex, split across multiple lines for readability:
        // Any number of "import:" or "importall:" statements
        String regex = "(import[a-z]*: [a-zA-Z_. ]*[\\s]*)*" +
        // "{defprocedure" or "{defaction"
                "\\{def[a-z]*" +
                // The procedure or action body
                ".*" +
                // The closing "}". Note the above .* is greedy, so it'll match
                // as much as possible
                "\\}\r?\n" +
                // An optional trailing "export:" or "exportall:"
                "(export[a-z]*:[^\n]*\n)?";

        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String definition = matcher.group();
            return definition;
        } else {
            throw new PALException("Unable to find SPARK code in input");
        }
    }

    private static String readFile(File file)
            throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = in.readLine();
            }
            return sb.toString();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static void writeFile(File file,
                                  String item43s)
            throws IOException {
        FileWriter out = null;
        try {
            out = new FileWriter(file);
            out.write(item43s);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
