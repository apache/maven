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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * An immutable XML node representation that provides a clean API for working with XML data structures.
 * This interface represents a single node in an XML document tree, containing information about
 * the node's name, value, attributes, and child nodes.
 *
 * <p>Example usage:</p>
 * <pre>
 * XmlNode node = XmlNode.newBuilder()
 *     .name("configuration")
 *     .attribute("version", "1.0")
 *     .child(XmlNode.newInstance("property", "value"))
 *     .build();
 * </pre>
 *
 * @since 4.0.0
 */
@Experimental
@ThreadSafe
@Immutable
public interface XmlNode {

    @Deprecated(since = "4.0.0", forRemoval = true)
    String CHILDREN_COMBINATION_MODE_ATTRIBUTE = XmlService.CHILDREN_COMBINATION_MODE_ATTRIBUTE;

    @Deprecated(since = "4.0.0", forRemoval = true)
    String CHILDREN_COMBINATION_MERGE = XmlService.CHILDREN_COMBINATION_MERGE;

    @Deprecated(since = "4.0.0", forRemoval = true)
    String CHILDREN_COMBINATION_APPEND = XmlService.CHILDREN_COMBINATION_APPEND;

    /**
     * This default mode for combining children DOMs during merge means that where element names match, the process will
     * try to merge the element data, rather than putting the dominant and recessive elements (which share the same
     * element name) as siblings in the resulting DOM.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    String DEFAULT_CHILDREN_COMBINATION_MODE = XmlService.DEFAULT_CHILDREN_COMBINATION_MODE;

    @Deprecated(since = "4.0.0", forRemoval = true)
    String SELF_COMBINATION_MODE_ATTRIBUTE = XmlService.SELF_COMBINATION_MODE_ATTRIBUTE;

    @Deprecated(since = "4.0.0", forRemoval = true)
    String SELF_COMBINATION_OVERRIDE = XmlService.SELF_COMBINATION_OVERRIDE;

    @Deprecated(since = "4.0.0", forRemoval = true)
    String SELF_COMBINATION_MERGE = XmlService.SELF_COMBINATION_MERGE;

    @Deprecated(since = "4.0.0", forRemoval = true)
    String SELF_COMBINATION_REMOVE = XmlService.SELF_COMBINATION_REMOVE;

    /**
     * In case of complex XML structures, combining can be done based on id.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    String ID_COMBINATION_MODE_ATTRIBUTE = XmlService.ID_COMBINATION_MODE_ATTRIBUTE;

    /**
     * In case of complex XML structures, combining can be done based on keys.
     * This is a comma separated list of attribute names.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    String KEYS_COMBINATION_MODE_ATTRIBUTE = XmlService.KEYS_COMBINATION_MODE_ATTRIBUTE;

    /**
     * This default mode for combining a DOM node during merge means that where element names match, the process will
     * try to merge the element attributes and values, rather than overriding the recessive element completely with the
     * dominant one. This means that wherever the dominant element doesn't provide the value or a particular attribute,
     * that value or attribute will be set from the recessive DOM node.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    String DEFAULT_SELF_COMBINATION_MODE = XmlService.DEFAULT_SELF_COMBINATION_MODE;

    /**
     * Returns the local name of this XML node.
     *
     * @return the node name, never {@code null}
     */
    @Nonnull
    String name();

    /**
     * Returns the namespace URI of this XML node.
     *
     * @return the namespace URI, never {@code null} (empty string if no namespace)
     */
    @Nonnull
    String namespaceUri();

    /**
     * Returns the namespace prefix of this XML node.
     *
     * @return the namespace prefix, never {@code null} (empty string if no prefix)
     */
    @Nonnull
    String prefix();

    /**
     * Returns the text content of this XML node.
     *
     * @return the node's text value, or {@code null} if none exists
     */
    @Nullable
    String value();

    /**
     * Returns an immutable map of all attributes defined on this XML node.
     *
     * @return map of attribute names to values, never {@code null}
     */
    @Nonnull
    Map<String, String> attributes();

    /**
     * Returns the value of a specific attribute.
     *
     * @param name the name of the attribute to retrieve
     * @return the attribute value, or {@code null} if the attribute doesn't exist
     * @throws NullPointerException if name is null
     */
    @Nullable
    String attribute(@Nonnull String name);

