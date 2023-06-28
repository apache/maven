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

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.regex.Pattern;

import org.codehaus.stax2.XMLStreamReader2;

public class BufferingParser implements XMLStreamReader {

    private static final Pattern WHITESPACE_REGEX = Pattern.compile("[ \r\t\n]+");

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

    protected XMLStreamReader delegate;
    protected Deque<Event> events;
    protected Event current;
    protected boolean bypass;

    @SuppressWarnings("checkstyle:VisibilityModifier")
    public static class Event {
        public int event;
        public String name;
        public String prefix;
        public String namespace;
        public boolean empty;
        public String text;
        public Attribute[] attributes;
        public Namespace[] namespaces;
        public int columnNumber;
        public int lineNumber;

        public String positionDescription() {
            return " " + TYPES[event] + " @" + lineNumber + ":" + columnNumber;
        }

        @Override
        public String toString() {
            switch (event) {
                case START_DOCUMENT:
                case END_DOCUMENT:
                    return "Event{event=" + TYPES[event] + "'}";
                case PROCESSING_INSTRUCTION:
                case CHARACTERS:
                case CDATA:
                case ENTITY_REFERENCE:
                case COMMENT:
                case SPACE:
                    return "Event{event=" + TYPES[event] + ", text='" + text + "'}";
                case START_ELEMENT:
                    return "Event{" + "event=START_TAG"
                            + ", name='"
                            + name + '\'' + ", prefix='"
                            + prefix + '\'' + ", namespace='"
                            + namespace + '\'' + ", empty="
                            + empty + ", attributes="
                            + Arrays.toString(attributes) + ", namespaces="
                            + Arrays.toString(namespaces) + '}';
                case END_ELEMENT:
                    return "Event{" + "event=END_TAG"
                            + ", name='"
                            + name + '\'' + ", prefix='"
                            + prefix + '\'' + ", namespace='"
                            + namespace + '\'' + ", empty="
                            + empty + ", namespaces="
                            + Arrays.toString(namespaces) + '}';
                default:
                    return "Event{" + "event="
                            + TYPES[event] + ", name='"
                            + name + '\'' + ", prefix='"
                            + prefix + '\'' + ", namespace='"
                            + namespace + '\'' + ", empty="
                            + empty + ", text='"
                            + text + '\'' + ", attributes="
                            + Arrays.toString(attributes) + ", namespaces="
                            + Arrays.toString(namespaces) + '}';
            }
        }
    }

    @SuppressWarnings("checkstyle:VisibilityModifier")
    public static class Namespace {
        public String prefix;
        public String uri;
    }

    @SuppressWarnings("checkstyle:VisibilityModifier")
    public static class Attribute {
        public String name;
        public String prefix;
        public String namespace;
        public String type;
        public String value;
    }

    public BufferingParser(XMLStreamReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return delegate.getElementText();
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return delegate.hasNext();
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return delegate.getNamespaceURI(prefix);
    }

    @Override
    public boolean isStartElement() {
        return delegate.isStartElement();
    }

    @Override
    public boolean isEndElement() {
        return delegate.isEndElement();
    }

    @Override
    public boolean isCharacters() {
        return delegate.isCharacters();
    }

    @Override
    public boolean isWhiteSpace() {
        return delegate.isWhiteSpace();
    }

    @Override
    public QName getAttributeName(int index) {
        return delegate.getAttributeName(index);
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return delegate.isAttributeSpecified(index);
    }

    @Override
    public int getNamespaceCount() {
        return current != null
                ? current.namespaces != null ? current.namespaces.length : 0
                : delegate.getNamespaceCount();
    }

    @Override
    public String getNamespacePrefix(int index) {
        return current != null ? current.namespaces[index].prefix : delegate.getNamespacePrefix(index);
    }

