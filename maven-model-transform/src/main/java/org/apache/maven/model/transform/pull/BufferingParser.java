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
package org.apache.maven.model.transform.pull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * An xml pull parser filter base implementation.
 *
 * @author Guillaume Nodet
 * @since 4.0.0
 */
public class BufferingParser implements XmlPullParser {

    private static final Pattern WHITESPACE_REGEX = Pattern.compile("[ \r\t\n]+");

    protected XmlPullParser xmlPullParser;
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
        public boolean isDefault;
    }

    public BufferingParser(XmlPullParser xmlPullParser) {
        this.xmlPullParser = xmlPullParser;
    }

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        xmlPullParser.setFeature(name, state);
    }

    @Override
    public boolean getFeature(String name) {
        return xmlPullParser.getFeature(name);
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        xmlPullParser.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) {
        return xmlPullParser.getProperty(name);
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        xmlPullParser.setInput(in);
    }

    @Override
    public void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException {
        xmlPullParser.setInput(inputStream, inputEncoding);
    }

    @Override
    public String getInputEncoding() {
        return xmlPullParser.getInputEncoding();
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException {
        xmlPullParser.defineEntityReplacementText(entityName, replacementText);
    }

    @Override
    public int getNamespaceCount(int depth) throws XmlPullParserException {
        //  TODO:      if (current != null) throw new IllegalStateException("Not supported during events replay");
        return xmlPullParser.getNamespaceCount(depth);
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        //  TODO:      if (current != null) throw new IllegalStateException("Not supported during events replay");
        return xmlPullParser.getNamespacePrefix(pos);
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        //  TODO:      if (current != null) throw new IllegalStateException("Not supported during events replay");
        return xmlPullParser.getNamespaceUri(pos);
    }

    @Override
    public String getNamespace(String prefix) {
        //  TODO:      if (current != null) throw new IllegalStateException("Not supported during events replay");
        return xmlPullParser.getNamespace(prefix);
    }

    @Override
    public int getDepth() {
        //  TODO:      if (current != null) throw new IllegalStateException("Not supported during events replay");
        return xmlPullParser.getDepth();
    }

    @Override
    public String getPositionDescription() {
        if (current != null) {
            throw new IllegalStateException("Not supported during events replay");
        }
        return xmlPullParser.getPositionDescription();
    }

    @Override
    public int getLineNumber() {
        if (current != null) {
            throw new IllegalStateException("Not supported during events replay");
        }
        return xmlPullParser.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        if (current != null) {
            throw new IllegalStateException("Not supported during events replay");
        }
        return xmlPullParser.getColumnNumber();
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        if (current != null) {
            if (current.event == TEXT || current.event == CDSECT) {
                return WHITESPACE_REGEX.matcher(current.text).matches();
            } else if (current.event == IGNORABLE_WHITESPACE) {
                return true;
            } else {
                throw new XmlPullParserException("no content available to check for whitespaces");
            }
        }
        return xmlPullParser.isWhitespace();
    }

    @Override
    public String getText() {
        return current != null ? current.text : xmlPullParser.getText();
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) {
        if (current != null) {
            throw new IllegalStateException("Not supported during events replay");
        }
        return xmlPullParser.getTextCharacters(holderForStartAndLength);
    }

    @Override
    public String getNamespace() {
        return current != null ? current.namespace : xmlPullParser.getNamespace();
    }

    @Override
    public String getName() {
        return current != null ? current.name : xmlPullParser.getName();
    }

    @Override
    public String getPrefix() {
        return current != null ? current.prefix : xmlPullParser.getPrefix();
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        return current != null ? current.empty : xmlPullParser.isEmptyElementTag();
    }

    @Override
    public int getAttributeCount() {
        if (current != null) {
            return current.attributes != null ? current.attributes.length : 0;
        } else {
            return xmlPullParser.getAttributeCount();
        }
    }

    @Override
    public String getAttributeNamespace(int index) {
        if (current != null) {
            return current.attributes[index].namespace;
        } else {
            return xmlPullParser.getAttributeNamespace(index);
        }
    }

    @Override
    public String getAttributeName(int index) {
        if (current != null) {
            return current.attributes[index].name;
        } else {
            return xmlPullParser.getAttributeName(index);
        }
    }

    @Override
    public String getAttributePrefix(int index) {
        if (current != null) {
            return current.attributes[index].prefix;
        } else {
            return xmlPullParser.getAttributePrefix(index);
        }
    }

    @Override
    public String getAttributeType(int index) {
        if (current != null) {
            return current.attributes[index].type;
        } else {
            return xmlPullParser.getAttributeType(index);
        }
    }

    @Override
    public boolean isAttributeDefault(int index) {
        if (current != null) {
            return current.attributes[index].isDefault;
        } else {
            return xmlPullParser.isAttributeDefault(index);
        }
    }

    @Override
    public String getAttributeValue(int index) {
        if (current != null) {
            return current.attributes[index].value;
        } else {
            return xmlPullParser.getAttributeValue(index);
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
            return xmlPullParser.getAttributeValue(namespace, name);
        }
    }

    @Override
    public void require(int type, String namespace, String name) throws XmlPullParserException, IOException {
        if (current != null) {
            throw new IllegalStateException("Not supported during events replay");
        }
        xmlPullParser.require(type, namespace, name);
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        return current != null ? current.event : xmlPullParser.getEventType();
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        while (true) {
            if (events != null && !events.isEmpty()) {
                current = events.removeFirst();
                return current.event;
            } else {
                current = null;
            }
            if (getEventType() == END_DOCUMENT) {
                throw new XmlPullParserException("already reached end of XML input", this, null);
            }
            int currentEvent = xmlPullParser.next();
            if (bypass() || accept()) {
                return currentEvent;
            }
        }
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        while (true) {
            if (events != null && !events.isEmpty()) {
                current = events.removeFirst();
                return current.event;
            } else {
                current = null;
            }
            if (getEventType() == END_DOCUMENT) {
                throw new XmlPullParserException("already reached end of XML input", this, null);
            }
            int currentEvent = xmlPullParser.nextToken();
            if (bypass() || accept()) {
                return currentEvent;
            }
        }
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        int eventType = next();
        if (eventType == TEXT && isWhitespace()) { // skip whitespace
            eventType = next();
        }
        if (eventType != START_TAG && eventType != END_TAG) {
            throw new XmlPullParserException("expected START_TAG or END_TAG not " + TYPES[getEventType()], this, null);
        }
        return eventType;
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        int eventType = getEventType();
        if (eventType != START_TAG) {
            throw new XmlPullParserException("parser must be on START_TAG to read next text", this, null);
        }
        eventType = next();
        if (eventType == TEXT) {
            final String result = getText();
            eventType = next();
            if (eventType != END_TAG) {
                throw new XmlPullParserException(
                        "TEXT must be immediately followed by END_TAG and not " + TYPES[getEventType()], this, null);
            }
            return result;
        } else if (eventType == END_TAG) {
            return "";
        } else {
            throw new XmlPullParserException("parser must be on START_TAG or TEXT to read text", this, null);
        }
    }

    protected Event bufferEvent() throws XmlPullParserException {
        Event event = new Event();
        XmlPullParser pp = xmlPullParser;
        event.event = xmlPullParser.getEventType();
        switch (event.event) {
            case START_DOCUMENT:
            case END_DOCUMENT:
                break;
            case START_TAG:
                event.name = pp.getName();
                event.namespace = pp.getNamespace();
                event.prefix = pp.getPrefix();
                event.empty = pp.isEmptyElementTag();
                event.text = pp.getText();
                event.attributes = new Attribute[pp.getAttributeCount()];
                for (int i = 0; i < pp.getAttributeCount(); i++) {
                    Attribute attr = new Attribute();
                    attr.name = pp.getAttributeName(i);
                    attr.namespace = pp.getAttributeNamespace(i);
                    attr.value = pp.getAttributeValue(i);
                    attr.type = pp.getAttributeType(i);
                    attr.isDefault = pp.isAttributeDefault(i);
                    event.attributes[i] = attr;
                }
                break;
            case END_TAG:
                event.name = pp.getName();
                event.namespace = pp.getNamespace();
                event.prefix = pp.getPrefix();
                event.text = pp.getText();
                break;
            case TEXT:
            case COMMENT:
            case IGNORABLE_WHITESPACE:
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

    protected boolean accept() throws XmlPullParserException, IOException {
        return true;
    }

    public void bypass(boolean bypass) {
        if (bypass && events != null && !events.isEmpty()) {
            throw new IllegalStateException("Can not disable filter while processing");
        }
        this.bypass = bypass;
    }

    public boolean bypass() {
        return bypass || (xmlPullParser instanceof BufferingParser && ((BufferingParser) xmlPullParser).bypass());
    }

    protected static String nullSafeAppend(String originalValue, String charSegment) {
        if (originalValue == null) {
            return charSegment;
        } else {
            return originalValue + charSegment;
        }
    }
}
