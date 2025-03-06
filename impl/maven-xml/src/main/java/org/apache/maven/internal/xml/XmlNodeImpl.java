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
package org.apache.maven.internal.xml;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;

/**
 *  NOTE: remove all the util code in here when separated, this class should be pure data.
 */
@Deprecated
public class XmlNodeImpl implements Serializable, XmlNode {
    @Serial
    private static final long serialVersionUID = 2567894443061173996L;

    protected final String prefix;

    protected final String namespaceUri;

    protected final String name;

    protected final String value;

    protected final Map<String, String> attributes;

    protected final List<XmlNode> children;

    protected final Object location;

    public XmlNodeImpl(String name) {
        this(name, null, null, null, null);
    }

    public XmlNodeImpl(String name, String value) {
        this(name, value, null, null, null);
    }

    public XmlNodeImpl(XmlNode from, String name) {
        this(name, from.getValue(), from.getAttributes(), from.getChildren(), from.getInputLocation());
    }

    public XmlNodeImpl(
            String name, String value, Map<String, String> attributes, List<XmlNode> children, Object location) {
        this("", "", name, value, attributes, children, location);
    }

    public XmlNodeImpl(
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

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getNamespaceUri() {
        return namespaceUri;
    }

    @Override
    @Nonnull
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

    @Override
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
    // Helpers
    // ----------------------------------------------------------------------

    /**
     * Merge two DOMs, with one having dominance in the case of collision. Merge mechanisms (vs. override for nodes, or
     * vs. append for children) is determined by attributes of the dominant root node.
     *
     * @see XmlService#CHILDREN_COMBINATION_MODE_ATTRIBUTE
     * @see XmlService#SELF_COMBINATION_MODE_ATTRIBUTE
     * @param dominant The dominant DOM into which the recessive value/attributes/children will be merged
     * @param recessive The recessive DOM, which will be merged into the dominant DOM
     * @return merged DOM
     *
     * @deprecated use {@link XmlService#merge(XmlNode, XmlNode, Boolean)} instead
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public static XmlNode merge(XmlNode dominant, XmlNode recessive) {
        return XmlService.merge(dominant, recessive);
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
        XmlNodeImpl that = (XmlNodeImpl) o;
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
            StringWriter writer = new StringWriter();
            XmlService.write(this, writer);
            return writer.toString();
        } catch (IOException e) {
            return toStringObject();
        }
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

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
