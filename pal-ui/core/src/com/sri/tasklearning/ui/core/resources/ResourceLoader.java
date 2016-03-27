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

package com.sri.tasklearning.ui.core.resources;

import java.io.InputStream;
import java.net.URL;

/**
 * Loads resources from a well-known location.
 */
public class ResourceLoader {
    private static final String RESOURCE = "./";

    /**
     * Get a URL based on a file's relative path.
     * 
     * @param relativePath
     *            the relative path to this URL in the resources directory tree
     * @return an input stream to this resource or null if none
     */
    public static InputStream getStream(final String relativePath) {
        if (relativePath == null) {
            return null;
        }
        // Handle the ./ case
        if (relativePath.startsWith(RESOURCE)) {
            return ResourceLoader.class.getResourceAsStream(relativePath
                    .substring(2));
        }
        return ResourceLoader.class.getResourceAsStream(relativePath);
    }
    
    public static URL getURL(final String relativePath) {
        if (relativePath == null) {
            return null;
        }
        // Handle the ./ case
        if (relativePath.startsWith(RESOURCE)) {
            return ResourceLoader.class.getResource(relativePath
                    .substring(2));
        }
        return ResourceLoader.class.getResource(relativePath);
    }
}
