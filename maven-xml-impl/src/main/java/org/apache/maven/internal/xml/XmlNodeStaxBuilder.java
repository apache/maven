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

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.xml.XmlNode;

/**
 * All methods in this class attempt to fully parse the XML.
 * The caller is responsible for closing {@code InputStream} and {@code Reader} arguments.
 */
public class XmlNodeStaxBuilder {
    private static final boolean DEFAULT_TRIM = true;

    public static XmlNodeImpl build(InputStream stream, InputLocationBuilderStax locationBuilder)
            throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newFactory().createXMLStreamReader(stream);
        return build(parser, DEFAULT_TRIM, locationBuilder);
    }

    public static XmlNodeImpl build(Reader reader, InputLocationBuilderStax locationBuilder) throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newFactory().createXMLStreamReader(reader);
        return build(parser, DEFAULT_TRIM, locationBuilder);
    }

    public static XmlNodeImpl build(XMLStreamReader parser) throws XMLStreamException {
        return build(parser, DEFAULT_TRIM, null);
    }

    public static XmlNodeImpl build(XMLStreamReader parser, InputLocationBuilderStax locationBuilder)
            throws XMLStreamException {
        return build(parser, DEFAULT_TRIM, locationBuilder);
    }

    public static XmlNodeImpl build(XMLStreamReader parser, boolean trim, InputLocationBuilderStax locationBuilder)
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
                    XmlNode child = build(parser, trim, locationBuilder);
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
                return new XmlNodeImpl(
                        lPrefix,
                        lNamespaceUri,
                        lName,
                        children == null ? (lValue != null ? lValue : emptyTag ? null : "") : null,
                        attrs,
                        children,
                        location);
            }
            eventType = parser.next();
        }
        throw new IllegalStateException("End of document found before returning to 0 depth");
    }

    /**
     * Input location builder interface, to be implemented to choose how to store data.
     *
     * @since 3.2.0
     */
    public interface InputLocationBuilderStax {
        Object toInputLocation(XMLStreamReader parser);
    }
}
