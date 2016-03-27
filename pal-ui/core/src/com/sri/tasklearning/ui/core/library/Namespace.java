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

package com.sri.tasklearning.ui.core.library;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.sri.tasklearning.ui.core.BackendInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.PALException;
import com.sri.tasklearning.ui.core.BackendFacade;
import com.sri.tasklearning.ui.core.resources.ResourceLoader;

/**
 * Represents a logical namespace for actions. Actions include true actions,
 * procedure steps, etc. In general, each client application has its own 
 * namespace and all procedures live in a special built-in namespace (the
 * lumen namespace). 
 */
public class Namespace implements Comparable<Namespace> {

    // built-in procedure namespace
    public static final Namespace BUILTIN = 
        new Namespace(BackendInterface.PROC_NAMESPACE,
                      BackendInterface.PROC_VERSION,
                      ResourceLoader.getURL("gear-x2.png"));    

    private static final Logger log = LoggerFactory
                .getLogger(Namespace.class);
    
    private final String name; // The name of this namespace
    private final String version;
    private URL icon = null;

    public Namespace(
            final String name,
            final String version) {
        
        this.name = name;
        this.version = version;
    }
    
    public Namespace(
            final String name,
            final String version,
            final URL icon) {
        
        this.name = name;
        this.version = version;
        this.icon = icon; 
    }

    public String getName() {
        return name;
    }    
    
    public String getVersion() {
        return version; 
    }
    
    public String getFullName() {
        return fullNamespaceName(name, version);
    }
    
    public static String fullNamespaceName(String name, String version) {
        return name + "." + version;
    }

    public URL getIcon() {
        if (icon == null) {
            String resourcesPath = getResourcePath();
            
            if (resourcesPath != null) {
                Map<String, String> meta = getMetadata();
                
                if (meta.containsKey("icon")) {
                    try {
                        icon = new URL(resourcesPath + meta.get("icon"));
                        return icon;
                    } catch (MalformedURLException e){
                        log.error("Unable to create URL for icon: " + resourcesPath + meta.get("icon"));
                    }
                }
            }
        }
        
        if (icon == null)
            icon = ResourceLoader.getURL("gear.png");

        return icon;
    }
    
    public String getResourcePath() {
        Map<String, String> meta = getMetadata();
        
        if (meta != null)
            return meta.get("resource_path");
        
        return null; 
    }
    
    private Map<String, String> getMetadata() {
        try {
            Map<String, String> meta = 
                    BackendFacade.getInstance().getNamespaceMetadata(name, version);
            return meta;
        } catch (PALException e) {
            log.error("Failed to load namespace metadata for " + getFullName());
        }
        
        return null; 
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Namespace && 
                ((Namespace)other).getFullName().equals(getFullName());        
    }

    @Override
    public int hashCode() {
        return getFullName().hashCode();
    }

    @Override
    public int compareTo(Namespace other) {
        return getFullName().compareTo(other.getFullName());
    }
}
