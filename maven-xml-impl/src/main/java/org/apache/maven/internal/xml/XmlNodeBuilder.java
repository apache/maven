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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.xml.XmlNode;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 */
public class XmlNodeBuilder {
    private static final boolean DEFAULT_TRIM = true;

    public static XmlNodeImpl build(Reader reader) throws XmlPullParserException, IOException {
        return build(reader, null);
    }

    /**
     * @param reader the reader
     * @param locationBuilder the builder
     * @since 3.2.0
     * @return DOM
     * @throws XmlPullParserException xml exception
     * @throws IOException io
     */
    public static XmlNodeImpl build(Reader reader, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        return build(reader, DEFAULT_TRIM, locationBuilder);
    }

    public static XmlNodeImpl build(InputStream is, String encoding) throws XmlPullParserException, IOException {
        return build(is, encoding, DEFAULT_TRIM);
    }

    public static XmlNodeImpl build(InputStream is, String encoding, boolean trim)
            throws XmlPullParserException, IOException {
        try (InputStream closeMe = is) {
            final XmlPullParser parser = new MXParser();
            parser.setInput(is, encoding);

            final XmlNodeImpl node = build(parser, trim);
            return node;
        }
    }

    public static XmlNodeImpl build(Reader reader, boolean trim) throws XmlPullParserException, IOException {
        return build(reader, trim, null);
    }

    /**
     * @param reader the reader
     * @param trim to trim
     * @param locationBuilder the builder
     * @since 3.2.0
     * @return DOM
     * @throws XmlPullParserException xml exception
     * @throws IOException io
     */
    public static XmlNodeImpl build(Reader reader, boolean trim, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        try (Reader closeMe = reader) {
            final XmlPullParser parser = new MXParser();
            parser.setInput(reader);

            final XmlNodeImpl node = build(parser, trim, locationBuilder);

            return node;
        }
    }

    public static XmlNodeImpl build(XmlPullParser parser) throws XmlPullParserException, IOException {
        return build(parser, DEFAULT_TRIM);
    }

    public static XmlNodeImpl build(XmlPullParser parser, boolean trim) throws XmlPullParserException, IOException {
        return build(parser, trim, null);
    }

    /**
     * @since 3.2.0
     * @param locationBuilder builder
     * @param parser the parser
     * @param trim do trim
     * @return DOM
     * @throws XmlPullParserException xml exception
     * @throws IOException io
     */
    public static XmlNodeImpl build(XmlPullParser parser, boolean trim, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        boolean spacePreserve = false;
        String name = null;
        String value = null;
        Object location = null;
        Map<String, String> attrs = null;
        List<XmlNode> children = null;
        int eventType = parser.getEventType();
        boolean emptyTag = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                emptyTag = parser.isEmptyElementTag();
                if (name == null) {
                    name = parser.getName();
                    location = locationBuilder != null ? locationBuilder.toInputLocation(parser) : null;
                    int attributesSize = parser.getAttributeCount();
                    if (attributesSize > 0) {
                        attrs = new HashMap<>();
                        for (int i = 0; i < attributesSize; i++) {
                            String aname = parser.getAttributeName(i);
                            String avalue = parser.getAttributeValue(i);
                            attrs.put(aname, avalue);
                            spacePreserve = spacePreserve || ("xml:space".equals(aname) && "preserve".equals(avalue));
                        }
                    }
                } else {
                    if (children == null) {
                        children = new ArrayList<>();
                    }
                    XmlNode child = build(parser, trim, locationBuilder);
                    children.add(child);
                }
            } else if (eventType == XmlPullParser.TEXT) {
                String text = parser.getText();
                if (trim && !spacePreserve) {
                    text = text.trim();
                }
                value = value != null ? value + text : text;
            } else if (eventType == XmlPullParser.END_TAG) {
                return new XmlNodeImpl(
                        name,
                        children == null ? (value != null ? value : emptyTag ? null : "") : null,
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
    public interface InputLocationBuilder {
        Object toInputLocation(XmlPullParser parser);
    }
}
