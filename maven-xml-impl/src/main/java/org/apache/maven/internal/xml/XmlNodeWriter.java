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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.Writer;
import java.util.Map;

import org.apache.maven.api.xml.XmlNode;
import org.codehaus.stax2.util.StreamWriterDelegate;

/**
 *
 */
public class XmlNodeWriter {
    public static void write(Writer writer, XmlNode dom) throws XMLStreamException {
        XMLOutputFactory factory = new com.ctc.wstx.stax.WstxOutputFactory();
        factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
        factory.setProperty(com.ctc.wstx.api.WstxOutputProperties.P_USE_DOUBLE_QUOTES_IN_XML_DECL, true);
        factory.setProperty(com.ctc.wstx.api.WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM, true);
        XMLStreamWriter serializer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(writer));
        write(serializer, dom);
        serializer.close();
    }

    public static void write(XMLStreamWriter xmlWriter, XmlNode dom) throws XMLStreamException {
        xmlWriter.writeStartElement(dom.getPrefix(), dom.getName(), dom.getNamespaceUri());
        for (Map.Entry<String, String> attr : dom.getAttributes().entrySet()) {
            xmlWriter.writeAttribute(attr.getKey(), attr.getValue());
        }
        for (XmlNode aChildren : dom.getChildren()) {
            write(xmlWriter, aChildren);
        }
        String value = dom.getValue();
        if (value != null) {
            xmlWriter.writeCharacters(value);
        }
        xmlWriter.writeEndElement();
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
