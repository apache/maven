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

import javax.xml.stream.XMLStreamException;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.xml.XmlNode;

/**
 *  NOTE: remove all the util code in here when separated, this class should be pure data.
 */
public class XmlNodeImpl implements Serializable, XmlNode {
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

    @Override
    public XmlNode merge(XmlNode source, Boolean childMergeOverride) {
        return merge(this, source, childMergeOverride);
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
     * Merges one DOM into another, given a specific algorithm and possible override points for that algorithm.<p>
     * The algorithm is as follows:
     * <ol>
     * <li> if the recessive DOM is null, there is nothing to do... return.</li>
     * <li> Determine whether the dominant node will suppress the recessive one (flag=mergeSelf).
     *   <ol type="A">
     *   <li> retrieve the 'combine.self' attribute on the dominant node, and try to match against 'override'...
     *        if it matches 'override', then set mergeSelf == false...the dominant node suppresses the recessive one
     *        completely.</li>
     *   <li> otherwise, use the default value for mergeSelf, which is true...this is the same as specifying
     *        'combine.self' == 'merge' as an attribute of the dominant root node.</li>
     *   </ol></li>
     * <li> If mergeSelf == true
     *   <ol type="A">
     *   <li> Determine whether children from the recessive DOM will be merged or appended to the dominant DOM as
     *        siblings (flag=mergeChildren).
     *     <ol type="i">
     *     <li> if childMergeOverride is set (non-null), use that value (true/false)</li>
     *     <li> retrieve the 'combine.children' attribute on the dominant node, and try to match against
     *          'append'...</li>
     *     <li> if it matches 'append', then set mergeChildren == false...the recessive children will be appended as
     *          siblings of the dominant children.</li>
     *     <li> otherwise, use the default value for mergeChildren, which is true...this is the same as specifying
     *         'combine.children' == 'merge' as an attribute on the dominant root node.</li>
     *     </ol></li>
     *   <li> Iterate through the recessive children, and:
     *     <ol type="i">
     *     <li> if mergeChildren == true and there is a corresponding dominant child (matched by element name),
     *          merge the two.</li>
     *     <li> otherwise, add the recessive child as a new child on the dominant root node.</li>
     *     </ol></li>
     *   </ol></li>
     * </ol>
     */
    @SuppressWarnings("checkstyle:MethodLength")
    public static XmlNode merge(XmlNode dominant, XmlNode recessive, Boolean childMergeOverride) {
        // TODO: share this as some sort of assembler, implement a walk interface?
        if (recessive == null) {
            return dominant;
        }
        if (dominant == null) {
            return recessive;
        }

        boolean mergeSelf = true;

        String selfMergeMode = dominant.getAttribute(SELF_COMBINATION_MODE_ATTRIBUTE);

        if (SELF_COMBINATION_OVERRIDE.equals(selfMergeMode)) {
            mergeSelf = false;
        }

        if (mergeSelf) {

            String value = dominant.getValue();
            Object location = dominant.getInputLocation();
            Map<String, String> attrs = dominant.getAttributes();
            List<XmlNode> children = null;

            for (Map.Entry<String, String> attr : recessive.getAttributes().entrySet()) {
                String key = attr.getKey();
                if (isEmpty(attrs.get(key))) {
                    if (attrs == dominant.getAttributes()) {
                        attrs = new HashMap<>(attrs);
                    }
                    attrs.put(key, attr.getValue());
                }
            }

            if (!recessive.getChildren().isEmpty()) {
                boolean mergeChildren = true;
                if (childMergeOverride != null) {
                    mergeChildren = childMergeOverride;
                } else {
                    String childMergeMode = attrs.get(CHILDREN_COMBINATION_MODE_ATTRIBUTE);
                    if (CHILDREN_COMBINATION_APPEND.equals(childMergeMode)) {
                        mergeChildren = false;
                    }
                }

                Map<String, Iterator<XmlNode>> commonChildren = new HashMap<>();
                Set<String> names =
                        recessive.getChildren().stream().map(XmlNode::getName).collect(Collectors.toSet());
                for (String name : names) {
                    List<XmlNode> dominantChildren = dominant.getChildren().stream()
                            .filter(n -> n.getName().equals(name))
                            .collect(Collectors.toList());
                    if (!dominantChildren.isEmpty()) {
                        commonChildren.put(name, dominantChildren.iterator());
                    }
                }

                String keysValue = recessive.getAttribute(KEYS_COMBINATION_MODE_ATTRIBUTE);

                for (XmlNode recessiveChild : recessive.getChildren()) {
                    String idValue = recessiveChild.getAttribute(ID_COMBINATION_MODE_ATTRIBUTE);

                    XmlNode childDom = null;
                    if (!isEmpty(idValue)) {
                        for (XmlNode dominantChild : dominant.getChildren()) {
                            if (idValue.equals(dominantChild.getAttribute(ID_COMBINATION_MODE_ATTRIBUTE))) {
                                childDom = dominantChild;
                                // we have a match, so don't append but merge
                                mergeChildren = true;
                            }
                        }
                    } else if (!isEmpty(keysValue)) {
                        String[] keys = keysValue.split(",");
                        Map<String, Optional<String>> recessiveKeyValues = Stream.of(keys)
                                .collect(Collectors.toMap(
                                        k -> k, k -> Optional.ofNullable(recessiveChild.getAttribute(k))));

                        for (XmlNode dominantChild : dominant.getChildren()) {
                            Map<String, Optional<String>> dominantKeyValues = Stream.of(keys)
                                    .collect(Collectors.toMap(
                                            k -> k, k -> Optional.ofNullable(dominantChild.getAttribute(k))));

                            if (recessiveKeyValues.equals(dominantKeyValues)) {
                                childDom = dominantChild;
                                // we have a match, so don't append but merge
                                mergeChildren = true;
                            }
                        }
                    } else {
                        childDom = dominant.getChild(recessiveChild.getName());
                    }

                    if (mergeChildren && childDom != null) {
                        String name = recessiveChild.getName();
                        Iterator<XmlNode> it =
                                commonChildren.computeIfAbsent(name, n1 -> Stream.of(dominant.getChildren().stream()
                                                .filter(n2 -> n2.getName().equals(n1))
                                                .collect(Collectors.toList()))
                                        .filter(l -> !l.isEmpty())
                                        .map(List::iterator)
                                        .findFirst()
                                        .orElse(null));
                        if (it == null) {
                            if (children == null) {
                                children = new ArrayList<>(dominant.getChildren());
                            }
                            children.add(recessiveChild);
                        } else if (it.hasNext()) {
                            XmlNode dominantChild = it.next();

                            String dominantChildCombinationMode =
                                    dominantChild.getAttribute(SELF_COMBINATION_MODE_ATTRIBUTE);
                            if (SELF_COMBINATION_REMOVE.equals(dominantChildCombinationMode)) {
                                if (children == null) {
                                    children = new ArrayList<>(dominant.getChildren());
                                }
                                children.remove(dominantChild);
                            } else {
                                int idx = dominant.getChildren().indexOf(dominantChild);
                                XmlNode merged = merge(dominantChild, recessiveChild, childMergeOverride);
                                if (merged != dominantChild) {
                                    if (children == null) {
                                        children = new ArrayList<>(dominant.getChildren());
                                    }
                                    children.set(idx, merged);
                                }
                            }
                        }
                    } else {
                        if (children == null) {
                            children = new ArrayList<>(dominant.getChildren());
                        }
                        int idx = mergeChildren
                                ? children.size()
                                : recessive.getChildren().indexOf(recessiveChild);
                        children.add(idx, recessiveChild);
                    }
                }
            }

            if (value != null || attrs != dominant.getAttributes() || children != null) {
                if (children == null) {
                    children = dominant.getChildren();
                }
                return new XmlNodeImpl(
                        dominant.getName(), value != null ? value : dominant.getValue(), attrs, children, location);
            }
        }
        return dominant;
    }

    /**
     * Merge two DOMs, with one having dominance in the case of collision. Merge mechanisms (vs. override for nodes, or
     * vs. append for children) is determined by attributes of the dominant root node.
     *
     * @see #CHILDREN_COMBINATION_MODE_ATTRIBUTE
     * @see #SELF_COMBINATION_MODE_ATTRIBUTE
     * @param dominant The dominant DOM into which the recessive value/attributes/children will be merged
     * @param recessive The recessive DOM, which will be merged into the dominant DOM
     * @return merged DOM
     */
    public static XmlNode merge(XmlNode dominant, XmlNode recessive) {
        return merge(dominant, recessive, null);
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

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
