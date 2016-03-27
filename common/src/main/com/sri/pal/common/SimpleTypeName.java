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

// $Id: SimpleTypeName.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

/**
 * Simple reference to a type, including namespace, version, and simple name.
 * Examples are {@code myApp^1.3^MyType}, {@code MyType}, and {@code string}.
 */
public class SimpleTypeName
        implements TypeName {
    private static final long serialVersionUID = 1L;

    public static final String PACKAGE_NAME_SEPARATOR = "^";
    public static final String IDIOM_TEMPLATE_SEPARATOR = "/";

    private final String name;
    private final String version;
    private final String namespace;

    /**
     * Creates a new type identifier with the given name and namespace.
     *
     * @param name
     *            the &quot;local&quot; name of the type
     * @param version
     *            the version of the application's action model which this type
     *            belongs to
     * @param namespace
     *            the namespace in which the type resides.
     */
    public SimpleTypeName(String name,
                          String version,
                          String namespace) {
        this.name = name;
        this.version = version;
        this.namespace = namespace;
        if (name == null) {
            throw new NullPointerException("Name cannot be null");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (name.contains(PACKAGE_NAME_SEPARATOR)) {
            throw new IllegalArgumentException("Name cannot contain '"
                    + PACKAGE_NAME_SEPARATOR + "': " + name);
        }

        if (version != null && version.contains(PACKAGE_NAME_SEPARATOR)) {
            throw new IllegalArgumentException("Version cannot contain '"
                    + PACKAGE_NAME_SEPARATOR + "': " + version);
        }
        if (namespace != null && namespace.contains(PACKAGE_NAME_SEPARATOR)) {
            throw new IllegalArgumentException("Version cannot contain '"
                    + PACKAGE_NAME_SEPARATOR + "': " + namespace);
        }

        if (namespace == null && version != null) {
            throw new IllegalArgumentException("Version " + version
                    + " is meaningless with null namespace");
        }
    }

    /**
     * Provides just the local part of the type's name. This corresponds to the
     * identifier given in the action model XML file.
     *
     * @return the type's name
     */
    public String getSimpleName() {
        return name;
    }

    /**
     * Provides just the version part of the type's name. This corresponds to
     * the version of the action model supplied in the {@code version} attribute
     * of the {@code actionModel} element in the action model XML file.
     *
     * @return the version of the action model fragment this type belongs to
     */
    public String getVersion() {
        return version;
    }

    /**
     * Provides just the namespace in which the type resides. This corresponds
     * to the <code>namespace</code> argument given to one of the
     * {@code ActionModel#load} methods.
     *
     * @return the namespace of the action
     */
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getFullName() {
        StringBuffer sb = new StringBuffer();
        if (namespace != null) {
            sb.append(namespace);
            sb.append(PACKAGE_NAME_SEPARATOR);
        }
        if (version != null) {
            sb.append(version);
            sb.append(PACKAGE_NAME_SEPARATOR);
        }
        sb.append(name);
        return sb.toString();
    }

    /**
     * Returns just the base name of an idiom. If this is a full idiom name
     * which refers to the active template as in
     * {@code namespace^version^name/template}, this method will return just
     * {@code namespace^version^name}. If there is no template name, this object
     * will be returned unchanged.
     *
     * @return this type name with the idiom template removed, if any
     * @see #getIdiomTemplateName
     * @see #addIdiomTemplateName
     */
    public SimpleTypeName getIdiomBaseName() {
        int loc = name.lastIndexOf(IDIOM_TEMPLATE_SEPARATOR);
        if (loc == -1) {
            return this;
        }
        String newName = name.substring(0, loc);
        return new SimpleTypeName(newName, version, namespace);
    }

    /**
     * Returns just the template name of an idiom. For a name describing an
     * idiom and template of the form {@code namespace^version^name/template},
     * this method will return just {@code template}. If there is no template
     * name, {@code null}.
     *
     * @return the template name, if any
     * @see #getIdiomBaseName
     * @see #addIdiomTemplateName
     */
    public String getIdiomTemplateName() {
        int loc = name.lastIndexOf(IDIOM_TEMPLATE_SEPARATOR);
        if (loc == -1) {
            return null;
        }
        return name.substring(loc + 1);
    }

    /**
     * Create a template-qualified idiom name from this (which represents an
     * idiom name) and a given template name.
     *
     * @param templateName
     *            the name of the template
     * @return a name which includes the template
     * @see #getIdiomBaseName
     * @see #getIdiomTemplateName
     */
    public SimpleTypeName addIdiomTemplateName(String templateName) {
        return new SimpleTypeName(name + IDIOM_TEMPLATE_SEPARATOR
                + templateName, version, namespace);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getFullName() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((namespace == null) ? 0 : namespace.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleTypeName other = (SimpleTypeName) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }
}
