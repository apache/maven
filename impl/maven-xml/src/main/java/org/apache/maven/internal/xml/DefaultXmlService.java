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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

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

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;
import org.codehaus.stax2.util.StreamWriterDelegate;

public class DefaultXmlService extends XmlService {
    private static final boolean DEFAULT_TRIM = true;

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
        return doBuild(parser, DEFAULT_TRIM, locationBuilder);
    }

    private XmlNode doBuild(XMLStreamReader parser, boolean trim, InputLocationBuilder locationBuilder)
            throws XMLStreamException {
        boolean spacePreserve = false;
        String lPrefix = null;
        String lNamespaceUri = null;
        String lName = null;
        String lValue = null;
        Object location = null;
        Map<String, String> attrs = null;
        List<XmlNode> children = null;
        int eventType = parser.getEventType();
        int lastStartTag = -1;
        while (eventType != XMLStreamReader.END_DOCUMENT) {
            if (eventType == XMLStreamReader.START_ELEMENT) {
                lastStartTag = parser.getLocation().getLineNumber() * 1000
                        + parser.getLocation().getColumnNumber();
                if (lName == null) {
                    int namespacesSize = parser.getNamespaceCount();
                    lPrefix = parser.getPrefix();
                    lNamespaceUri = parser.getNamespaceURI();
                    lName = parser.getLocalName();
                    location = locationBuilder != null ? locationBuilder.toInputLocation(parser) : null;
                    int attributesSize = parser.getAttributeCount();
                    if (attributesSize > 0 || namespacesSize > 0) {
                        attrs = new HashMap<>();
                        for (int i = 0; i < namespacesSize; i++) {
                            String nsPrefix = parser.getNamespacePrefix(i);
                            String nsUri = parser.getNamespaceURI(i);
                            attrs.put(nsPrefix != null && !nsPrefix.isEmpty() ? "xmlns:" + nsPrefix : "xmlns", nsUri);
                        }
                        for (int i = 0; i < attributesSize; i++) {
                            String aName = parser.getAttributeLocalName(i);
                            String aValue = parser.getAttributeValue(i);
                            String aPrefix = parser.getAttributePrefix(i);
                            if (aPrefix != null && !aPrefix.isEmpty()) {
                                aName = aPrefix + ":" + aName;
                            }
                            attrs.put(aName, aValue);
                            spacePreserve = spacePreserve || ("xml:space".equals(aName) && "preserve".equals(aValue));
                        }
                    }
                } else {
                    if (children == null) {
                        children = new ArrayList<>();
                    }
                    XmlNode child = doBuild(parser, trim, locationBuilder);
                    children.add(child);
                }
            } else if (eventType == XMLStreamReader.CHARACTERS || eventType == XMLStreamReader.CDATA) {
                String text = parser.getText();
                lValue = lValue != null ? lValue + text : text;
            } else if (eventType == XMLStreamReader.END_ELEMENT) {
                boolean emptyTag = lastStartTag
                        == parser.getLocation().getLineNumber() * 1000
                                + parser.getLocation().getColumnNumber();
                if (lValue != null && trim && !spacePreserve) {
                    lValue = lValue.trim();
                }
                return XmlNode.newBuilder()
                        .prefix(lPrefix)
                        .namespaceUri(lNamespaceUri)
                        .name(lName)
                        .value(children == null ? (lValue != null ? lValue : emptyTag ? null : "") : null)
                        .attributes(attrs)
                        .children(children)
                        .inputLocation(location)
                        .build();
            }
            eventType = parser.next();
        }
        throw new IllegalStateException("End of document found before returning to 0 depth");
    }

    @Override
    public void doWrite(XmlNode node, Writer writer) throws IOException {
        try {
            XMLOutputFactory factory = new WstxOutputFactory();
            factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
            factory.setProperty(WstxOutputProperties.P_USE_DOUBLE_QUOTES_IN_XML_DECL, true);
            factory.setProperty(WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM, true);
            XMLStreamWriter serializer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(writer));
            writeNode(serializer, node);
            serializer.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void writeNode(XMLStreamWriter xmlWriter, XmlNode node) throws XMLStreamException {
        xmlWriter.writeStartElement(node.prefix(), node.name(), node.namespaceUri());

        for (Map.Entry<String, String> attr : node.attributes().entrySet()) {
            xmlWriter.writeAttribute(attr.getKey(), attr.getValue());
        }

        for (XmlNode child : node.children()) {
            writeNode(xmlWriter, child);
        }

        String value = node.value();
        if (value != null) {
            xmlWriter.writeCharacters(value);
        }

        xmlWriter.writeEndElement();
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

            String value = dominant.value();
            Object location = dominant.inputLocation();
            Map<String, String> attrs = dominant.attributes();
            List<XmlNode> children = null;

            for (Map.Entry<String, String> attr : recessive.attributes().entrySet()) {
                String key = attr.getKey();
                if (isEmpty(attrs.get(key))) {
                    if (attrs == dominant.attributes()) {
                        attrs = new HashMap<>(attrs);
                    }
                    attrs.put(key, attr.getValue());
                }
            }

            if (!recessive.children().isEmpty()) {
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
                        recessive.children().stream().map(XmlNode::name).collect(Collectors.toSet());
                for (String name : names) {
                    List<XmlNode> dominantChildren = dominant.children().stream()
                            .filter(n -> n.name().equals(name))
                            .toList();
                    if (!dominantChildren.isEmpty()) {
                        commonChildren.put(name, dominantChildren.iterator());
                    }
                }

                String keysValue = recessive.attribute(KEYS_COMBINATION_MODE_ATTRIBUTE);

                int recessiveChildIndex = 0;
                for (XmlNode recessiveChild : recessive.children()) {
                    String idValue = recessiveChild.attribute(ID_COMBINATION_MODE_ATTRIBUTE);

                    XmlNode childDom = null;
                    if (!isEmpty(idValue)) {
                        for (XmlNode dominantChild : dominant.children()) {
                            if (idValue.equals(dominantChild.attribute(ID_COMBINATION_MODE_ATTRIBUTE))) {
                                childDom = dominantChild;
                                // we have a match, so don't append but merge
                                mergeChildren = true;
                            }
                        }
                    } else if (!isEmpty(keysValue)) {
                        String[] keys = keysValue.split(",");
                        Map<String, Optional<String>> recessiveKeyValues = Stream.of(keys)
                                .collect(Collectors.toMap(
                                        k -> k, k -> Optional.ofNullable(recessiveChild.attribute(k))));

                        for (XmlNode dominantChild : dominant.children()) {
                            Map<String, Optional<String>> dominantKeyValues = Stream.of(keys)
                                    .collect(Collectors.toMap(
                                            k -> k, k -> Optional.ofNullable(dominantChild.attribute(k))));

                            if (recessiveKeyValues.equals(dominantKeyValues)) {
                                childDom = dominantChild;
                                // we have a match, so don't append but merge
                                mergeChildren = true;
                            }
                        }
                    } else {
                        childDom = dominant.child(recessiveChild.name());
                    }

                    if (mergeChildren && childDom != null) {
                        String name = recessiveChild.name();
                        Iterator<XmlNode> it =
                                commonChildren.computeIfAbsent(name, n1 -> Stream.of(dominant.children().stream()
                                                .filter(n2 -> n2.name().equals(n1))
                                                .collect(Collectors.toList()))
                                        .filter(l -> !l.isEmpty())
                                        .map(List::iterator)
                                        .findFirst()
                                        .orElse(null));
                        if (it == null) {
                            if (children == null) {
                                children = new ArrayList<>(dominant.children());
                            }
                            children.add(recessiveChild);
                        } else if (it.hasNext()) {
                            XmlNode dominantChild = it.next();

                            String dominantChildCombinationMode = getSelfCombinationMode(dominantChild);
                            if (SELF_COMBINATION_REMOVE.equals(dominantChildCombinationMode)) {
                                if (children == null) {
                                    children = new ArrayList<>(dominant.children());
                                }
                                children.remove(dominantChild);
                            } else {
                                int idx = dominant.children().indexOf(dominantChild);
                                XmlNode merged = merge(dominantChild, recessiveChild, childMergeOverride);
                                if (merged != dominantChild) {
                                    if (children == null) {
                                        children = new ArrayList<>(dominant.children());
                                    }
                                    children.set(idx, merged);
                                }
                            }
                        }
                    } else {
                        if (children == null) {
                            children = new ArrayList<>(dominant.children());
                        }
                        int idx = mergeChildren ? children.size() : recessiveChildIndex;
                        children.add(idx, recessiveChild);
                    }
                    recessiveChildIndex++;
                }
            }

            if (value != null || attrs != dominant.attributes() || children != null) {
                if (children == null) {
                    children = dominant.children();
                }
                if (!Objects.equals(value, dominant.value())
                        || !Objects.equals(attrs, dominant.attributes())
                        || !Objects.equals(children, dominant.children())
                        || !Objects.equals(location, dominant.inputLocation())) {
                    return XmlNode.newBuilder()
                            .prefix(dominant.prefix())
                            .namespaceUri(dominant.namespaceUri())
                            .name(dominant.name())
                            .value(value != null ? value : dominant.value())
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
        String value = node.attribute(SELF_COMBINATION_MODE_ATTRIBUTE);
        return !isEmpty(value) ? value : DEFAULT_SELF_COMBINATION_MODE;
    }

    private static String getChildCombinationMode(Map<String, String> attributes) {
        String value = attributes.get(CHILDREN_COMBINATION_MODE_ATTRIBUTE);
        return !isEmpty(value) ? value : DEFAULT_CHILDREN_COMBINATION_MODE;
    }

    @Nullable
    private static XmlNode findNodeById(@Nonnull List<XmlNode> nodes, @Nonnull String id) {
        return nodes.stream()
                .filter(n -> id.equals(n.attribute(ID_COMBINATION_MODE_ATTRIBUTE)))
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
            String value1 = node1.attribute(key);
            String value2 = node2.attribute(key);
            if (!Objects.equals(value1, value2)) {
                return false;
            }
        }
        return true;
    }

    static class IndentingXMLStreamWriter extends StreamWriterDelegate {

        int depth = 0;
        boolean hasChildren = false;
        boolean anew = true;

        IndentingXMLStreamWriter(XMLStreamWriter parent) {
            super(parent);
        }

        @Override
        public void writeStartDocument() throws XMLStreamException {
            super.writeStartDocument();
            anew = false;
        }

        @Override
        public void writeStartDocument(String version) throws XMLStreamException {
            super.writeStartDocument(version);
            anew = false;
        }

        @Override
        public void writeStartDocument(String encoding, String version) throws XMLStreamException {
            super.writeStartDocument(encoding, version);
            anew = false;
        }

        @Override
        public void writeEmptyElement(String localName) throws XMLStreamException {
            indent();
            super.writeEmptyElement(localName);
            hasChildren = true;
            anew = false;
        }

        @Override
        public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
            indent();
            super.writeEmptyElement(namespaceURI, localName);
            hasChildren = true;
            anew = false;
        }

        @Override
        public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            indent();
            super.writeEmptyElement(prefix, localName, namespaceURI);
            hasChildren = true;
            anew = false;
        }

        @Override
        public void writeStartElement(String localName) throws XMLStreamException {
            indent();
            super.writeStartElement(localName);
            depth++;
            hasChildren = false;
            anew = false;
        }

        @Override
        public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
            indent();
            super.writeStartElement(namespaceURI, localName);
            depth++;
            hasChildren = false;
            anew = false;
        }

        @Override
        public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            indent();
            super.writeStartElement(prefix, localName, namespaceURI);
            depth++;
            hasChildren = false;
            anew = false;
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            depth--;
            if (hasChildren) {
                indent();
            }
            super.writeEndElement();
            hasChildren = true;
            anew = false;
        }

        private void indent() throws XMLStreamException {
            if (!anew) {
                super.writeCharacters("\n");
            }
            for (int i = 0; i < depth; i++) {
                super.writeCharacters("  ");
            }
        }
    }
}
