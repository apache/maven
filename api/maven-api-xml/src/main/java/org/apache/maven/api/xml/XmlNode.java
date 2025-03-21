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
 * An immutable xml node.
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

    @Nonnull
    String getName();

    @Nonnull
    String getNamespaceUri();

    @Nonnull
    String getPrefix();

    @Nullable
    String getValue();

    @Nonnull
    Map<String, String> getAttributes();

    @Nullable
    String getAttribute(@Nonnull String name);

    @Nonnull
    List<XmlNode> getChildren();

    @Nullable
    XmlNode getChild(String name);

    @Nullable
    Object getInputLocation();

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

    static XmlNode newInstance(String name) {
        return newBuilder().name(name).build();
    }

    static XmlNode newInstance(String name, String value) {
        return newBuilder().name(name).value(value).build();
    }

    static XmlNode newInstance(String name, List<XmlNode> children) {
        return newBuilder().name(name).children(children).build();
    }

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

    static Builder newBuilder() {
        return new Builder();
    }

    class Builder {
        private String name;
        private String value;
        private String namespaceUri;
        private String prefix;
        private Map<String, String> attributes;
        private List<XmlNode> children;
        private Object inputLocation;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder namespaceUri(String namespaceUri) {
            this.namespaceUri = namespaceUri;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder children(List<XmlNode> children) {
            this.children = children;
            return this;
        }

        public Builder inputLocation(Object inputLocation) {
            this.inputLocation = inputLocation;
            return this;
        }

        public XmlNode build() {
            return new Impl(prefix, namespaceUri, name, value, attributes, children, inputLocation);
        }

        private static class Impl implements XmlNode, Serializable {
            @Nonnull
            private final String prefix;

            @Nonnull
            private final String namespaceUri;

            @Nonnull
            private final String name;

            private final String value;

            @Nonnull
            private final Map<String, String> attributes;

            @Nonnull
            private final List<XmlNode> children;

            private final Object location;

            private Impl(
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

            @Override
            @Nonnull
            public String getPrefix() {
                return prefix;
            }

            @Override
            @Nonnull
            public String getNamespaceUri() {
                return namespaceUri;
            }

            @Override
            @Nonnull
            public String getName() {
                return name;
            }

            @Override
            public String getValue() {
                return value;
            }

            @Override
            @Nonnull
            public Map<String, String> getAttributes() {
                return attributes;
            }

            @Override
            public String getAttribute(@Nonnull String name) {
                return attributes.get(name);
            }

            @Override
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
            @Nonnull
            public List<XmlNode> getChildren() {
                return children;
            }

            @Override
            public Object getInputLocation() {
                return location;
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
                w = addToStringField(sb, location, Objects::nonNull, "location", w);
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