    /**
     * Returns an immutable list of all child nodes.
     *
     * @return list of child nodes, never {@code null}
     */
    @Nonnull
    List<XmlNode> children();

    /**
     * Returns the first child node with the specified name.
     *
     * @param name the name of the child node to find
     * @return the first matching child node, or {@code null} if none found
     */
    @Nullable
    XmlNode child(String name);

    /**
     * Returns the input location information for this node, if available.
     * This can be useful for error reporting and debugging.
     *
     * @return the input location object, or {@code null} if not available
     */
    @Nullable
    Object inputLocation();

    // Deprecated methods that delegate to new ones
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default String getName() {
        return name();
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default String getNamespaceUri() {
        return namespaceUri();
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default String getPrefix() {
        return prefix();
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default String getValue() {
        return value();
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default Map<String, String> getAttributes() {
        return attributes();
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default String getAttribute(@Nonnull String name) {
        return attribute(name);
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nonnull
    default List<XmlNode> getChildren() {
        return children();
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default XmlNode getChild(String name) {
        return child(name);
    }

    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default Object getInputLocation() {
        return inputLocation();
    }

    /**
     * @deprecated use {@link XmlService#merge(XmlNode, XmlNode, Boolean)} instead
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    default XmlNode merge(@Nullable XmlNode source) {
        return XmlService.merge(this, source);
    }

    /**
     * @deprecated use {@link XmlService#merge(XmlNode, XmlNode, Boolean)} instead
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    default XmlNode merge(@Nullable XmlNode source, @Nullable Boolean childMergeOverride) {
        return XmlService.merge(this, source, childMergeOverride);
    }

    /**
     * Merge recessive into dominant and return either {@code dominant}
     * with merged information or a clone of {@code recessive} if
     * {@code dominant} is {@code null}.
     *
     * @param dominant the node
     * @param recessive if {@code null}, nothing will happen
     * @return the merged node
     *
     * @deprecated use {@link XmlService#merge(XmlNode, XmlNode, Boolean)} instead
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    static XmlNode merge(@Nullable XmlNode dominant, @Nullable XmlNode recessive) {
        return XmlService.merge(dominant, recessive, null);
    }

    @Nullable
    static XmlNode merge(
            @Nullable XmlNode dominant, @Nullable XmlNode recessive, @Nullable Boolean childMergeOverride) {
        return XmlService.merge(dominant, recessive, childMergeOverride);
    }

    /**
     * Creates a new XmlNode instance with the specified name.
     *
     * @param name the name for the new node
     * @return a new XmlNode instance
     * @throws NullPointerException if name is null
     */
    static XmlNode newInstance(String name) {
        return newBuilder().name(name).build();
    }

    /**
     * Creates a new XmlNode instance with the specified name and value.
     *
     * @param name the name for the new node
     * @param value the value for the new node
     * @return a new XmlNode instance
     * @throws NullPointerException if name is null
     */
    static XmlNode newInstance(String name, String value) {
        return newBuilder().name(name).value(value).build();
    }

    /**
     * Creates a new XmlNode instance with the specified name and children.
     *
     * @param name the name for the new node
     * @param children the list of child nodes
     * @return a new XmlNode instance
     * @throws NullPointerException if name is null
     */
    static XmlNode newInstance(String name, List<XmlNode> children) {
        return newBuilder().name(name).children(children).build();
    }

    /**
     * Creates a new XmlNode instance with all properties specified.
     *
     * @param name the name for the new node
     * @param value the value for the new node
     * @param attrs the attributes for the new node
     * @param children the list of child nodes
     * @param location the input location information
     * @return a new XmlNode instance
     * @throws NullPointerException if name is null
     */
    static XmlNode newInstance(
            String name, String value, Map<String, String> attrs, List<XmlNode> children, Object location) {
        return newBuilder()
                .name(name)
                .value(value)
                .attributes(attrs)
                .children(children)
                .inputLocation(location)
                .build();
    }

    /**
     * Returns a new builder for creating XmlNode instances.
     *
     * @return a new Builder instance
     */
    static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder class for creating XmlNode instances.
     * <p>
     * This builder provides a fluent API for setting the various properties of an XML node.
     * All properties are optional except for the node name, which must be set before calling
     * {@link #build()}.
     */
    class Builder {
        private String name;
        private String value;
        private String namespaceUri;
        private String prefix;
        private Map<String, String> attributes;
        private List<XmlNode> children;
        private Object inputLocation;

        /**
         * Sets the name of the XML node.
         * <p>
         * This is the only required property that must be set before calling {@link #build()}.
         *
         * @param name the name of the XML node
         * @return this builder instance
         * @throws NullPointerException if name is null
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the text content of the XML node.
         *
         * @param value the text content of the XML node
         * @return this builder instance
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the namespace URI of the XML node.
         *
         * @param namespaceUri the namespace URI of the XML node
         * @return this builder instance
         */
        public Builder namespaceUri(String namespaceUri) {
            this.namespaceUri = namespaceUri;
            return this;
        }

        /**
         * Sets the namespace prefix of the XML node.
         *
         * @param prefix the namespace prefix of the XML node
         * @return this builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the attributes of the XML node.
         * <p>
         * The provided map will be copied to ensure immutability.
         *
         * @param attributes the map of attribute names to values
         * @return this builder instance
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Sets the child nodes of the XML node.
         * <p>
         * The provided list will be copied to ensure immutability.
         *
         * @param children the list of child nodes
         * @return this builder instance
         */
        public Builder children(List<XmlNode> children) {
            this.children = children;
            return this;
        }

        /**
         * Sets the input location information for the XML node.
         * <p>
         * This is typically used for error reporting and debugging purposes.
         *
         * @param inputLocation the input location object
         * @return this builder instance
         */
        public Builder inputLocation(Object inputLocation) {
            this.inputLocation = inputLocation;
            return this;
        }

        /**
         * Builds a new XmlNode instance with the current builder settings.
         *
         * @return a new immutable XmlNode instance
         * @throws NullPointerException if name has not been set
         */
        public XmlNode build() {
            return new Impl(prefix, namespaceUri, name, value, attributes, children, inputLocation);
        }

        private record Impl(
                String prefix,
                String namespaceUri,
                @Nonnull String name,
                String value,
                @Nonnull Map<String, String> attributes,
                @Nonnull List<XmlNode> children,
                Object inputLocation)
                implements XmlNode, Serializable {

            private Impl {
                // Validation and normalization from the original constructor
                prefix = prefix == null ? "" : prefix;
                namespaceUri = namespaceUri == null ? "" : namespaceUri;
                name = Objects.requireNonNull(name);
                attributes = ImmutableCollections.copy(attributes);
                children = ImmutableCollections.copy(children);
            }

            @Override
            public String attribute(@Nonnull String name) {
                return attributes.get(name);
            }

            @Override
            public XmlNode child(String name) {
                if (name != null) {
                    ListIterator<XmlNode> it = children.listIterator(children.size());
                    while (it.hasPrevious()) {
                        XmlNode child = it.previous();
                        if (name.equals(child.name())) {
                            return child;
                        }
                    }
                }
                return null;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Impl that = (Impl) o;
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

            private String toStringObject() {
                StringBuilder sb = new StringBuilder();
                sb.append("XmlNode[");
                boolean w = false;
                w = addToStringField(sb, prefix, o -> !o.isEmpty(), "prefix", w);
                w = addToStringField(sb, namespaceUri, o -> !o.isEmpty(), "namespaceUri", w);
                w = addToStringField(sb, name, o -> !o.isEmpty(), "name", w);
                w = addToStringField(sb, value, o -> !o.isEmpty(), "value", w);
                w = addToStringField(sb, attributes, o -> !o.isEmpty(), "attributes", w);
                w = addToStringField(sb, children, o -> !o.isEmpty(), "children", w);
                w = addToStringField(sb, inputLocation, Objects::nonNull, "inputLocation", w);
                sb.append("]");
                return sb.toString();
            }

            private static <T> boolean addToStringField(
                    StringBuilder sb, T o, Function<T, Boolean> p, String n, boolean w) {
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
    }
}
