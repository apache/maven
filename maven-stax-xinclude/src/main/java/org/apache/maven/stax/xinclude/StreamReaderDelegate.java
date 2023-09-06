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
package org.apache.maven.stax.xinclude;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.regex.Pattern;

/**
 * A {@code XMLStreamReader} delegating to the {@link #getDelegate()} method.
 * <p>
 * This class is similar to {@link javax.xml.stream.util.StreamReaderDelegate} though
 * it differs in using a method to delegate to instead of a field.  This is needed as
 * the {@link XIncludeStreamReader} needs to pick up the reader to delegate to from
 * a stack.
 *
 * @see XIncludeStreamReader#getDelegate()
 */
abstract class StreamReaderDelegate implements XMLStreamReader {

    protected abstract XMLStreamReader getDelegate();

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return getDelegate().getProperty(name);
    }

    @Override
    public int next() throws XMLStreamException {
        return getDelegate().next();
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        getDelegate().require(type, namespaceURI, localName);
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return getDelegate().getElementText();
    }

    private static final String[] TYPES = new String[] {
        "",
        "START_ELEMENT",
        "END_ELEMENT",
        "PROCESSING_INSTRUCTION",
        "CHARACTERS",
        "COMMENT",
        "SPACE",
        "START_DOCUMENT",
        "END_DOCUMENT",
        "ENTITY_REFERENCE",
        "ATTRIBUTE",
        "DTD",
        "CDATA",
        "NAMESPACE",
        "NOTATION_DECLARATION",
        "ENTITY_DECLARATION"
    };

    private static final Pattern WHITESPACE_REGEX = Pattern.compile("[ \r\t\n]+");

    public boolean isWhitespace() throws XMLStreamException {
        if (getEventType() == CHARACTERS || getEventType() == CDATA) {
            return WHITESPACE_REGEX.matcher(getText()).matches();
        } else if (getEventType() == SPACE) {
            return true;
        } else {
            throw new XMLStreamException("no content available to check for whitespaces");
        }
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int eventType = next();
        while (eventType == CHARACTERS && isWhitespace() // skip whitespace
                || eventType == COMMENT) { // skip comments
            eventType = next();
        }
        if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
            throw new XMLStreamException(
                    "expected START_TAG or END_TAG, not " + TYPES[getEventType()], getLocation(), null);
        }
        return eventType;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return getDelegate().hasNext();
    }

    @Override
    public void close() throws XMLStreamException {
        getDelegate().close();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return getDelegate().getNamespaceURI(prefix);
    }

    @Override
    public boolean isStartElement() {
        return getDelegate().isStartElement();
    }

    @Override
    public boolean isEndElement() {
        return getDelegate().isEndElement();
    }

    @Override
    public boolean isCharacters() {
        return getDelegate().isCharacters();
    }

    @Override
    public boolean isWhiteSpace() {
        return getDelegate().isWhiteSpace();
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return getDelegate().getAttributeValue(namespaceURI, localName);
    }

    @Override
    public int getAttributeCount() {
        return getDelegate().getAttributeCount();
    }

    @Override
    public QName getAttributeName(int index) {
        return getDelegate().getAttributeName(index);
    }

    @Override
    public String getAttributeNamespace(int index) {
        return getDelegate().getAttributeNamespace(index);
    }

    @Override
    public String getAttributeLocalName(int index) {
        return getDelegate().getAttributeLocalName(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        return getDelegate().getAttributePrefix(index);
    }

    @Override
    public String getAttributeType(int index) {
        return getDelegate().getAttributeType(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return getDelegate().getAttributeValue(index);
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return getDelegate().isAttributeSpecified(index);
    }

    @Override
    public int getNamespaceCount() {
        return getDelegate().getNamespaceCount();
    }

    @Override
    public String getNamespacePrefix(int index) {
        return getDelegate().getNamespacePrefix(index);
    }

    @Override
    public String getNamespaceURI(int index) {
        return getDelegate().getNamespaceURI(index);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return getDelegate().getNamespaceContext();
    }

    @Override
    public int getEventType() {
        return getDelegate().getEventType();
    }

    @Override
    public String getText() {
        return getDelegate().getText();
    }

    @Override
    public char[] getTextCharacters() {
        return getDelegate().getTextCharacters();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException {
        return getDelegate().getTextCharacters(sourceStart, target, targetStart, length);
    }

    @Override
    public int getTextStart() {
        return getDelegate().getTextStart();
    }

    @Override
    public int getTextLength() {
        return getDelegate().getTextLength();
    }

    @Override
    public String getEncoding() {
        return getDelegate().getEncoding();
    }

    @Override
    public boolean hasText() {
        return getDelegate().hasText();
    }

    @Override
    public Location getLocation() {
        return getDelegate().getLocation();
    }

    @Override
    public QName getName() {
        return getDelegate().getName();
    }

    @Override
    public String getLocalName() {
        return getDelegate().getLocalName();
    }

    @Override
    public boolean hasName() {
        return getDelegate().hasName();
    }

    @Override
    public String getNamespaceURI() {
        return getDelegate().getNamespaceURI();
    }

    @Override
    public String getPrefix() {
        return getDelegate().getPrefix();
    }

    @Override
    public String getVersion() {
        return getDelegate().getVersion();
    }

    @Override
    public boolean isStandalone() {
        return getDelegate().isStandalone();
    }

    @Override
    public boolean standaloneSet() {
        return getDelegate().standaloneSet();
    }

    @Override
    public String getCharacterEncodingScheme() {
        return getDelegate().getCharacterEncodingScheme();
    }

    @Override
    public String getPITarget() {
        return getDelegate().getPITarget();
    }

    @Override
    public String getPIData() {
        return getDelegate().getPIData();
    }
}
