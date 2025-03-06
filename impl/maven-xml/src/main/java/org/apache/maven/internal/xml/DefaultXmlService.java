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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;

public class DefaultXmlService extends XmlService {

    @Nonnull
    @Override
    public XmlNode doRead(InputStream input, @Nullable XmlService.InputLocationBuilder locationBuilder)
            throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newFactory().createXMLStreamReader(input);
        return doRead(parser, locationBuilder);
    }

    @Nonnull
    @Override
    public XmlNode doRead(Reader reader, @Nullable XmlService.InputLocationBuilder locationBuilder)
            throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newFactory().createXMLStreamReader(reader);
        return doRead(parser, locationBuilder);
    }

    @Nonnull
    @Override
    public XmlNode doRead(XMLStreamReader parser, @Nullable XmlService.InputLocationBuilder locationBuilder)
            throws XMLStreamException {
        return XmlNodeStaxBuilder.build(
                parser, true, locationBuilder != null ? locationBuilder::toInputLocation : null);
    }

    @Override
    public void doWrite(XmlNode node, Writer writer) throws IOException {
        try {
            XmlNodeWriter.write(writer, node);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

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
    public XmlNode doMerge(XmlNode dominant, XmlNode recessive, Boolean childMergeOverride) {
        // TODO: share this as some sort of assembler, implement a walk interface?
        if (recessive == null) {
            return dominant;
        }
        if (dominant == null) {
            return recessive;
        }

        boolean mergeSelf = true;

        String selfMergeMode = getSelfCombinationMode(dominant);

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
                    String childCombinationMode = getChildCombinationMode(attrs);
                    if (CHILDREN_COMBINATION_APPEND.equals(childCombinationMode)) {
                        mergeChildren = false;
                    }
                }

                Map<String, Iterator<XmlNode>> commonChildren = new HashMap<>();
                Set<String> names =
                        recessive.getChildren().stream().map(XmlNode::getName).collect(Collectors.toSet());
                for (String name : names) {
                    List<XmlNode> dominantChildren = dominant.getChildren().stream()
                            .filter(n -> n.getName().equals(name))
                            .toList();
                    if (!dominantChildren.isEmpty()) {
                        commonChildren.put(name, dominantChildren.iterator());
                    }
                }

                String keysValue = recessive.getAttribute(KEYS_COMBINATION_MODE_ATTRIBUTE);

                int recessiveChildIndex = 0;
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

                            String dominantChildCombinationMode = getSelfCombinationMode(dominantChild);
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
                        int idx = mergeChildren ? children.size() : recessiveChildIndex;
                        children.add(idx, recessiveChild);
                    }
                    recessiveChildIndex++;
                }
            }

            if (value != null || attrs != dominant.getAttributes() || children != null) {
                if (children == null) {
                    children = dominant.getChildren();
                }
                if (!Objects.equals(value, dominant.getValue())
                        || !Objects.equals(attrs, dominant.getAttributes())
                        || !Objects.equals(children, dominant.getChildren())
                        || !Objects.equals(location, dominant.getInputLocation())) {
                    return XmlNode.newBuilder()
                            .prefix(dominant.getPrefix())
                            .namespaceUri(dominant.getNamespaceUri())
                            .name(dominant.getName())
                            .value(value != null ? value : dominant.getValue())
                            .attributes(attrs)
                            .children(children)
                            .inputLocation(location)
                            .build();
                } else {
                    return dominant;
                }
            }
        }
        return dominant;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static String getSelfCombinationMode(XmlNode node) {
        String value = node.getAttribute(SELF_COMBINATION_MODE_ATTRIBUTE);
        return !isEmpty(value) ? value : DEFAULT_SELF_COMBINATION_MODE;
    }

    private static String getChildCombinationMode(Map<String, String> attributes) {
        String value = attributes.get(CHILDREN_COMBINATION_MODE_ATTRIBUTE);
        return !isEmpty(value) ? value : DEFAULT_CHILDREN_COMBINATION_MODE;
    }

    @Nullable
    private static XmlNode findNodeById(@Nonnull List<XmlNode> nodes, @Nonnull String id) {
        return nodes.stream()
                .filter(n -> id.equals(n.getAttribute(ID_COMBINATION_MODE_ATTRIBUTE)))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static XmlNode findNodeByKeys(
            @Nonnull List<XmlNode> nodes, @Nonnull XmlNode target, @Nonnull String[] keys) {
        return nodes.stream()
                .filter(n -> matchesKeys(n, target, keys))
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesKeys(@Nonnull XmlNode node1, @Nonnull XmlNode node2, @Nonnull String[] keys) {
        for (String key : keys) {
            String value1 = node1.getAttribute(key);
            String value2 = node2.getAttribute(key);
            if (!Objects.equals(value1, value2)) {
                return false;
            }
        }
        return true;
    }
}