    @Override
    public String getNamespaceURI(int index) {
        return current != null ? current.namespaces[index].uri : delegate.getNamespaceURI(index);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public char[] getTextCharacters() {
        return delegate.getTextCharacters();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException {
        return delegate.getTextCharacters(sourceStart, target, targetStart, length);
    }

    @Override
    public int getTextStart() {
        return delegate.getTextStart();
    }

    @Override
    public int getTextLength() {
        return delegate.getTextLength();
    }

    @Override
    public String getEncoding() {
        return delegate.getEncoding();
    }

    @Override
    public boolean hasText() {
        return delegate.hasText();
    }

    @Override
    public Location getLocation() {
        return delegate.getLocation();
    }

    @Override
    public QName getName() {
        return delegate.getName();
    }

    @Override
    public boolean hasName() {
        return delegate.hasName();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public boolean isStandalone() {
        return delegate.isStandalone();
    }

    @Override
    public boolean standaloneSet() {
        return delegate.standaloneSet();
    }

    @Override
    public String getCharacterEncodingScheme() {
        return delegate.getCharacterEncodingScheme();
    }

    @Override
    public String getPITarget() {
        return delegate.getPITarget();
    }

    @Override
    public String getPIData() {
        return delegate.getPIData();
    }

    @Override
    public String getText() {
        return current != null ? current.text : delegate.getText();
    }

    @Override
    public String getNamespaceURI() {
        return current != null ? current.namespace : delegate.getNamespaceURI();
    }

    @Override
    public String getLocalName() {
        return current != null ? current.name : delegate.getLocalName();
    }

    @Override
    public String getPrefix() {
        return current != null ? current.prefix : delegate.getPrefix();
    }

    @Override
    public int getAttributeCount() {
        if (current != null) {
            return current.attributes != null ? current.attributes.length : 0;
        } else {
            return delegate.getAttributeCount();
        }
    }

    @Override
    public String getAttributeNamespace(int index) {
        if (current != null) {
            return current.attributes[index].namespace;
        } else {
            return delegate.getAttributeNamespace(index);
        }
    }

    @Override
    public String getAttributeLocalName(int index) {
        if (current != null) {
            return current.attributes[index].name;
        } else {
            return delegate.getAttributeLocalName(index);
        }
    }

    @Override
    public String getAttributePrefix(int index) {
        if (current != null) {
            return current.attributes[index].prefix;
        } else {
            return delegate.getAttributePrefix(index);
        }
    }

    @Override
    public String getAttributeType(int index) {
        if (current != null) {
            return current.attributes[index].type;
        } else {
            return delegate.getAttributeType(index);
        }
    }

    @Override
    public String getAttributeValue(int index) {
        if (current != null) {
            return current.attributes[index].value;
        } else {
            return delegate.getAttributeValue(index);
        }
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        if (current != null) {
            if (current.attributes != null) {
                for (Attribute attr : current.attributes) {
                    if (Objects.equals(namespace, attr.namespace) && Objects.equals(name, attr.name)) {
                        return attr.value;
                    }
                }
            }
            return null;
        } else {
            return delegate.getAttributeValue(namespace, name);
        }
    }

    @Override
    public void require(int type, String namespace, String name) throws XMLStreamException {
        if (current != null) {
            throw new IllegalStateException("Not supported during events replay");
        }
        delegate.require(type, namespace, name);
    }

    @Override
    public int getEventType() {
        return current != null ? current.event : delegate.getEventType();
    }

    @Override
    public int next() throws XMLStreamException {
        while (true) {
            if (events != null && !events.isEmpty()) {
                current = events.removeFirst();
                return current.event;
            } else {
                current = null;
            }
            if (getEventType() == END_DOCUMENT) {
                throw new XMLStreamException("already reached end of XML input", getLocation(), null);
            }
            int currentEvent = delegate.next();
            if (bypass() || accept()) {
                return currentEvent;
            }
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
                    "expected START_TAG or END_TAG not " + TYPES[getEventType()], getLocation(), null);
        }
        return eventType;
    }

    public boolean isWhitespace() throws XMLStreamException {
        if (getEventType() == CHARACTERS || getEventType() == CDATA) {
            return WHITESPACE_REGEX.matcher(getText()).matches();
        } else if (getEventType() == SPACE) {
            return true;
        } else {
            throw new XMLStreamException("no content available to check for whitespaces");
        }
    }

    protected Event bufferEvent() throws XMLStreamException {
        Event event = new Event();
        XMLStreamReader pp = delegate;
        event.event = delegate.getEventType();
        event.columnNumber = delegate.getLocation().getColumnNumber();
        event.lineNumber = delegate.getLocation().getLineNumber();
        switch (event.event) {
            case START_DOCUMENT:
            case END_DOCUMENT:
                break;
            case START_ELEMENT:
                event.name = pp.getLocalName();
                event.namespace = pp.getNamespaceURI();
                event.prefix = pp.getPrefix();
                event.empty = (pp instanceof XMLStreamReader2) && ((XMLStreamReader2) pp).isEmptyElement();
                // event.text = pp.getText();
                event.attributes = new Attribute[pp.getAttributeCount()];
                for (int i = 0; i < pp.getAttributeCount(); i++) {
                    Attribute attr = new Attribute();
                    attr.name = pp.getAttributeLocalName(i);
                    attr.namespace = pp.getAttributeNamespace(i);
                    attr.value = pp.getAttributeValue(i);
                    attr.type = pp.getAttributeType(i);
                    event.attributes[i] = attr;
                }
                event.namespaces = new Namespace[pp.getNamespaceCount()];
                for (int i = 0; i < pp.getNamespaceCount(); i++) {
                    Namespace ns = new Namespace();
                    ns.uri = pp.getNamespaceURI(i);
                    ns.prefix = pp.getNamespacePrefix(i);
                    event.namespaces[i] = ns;
                }
                break;
            case END_ELEMENT:
                event.name = pp.getLocalName();
                event.namespace = pp.getNamespaceURI();
                event.prefix = pp.getPrefix();
                // event.text = pp.getText();
                break;
            case CHARACTERS:
            case COMMENT:
            case SPACE:
            case CDATA:
            case ENTITY_REFERENCE:
                event.text = pp.getText();
                break;
            default:
                break;
        }
        return event;
    }

    protected void pushEvent(Event event) {
        if (events == null) {
            events = new ArrayDeque<>();
        }
        events.add(event);
    }

    protected boolean accept() throws XMLStreamException {
        return true;
    }

    public void bypass(boolean bypass) {
        if (bypass && events != null && !events.isEmpty()) {
            throw new IllegalStateException("Can not disable filter while processing");
        }
        this.bypass = bypass;
    }

    public boolean bypass() {
        return bypass || (delegate instanceof BufferingParser && ((BufferingParser) delegate).bypass());
    }

    protected static String nullSafeAppend(String originalValue, String charSegment) {
        if (originalValue == null) {
            return charSegment;
        } else {
            return originalValue + charSegment;
        }
    }
}
