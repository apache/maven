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
 * Builds an {@link XmlNodeImpl} DOM tree from XML input using an {@link XmlPullParser}.
 *
 * <p>All {@code build} methods in this class parse a single XML document element (including
 * its children) from the input and return it as an {@link XmlNodeImpl}. The caller is
 * responsible for closing any {@link InputStream} or {@link Reader} passed to these methods.</p>
 *
 * <h2>Multi-document stream reading</h2>
 *
 * <p>When the underlying {@link Reader} or {@link InputStream} contains multiple concatenated
 * XML documents, each call to {@code build} consumes exactly one root element and its children.
 * The stream position is left immediately after the closing tag of that element, so a subsequent
 * call to {@code build} with a new parser wrapping the same reader will parse the next document.
 * For example, given a reader over the concatenation of two identical documents:</p>
 *
 * <pre>
 *   String doc = "&lt;?xml version='1.0'?&gt;&lt;doc&gt;&lt;child&gt;foo&lt;/child&gt;&lt;/doc&gt;";
 *   Reader r = new StringReader(doc + doc);
 *   XmlNode first  = XmlService.read(r);   // reads the first &lt;doc&gt;
 *   XmlNode second = XmlService.read(r);   // reads the second &lt;doc&gt;
 *   // first.equals(second) is true
 * </pre>
 *
 * <h2>Whitespace trimming</h2>
 *
 * <p>By default, text content is trimmed of leading and trailing whitespace. This can be
 * disabled by passing {@code trim = false}, or on a per-element basis by setting the
 * {@code xml:space="preserve"} attribute on an element.</p>
 *
 * <h2>Empty vs. self-closing elements</h2>
 *
 * <p>Self-closing tags (e.g. {@code <item/>}) produce a node whose value is {@code null}.
 * An element with an explicit open and close tag but no content (e.g. {@code <item></item>})
 * produces a node whose value is the empty string {@code ""}.</p>
 *
 * <h2>Child elements vs. text content</h2>
 *
 * <p>If an element contains child elements, the resulting node carries the children and its
 * text value is {@code null}, even if there is interleaved text content. If an element
 * contains only text (no child elements), the node carries the text value and has no
 * children.</p>
 *
 * @deprecated Use {@link org.apache.maven.api.xml.XmlService} instead.
 */
@Deprecated
public class XmlNodeBuilder {
    private static final boolean DEFAULT_TRIM = true;

    /**
     * Builds an XML node tree from the given reader, trimming whitespace by default and
     * without tracking input locations.
     *
     * @param reader the reader to parse XML from
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     */
    public static XmlNodeImpl build(Reader reader) throws XmlPullParserException, IOException {
        return build(reader, (InputLocationBuilder) null);
    }

    /**
     * Builds an XML node tree from the given reader, trimming whitespace by default.
     *
     * @param reader the reader to parse XML from
     * @param locationBuilder optional builder for recording input locations of parsed elements,
     *                        or {@code null} to skip location tracking
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     * @since 3.2.0
     */
    public static XmlNodeImpl build(Reader reader, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        return build(reader, DEFAULT_TRIM, locationBuilder);
    }

    /**
     * Builds an XML node tree from the given input stream, trimming whitespace by default.
     *
     * @param is the input stream to parse XML from
     * @param encoding the character encoding of the stream (e.g. {@code "UTF-8"}),
     *                 or {@code null} to let the parser detect it
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     */
    public static XmlNodeImpl build(InputStream is, String encoding) throws XmlPullParserException, IOException {
        return build(is, encoding, DEFAULT_TRIM);
    }

    /**
     * Builds an XML node tree from the given input stream.
     *
     * @param is the input stream to parse XML from
     * @param encoding the character encoding of the stream (e.g. {@code "UTF-8"}),
     *                 or {@code null} to let the parser detect it
     * @param trim if {@code true}, leading and trailing whitespace is removed from text
     *             content unless the element has {@code xml:space="preserve"}
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     */
    public static XmlNodeImpl build(InputStream is, String encoding, boolean trim)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = new MXParser();
        parser.setInput(is, encoding);
        return build(parser, trim);
    }

    /**
     * Builds an XML node tree from the given reader without location tracking.
     *
     * @param reader the reader to parse XML from
     * @param trim if {@code true}, leading and trailing whitespace is removed from text
     *             content unless the element has {@code xml:space="preserve"}
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     */
    public static XmlNodeImpl build(Reader reader, boolean trim) throws XmlPullParserException, IOException {
        return build(reader, trim, null);
    }

    /**
     * Builds an XML node tree from the given reader.
     *
     * @param reader the reader to parse XML from
     * @param trim if {@code true}, leading and trailing whitespace is removed from text
     *             content unless the element has {@code xml:space="preserve"}
     * @param locationBuilder optional builder for recording input locations of parsed elements,
     *                        or {@code null} to skip location tracking
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     * @since 3.2.0
     */
    public static XmlNodeImpl build(Reader reader, boolean trim, InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = new MXParser();
        parser.setInput(reader);
        return build(parser, trim, locationBuilder);
    }

    /**
     * Builds an XML node tree from the given pull parser, trimming whitespace by default and
     * without tracking input locations.
     *
     * @param parser the pull parser positioned at or before the root element's start tag
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     */
    public static XmlNodeImpl build(XmlPullParser parser) throws XmlPullParserException, IOException {
        return build(parser, DEFAULT_TRIM);
    }

    /**
     * Builds an XML node tree from the given pull parser without location tracking.
     *
     * @param parser the pull parser positioned at or before the root element's start tag
     * @param trim if {@code true}, leading and trailing whitespace is removed from text
     *             content unless the element has {@code xml:space="preserve"}
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     */
    public static XmlNodeImpl build(XmlPullParser parser, boolean trim) throws XmlPullParserException, IOException {
        return build(parser, trim, null);
    }

    /**
     * Core parsing method. Builds an XML node tree from the given pull parser.
     *
     * <p>Parsing begins at the parser's current event and consumes tokens through the
     * matching end tag of the first start tag encountered. When this method returns, the
     * parser is positioned immediately after that end tag, so the caller (or a subsequent
     * {@code build} call) can continue reading the same stream.</p>
     *
     * <p>Child elements are parsed recursively. If the element contains only text content
     * (no child elements), the text is stored as the node's value. If child elements are
     * present, the text value is {@code null} and children are accessible via
     * {@link XmlNodeImpl#getChildren()}.</p>
     *
     * @param parser the pull parser positioned at or before the root element's start tag
     * @param trim if {@code true}, leading and trailing whitespace is removed from text
     *             content unless the element has {@code xml:space="preserve"}
     * @param locationBuilder optional builder for recording input locations of parsed elements,
     *                        or {@code null} to skip location tracking
     * @return the parsed XML node tree
     * @throws XmlPullParserException if the XML is not well-formed
     * @throws IOException if an I/O error occurs while reading
     * @throws IllegalStateException if the end of the document is reached before the root
     *                               element's end tag is found
     * @since 3.2.0
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
     * Callback interface for creating input location objects during parsing.
     *
     * <p>Implementations determine how source location information (line number, column, etc.)
     * is captured and stored for each parsed element. The returned object is attached to the
     * resulting {@link XmlNodeImpl} as its location.</p>
     *
     * @since 3.2.0
     */
    public interface InputLocationBuilder {
        /**
         * Creates an input location object from the parser's current position.
         *
         * @param parser the pull parser, positioned at the start tag of the element being built
         * @return an object representing the source location, or {@code null} if location
         *         tracking is not needed for this element
         */
        Object toInputLocation(XmlPullParser parser);
    }
}
