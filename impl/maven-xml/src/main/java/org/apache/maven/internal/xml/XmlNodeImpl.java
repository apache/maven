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
@SuppressWarnings("removal")
public class XmlNodeImpl implements Serializable, XmlNode {
    @Serial
    private static final long serialVersionUID = 2567894443061173996L;

    @Nonnull
    protected final String prefix;

    @Nonnull
    protected final String namespaceUri;

    @Nonnull
    protected final String name;

    protected final String value;

    @Nonnull
    protected final Map<String, String> attributes;

    @Nonnull
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

    @SuppressWarnings("removal")
    @Override
    public XmlNode merge(XmlNode source, Boolean childMergeOverride) {
        return XmlService.merge(this, source, childMergeOverride);
    }

    // ----------------------------------------------------------------------
    // Name handling
    // ----------------------------------------------------------------------

    @Override
    @Nonnull
    @Deprecated(since = "4.0.0", forRemoval = true)
    public String getPrefix() {
        return prefix;
    }

    @Override
    @Nonnull
    public String prefix() {
        return getPrefix();
    }

    @Override
    @Nonnull
    @Deprecated(since = "4.0.0", forRemoval = true)
    public String getNamespaceUri() {
        return namespaceUri;
    }

    @Override
    @Nonnull
    public String namespaceUri() {
        return getNamespaceUri();
    }

    @Override
    @Nonnull
    @Deprecated(since = "4.0.0", forRemoval = true)
    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public String name() {
        return getName();
    }

    // ----------------------------------------------------------------------
    // Value handling
    // ----------------------------------------------------------------------

    @Override
    @Deprecated(since = "4.0.0", forRemoval = true)
    public String getValue() {
        return value;
    }

    @Override
    public String value() {
        return getValue();
    }

    // ----------------------------------------------------------------------
    // Attribute handling
    // ----------------------------------------------------------------------

    @Override
    @Nonnull
    @Deprecated(since = "4.0.0", forRemoval = true)
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    @Nonnull
    public Map<String, String> attributes() {
        return getAttributes();
    }

    @Override
    @Deprecated(since = "4.0.0", forRemoval = true)
    public String getAttribute(@Nonnull String name) {
        return attributes.get(name);
    }

    @Override
    public String attribute(@Nonnull String name) {
        return getAttribute(name);
    }

    // ----------------------------------------------------------------------
    // Child handling
    // ----------------------------------------------------------------------

    @Deprecated(since = "4.0.0", forRemoval = true)
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

    @Override
    public XmlNode child(String name) {
        return getChild(name);
    }

    @Override
    @Nonnull
    @Deprecated(since = "4.0.0", forRemoval = true)
    public List<XmlNode> getChildren() {
        return children;
    }

    @Override
    @Nonnull
    public List<XmlNode> children() {
        return getChildren();
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
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
    @Deprecated(since = "4.0.0", forRemoval = true)
    public Object getInputLocation() {
        return location;
    }

    @Override
    public Object inputLocation() {
        return getInputLocation();
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    @SuppressWarnings("checkstyle:MethodLength")
    public static XmlNode merge(XmlNode dominant, XmlNode recessive, Boolean childMergeOverride) {
        return XmlService.merge(dominant, recessive, childMergeOverride);
    }

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
}
