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
package org.apache.maven.model.transform.stax;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import com.ctc.wstx.api.WstxOutputProperties;

public class XmlUtils {

    public static InputStream writeDocument(XMLStreamReader parser) throws XMLStreamException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeDocument(parser, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public static void writeDocument(XMLStreamReader parser, OutputStream output) throws XMLStreamException {
        XMLOutputFactory factory = new com.ctc.wstx.stax.WstxOutputFactory();
        factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
        factory.setProperty(WstxOutputProperties.P_USE_DOUBLE_QUOTES_IN_XML_DECL, true);
        factory.setProperty(WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM, true);
        XMLStreamWriter serializer = factory.createXMLStreamWriter(output, parser.getCharacterEncodingScheme());
        copy(parser, serializer);
    }

    private static String normalize(String input) {
        if (input != null) {
            return input.replace("\r\n", "\n");
        }
        return input;
    }

    /**
     * Copies the reader to the writer. The start and end document methods must
     * be handled on the writer manually.
     */
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        copy(reader, writer, false, false);
    }

    public static void copy(XMLStreamReader reader, XMLStreamWriter writer, boolean fragment)
            throws XMLStreamException {
        copy(reader, writer, fragment, false);
    }

    public static void copy(XMLStreamReader reader, XMLStreamWriter writer, boolean fragment, boolean isThreshold)
            throws XMLStreamException {
        // number of elements read in
        int read = 0;
        int elementCount = 0;
        final Deque<Integer> countStack = new ArrayDeque<>();
        int event = reader.getEventType();

        while (true) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    read++;
                    if (isThreshold) {
                        elementCount++;
                        countStack.push(elementCount);
                        elementCount = 0;
                    }
                    writeStartElement(reader, writer);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (read > 0) {
                        writer.writeEndElement();
                    }
                    read--;
                    if (read < 0 && fragment) {
                        return;
                    }
                    if (isThreshold && !countStack.isEmpty()) {
                        elementCount = countStack.pop();
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters(normalize(reader.getText()));
                    break;
                case XMLStreamConstants.SPACE:
                    writer.writeCharacters(normalize(reader.getText()));
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    writer.writeEntityRef(reader.getLocalName());
                    break;
                case XMLStreamConstants.COMMENT:
                    writer.writeComment(normalize(reader.getText()));
                    break;
                case XMLStreamConstants.CDATA:
                    writer.writeCData(normalize(reader.getText()));
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    if (reader.getVersion() != null) {
                        writer.writeStartDocument(reader.getCharacterEncodingScheme(), reader.getVersion());
                    }
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    writer.writeEndDocument();
                    return;
                default:
                    break;
            }
            event = reader.next();
        }
    }

    private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        String uri = reader.getNamespaceURI();
        String prefix = reader.getPrefix();
        String local = reader.getLocalName();

        if (prefix == null) {
            prefix = "";
        }

        // Write out the element name
        if (uri != null) {
            if (prefix.isEmpty() && isEmpty(uri)) {
                writer.writeStartElement(local);
            } else {
                writer.writeStartElement(prefix, local, uri);
            }
        } else {
            writer.writeStartElement(local);
        }

        // Write out the namespaces
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String nsURI = reader.getNamespaceURI(i);
            String nsPrefix = reader.getNamespacePrefix(i);
            if (nsURI == null) {
                nsURI = "";
            }
            writer.writeNamespace(nsPrefix, nsURI);
            writer.setPrefix(nsPrefix, nsURI);
        }

        // Write out attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String ns = reader.getAttributeNamespace(i);
            String nsPrefix = reader.getAttributePrefix(i);
            if (ns == null || ns.isEmpty()) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else if (nsPrefix == null || nsPrefix.isEmpty()) {
                writer.writeAttribute(
                        reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(
                        reader.getAttributePrefix(i),
                        reader.getAttributeNamespace(i),
                        reader.getAttributeLocalName(i),
                        reader.getAttributeValue(i));
            }
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
