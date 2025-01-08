/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.api.xml;

import javax.xml.stream.XMLStreamException;
import java.io.StringWriter;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 *  NOTE: remove all the util code in here when separated, this class should be pure data.
 */
public class XmlNode {

    public static final String CHILDREN_COMBINATION_MODE_ATTRIBUTE = "combine.children";

    public static final String CHILDREN_COMBINATION_MERGE = "merge";

    public static final String CHILDREN_COMBINATION_APPEND = "append";

    /**
     * This default mode for combining children DOMs during merge means that where element names match, the process will
     * try to merge the element data, rather than putting the dominant and recessive elements (which share the same
     * element name) as siblings in the resulting DOM.
     */
    public static final String DEFAULT_CHILDREN_COMBINATION_MODE = CHILDREN_COMBINATION_MERGE;

    public static final String SELF_COMBINATION_MODE_ATTRIBUTE = "combine.self";

    public static final String SELF_COMBINATION_OVERRIDE = "override";

    public static final String SELF_COMBINATION_MERGE = "merge";

    public static final String SELF_COMBINATION_REMOVE = "remove";

    /**
     * In case of complex XML structures, combining can be done based on id.
     */
    public static final String ID_COMBINATION_MODE_ATTRIBUTE = "combine.id";

    /**
     * In case of complex XML structures, combining can be done based on keys.
     * This is a comma separated list of attribute names.
     */
    public static final String KEYS_COMBINATION_MODE_ATTRIBUTE = "combine.keys";

    /**
     * This default mode for combining a DOM node during merge means that where element names match, the process will
     * try to merge the element attributes and values, rather than overriding the recessive element completely with the
     * dominant one. This means that wherever the dominant element doesn't provide the value or a particular attribute,
     * that value or attribute will be set from the recessive DOM node.
     */
    public static final String DEFAULT_SELF_COMBINATION_MODE = SELF_COMBINATION_MERGE;

    protected final String prefix;

    protected final String namespaceUri;

    protected final String name;

    protected final String value;

    protected final Map<String, String> attributes;

    protected final List<XmlNode> children;

    protected final Object location;

    public XmlNode(String name) {
        this(name, null, null, null, null);
    }

    public XmlNode(String name, String value) {
        this(name, value, null, null, null);
    }

    public XmlNode(XmlNode from, String name) {
        this(name, from.getValue(), from.getAttributes(), from.getChildren(), from.getInputLocation());
    }

    public XmlNode(String name, String value, Map<String, String> attributes, List<XmlNode> children, Object location) {
        this("", "", name, value, attributes, children, location);
    }

    public XmlNode(
            String prefix,
            String namespaceUri,
            String name,
            String value,
            Map<String, String> attributes,
            List<XmlNode> children,
            Object location) {
        this.prefix = prefix == null ? "" : prefix;
        this.namespaceUri = namespaceUri == null ? "" : namespaceUri;
        this.name = Objects.requireNonNull(name);
        this.value = value;
        this.attributes = ImmutableCollections.copy(attributes);
        this.children = ImmutableCollections.copy(children);
        this.location = location;
    }
    // ----------------------------------------------------------------------
    // Name handling
    // ----------------------------------------------------------------------

    public String getPrefix() {
        return prefix;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getName() {
        return name;
    }

    // ----------------------------------------------------------------------
    // Value handling
    // ----------------------------------------------------------------------

    public String getValue() {
        return value;
    }

    // ----------------------------------------------------------------------
    // Attribute handling
    // ----------------------------------------------------------------------

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    // ----------------------------------------------------------------------
    // Child handling
    // ----------------------------------------------------------------------

    public XmlNode getChild(String name) {
        if (name != null) {
            ListIterator<XmlNode> it = children.listIterator(children.size());
            while (it.hasPrevious()) {
                XmlNode child = it.previous();
                if (name.equals(child.getName())) {
                    return child;
                }
            }
        }
        return null;
    }

    public List<XmlNode> getChildren() {
        return children;
    }

    public int getChildCount() {
        return children.size();
    }

    // ----------------------------------------------------------------------
    // Input location handling
    // ----------------------------------------------------------------------

    /**
     * @since 3.2.0
     * @return input location
     */
    public Object getInputLocation() {
        return location;
    }

    // ----------------------------------------------------------------------
    // Standard object handling
    // ----------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XmlNode that = (XmlNode) o;
        return Objects.equals(this.name, that.name)
                && Objects.equals(this.value, that.value)
                && Objects.equals(this.attributes, that.attributes)
                && Objects.equals(this.children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, attributes, children);
    }

    @Override
    public String toString() {
        try {
            return toStringXml();
        } catch (XMLStreamException e) {
            return toStringObject();
        }
    }

    public String toStringXml() throws XMLStreamException {
        StringWriter writer = new StringWriter();
        XmlNodeWriter.write(writer, this);
        return writer.toString();
    }

    public String toStringObject() {
        StringBuilder sb = new StringBuilder();
        sb.append("XmlNode[");
        boolean w = false;
        w = addToStringField(sb, prefix, o -> !o.isEmpty(), "prefix", w);
        w = addToStringField(sb, namespaceUri, o -> !o.isEmpty(), "namespaceUri", w);
        w = addToStringField(sb, name, o -> !o.isEmpty(), "name", w);
        w = addToStringField(sb, value, o -> !o.isEmpty(), "value", w);
        w = addToStringField(sb, attributes, o -> !o.isEmpty(), "attributes", w);
        w = addToStringField(sb, children, o -> !o.isEmpty(), "children", w);
        w = addToStringField(sb, location, Objects::nonNull, "location", w);
        sb.append("]");
        return sb.toString();
    }

    private static <T> boolean addToStringField(StringBuilder sb, T o, Function<T, Boolean> p, String n, boolean w) {
        if (!p.apply(o)) {
            if (w) {
                sb.append(", ");
            } else {
                w = true;
            }
            sb.append(n).append("='").append(o).append('\'');
        }
        return w;
    }
}
