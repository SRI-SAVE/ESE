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

package com.sri.tasklearning.ui.core.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

// filter a file open/save dialog by the specified extension(s)
public class GenericFilter extends FileFilter {

    private String[] extensions = { ".txt", ".text" };
    private String description = "Text Files";

    public GenericFilter(String[] exts, String desc) {
        extensions = exts;
        description = desc;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        } else {
            for (String ext : extensions) {
                if (f.getName().toLowerCase().endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String getDescription() {
        return description;
    }
}