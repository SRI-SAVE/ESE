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

package resources;

import java.io.InputStream;
import java.net.URL;


/**
 * @author Will Haines
 *
 * A class that lives in the resource package to give me a hook to
 * go and load the resources.
 */
public class ResourceLoader {

    /**
     * Get an input stream for a given resource, relative to {@link ResourceLoader}.
     *
     * @param name the relative path to look up
     * @return an input stream from the given resource
     */
    static public InputStream getResourceAsStream(String name) {
        return ResourceLoader.class.getResourceAsStream(name);
    }

    /**
     * Get a URL location for a given resource, relative to {@link ResourceLoader}
     *
     * @param name the relative path to look up
     * @return an absolute URL to the resource
     */
    static public URL getResource(String name) {
        return ResourceLoader.class.getResource(name);
    }
}
