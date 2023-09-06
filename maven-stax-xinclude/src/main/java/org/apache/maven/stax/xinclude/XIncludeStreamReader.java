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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ctc.wstx.dtd.DTDSubset;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.io.Stax2Source;
import org.codehaus.stax2.io.Stax2URLSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import static javax.xml.XMLConstants.XML_NS_URI;

/**
 * Main implementation class for XInclude support.
 */
@SuppressWarnings("checkstyle:MissingSwitchDefault")
class XIncludeStreamReader extends StreamReaderDelegate {

    private static final String XINCLUDE_NAMESPACE = "http://www.w3.org/2001/XInclude";
    private static final String XINCLUDE_INCLUDE = "include";
    private static final String XINCLUDE_FALLBACK = "fallback";

    private final XMLInputFactory factory;
    private final XMLOutputFactory outputFactory;
    private final Deque<EventContext> contextStack = new ArrayDeque<>();
    private final Deque<String> xmlLangs = new ArrayDeque<>();
    private final Deque<String> xmlBases = new ArrayDeque<>();
    private boolean firstElementInContext;

    XIncludeStreamReader(
            XMLInputFactory factory, XMLOutputFactory outputFactory, String location, XMLStreamReader reader) {
        this.factory = factory;
        this.outputFactory = outputFactory;
        if (!(Boolean) reader.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE)) {
            throw new IllegalArgumentException("Namespace support should be enabled");
        }
        pushContext(location, reader);
    }

    @Override
    public int next() throws XMLStreamException {
        int event = super.next();
        if (event == START_ELEMENT) {
            contextStack.peek().depth++;
            String xmlLang = this.xmlLangs.peek();
            String xmlBase = firstElementInContext ? contextStack.peek().location : this.xmlBases.peek();
            firstElementInContext = false;
            for (int i = 0; i < getAttributeCount(); i++) {
                if ("xml".equals(getAttributePrefix(i))) {
                    switch (getAttributeLocalName(i)) {
                        case "lang":
                            xmlLang = getAttributeValue(i);
                            break;
                        case "base":
                            xmlBase = getAttributeValue(i);
                            break;
                    }
                }
            }
            this.xmlLangs.push(xmlLang != null ? xmlLang : "");
            this.xmlBases.push(xmlBase != null ? xmlBase : "");
            String namespace = getNamespaceURI();
            String localName = getLocalName();
            if (XINCLUDE_NAMESPACE.equals(namespace) && XINCLUDE_INCLUDE.equals(localName)) {
                processInclude();
                return next();
            }
        } else if (event == END_ELEMENT) {
            contextStack.peek().depth--;
            this.xmlBases.pop();
            this.xmlLangs.pop();
        } else if (event == END_DOCUMENT) {
            while (event == END_DOCUMENT) {
                if (contextStack.size() > 1) {
                    contextStack.pop();
                    event = next();
                } else {
                    break;
                }
            }
            firstElementInContext = false;
        }
        return event;
    }

    private void processInclude() throws XMLStreamException {

        Location startLocation = this.getLocation();

        Element node;
        try {
            Document doc =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            DOMResult res = new DOMResult(doc);
            XMLEventWriter xew = outputFactory.createXMLEventWriter(res);
            XMLEventReader xer = factory.createXMLEventReader(new FragmentReader(getDelegate()));
            xew.add(xer);
            node = doc.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        String href = getAttribute(node, "href");
        String parse = getAttribute(node, "parse");
        String xpointer = getAttribute(node, "xpointer");
        String fragid = getAttribute(node, "fragid");
        String setXmlId = getAttribute(node, "set-xml-id");
        String encoding = getAttribute(node, "encoding");

        IOException resourceError = null;

        if (href == null) {
            if (xpointer == null && fragid == null) {
                throw new XMLStreamException("xpointer or fragid must be used as href is null");
            }
            href = "";
        } else if (href.contains("#")) {
            throw new XMLStreamException("fragment identifiers must not be used in href: " + href);
        }
        URI hrefUri;
        try {
            hrefUri = new URI(href);
        } catch (URISyntaxException e) {
            throw new XMLStreamException("invalid syntax for href: " + href, e);
        }

        Source input;
        String currentLocation = xmlBases.peek();
        XMLResolver r = factory.getXMLResolver();
        Object o = r != null ? r.resolveEntity(null, href, currentLocation, null) : null;
        if (o instanceof URI) {
            try {
                o = new Stax2URLSource(((URI) o).toURL());
            } catch (MalformedURLException e) {
                throw new XMLStreamException(e);
            }
        }
        if (o != null && !(o instanceof Source)) {
            throw new XMLStreamException(
                    "Unsupported input of class " + o.getClass().getName());
        }
        if (o == null) {
            resourceError = new IOException("Unable to load resource: " + href);
        }
        input = (Source) o;

        boolean isXml = false;
        boolean isText = false;
        if (resourceError == null) {
            if (parse == null || "xml".equals(parse) || "application/xml".equals(parse) || parse.endsWith("+xml")) {
                isXml = true;
            } else if ("text".equals(parse) || parse.startsWith("text/")) {
                isText = true;
                if (xpointer != null) {
                    throw new XMLStreamException("xpointer cannot be used with text parsing");
                }
            } else {
                resourceError = new IOException("Unsupported media type: " + parse);
            }
        }

        boolean fallback = true;
        if (resourceError == null && isXml) {
            boolean reportPrologWs = (boolean) factory.getProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE);
            if (reportPrologWs) {
                factory.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, false);
            }
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            String pointer = xpointer != null ? xpointer : fragid;
            try {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .newDocument();
                DOMResult res = new DOMResult(doc);
                XMLEventWriter xew = outputFactory.createXMLEventWriter(res);
                XMLEventReader xer = factory.createXMLEventReader(reader);
                DTD dtd = null;
                while (xer.hasNext()) {
                    XMLEvent event = xer.nextEvent();
                    if (event instanceof DTD) {
                        dtd = (DTD) event;
                    } else {
                        xew.add(event);
                    }
                }

                Element resNode;
                if (pointer != null) {
                    DOMXMLElementEvaluator evaluator = new DOMXMLElementEvaluator(
                            new XPointer(pointer),
                            doc.getDocumentElement(),
                            dtd != null ? (DTDSubset) dtd.getProcessedDTD() : null);
                    resNode = evaluator.evaluateElement();
                } else {
                    resNode = doc.getDocumentElement();
                }

                if (resNode != null) {
                    // xml:lang fix
                    String curLang = this.xmlLangs.peek();
                    String impLang = "";

                    Element p = resNode;
                    while (p != null) {
                        Attr attr = p.getAttributeNodeNS(XML_NS_URI, "lang");
                        if (attr != null) {
                            impLang = attr.getValue();
                            break;
                        }
                        Node np = p.getParentNode();
                        p = np instanceof Element ? (Element) np : null;
                    }
                    if (!Objects.equals(curLang, impLang)) {
                        resNode.setAttributeNS(XML_NS_URI, "xml:lang", impLang);
                    }
                    resNode.setAttributeNS(XML_NS_URI, "xml:base", input.getSystemId());

                    NamedNodeMap attrs = node.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Attr att = (Attr) attrs.item(i);
                        String ns = att.getNamespaceURI();
                        if (ns != null && !XML_NS_URI.equals(ns)) {
                            if ("http://www.w3.org/2001/XInclude/local-attributes".equals(ns)) {
                                resNode.setAttribute(att.getLocalName(), att.getValue());
                            } else {
                                resNode.setAttributeNS(ns, att.getPrefix() + ":" + att.getLocalName(), att.getValue());
                            }
                        }
                    }
                    if (setXmlId != null) {
                        resNode.setAttributeNS(XML_NS_URI, "xml:id", setXmlId);
                    }

                    XMLStreamReader sr = factory.createXMLStreamReader(new DOMSource(resNode));
                    pushContext(input.getSystemId(), sr);
                    fallback = false;
                }
            } catch (InvalidXPointerException | ParserConfigurationException e) {
                throw new XMLStreamException(e);
            }
        } else if (resourceError == null && isText) {
            try {
                StringWriter sw = new StringWriter();
                Reader reader;
                if (input instanceof StreamSource) {
                    StreamSource ss = (StreamSource) input;
                    reader = ss.getReader();
                    if (reader == null) {
                        InputStream is = ss.getInputStream();
                        reader = new InputStreamReader(is, encoding != null ? encoding : "UTF-8");
                    }
                } else if (input instanceof Stax2Source) {
                    Stax2Source ss = (Stax2Source) input;
                    reader = ss.constructReader();
                    if (reader == null) {
                        InputStream is = ss.constructInputStream();
                        reader = new InputStreamReader(is, encoding != null ? encoding : "UTF-8");
                    }
                } else {
                    throw new XMLStreamException(
                            "Unsupported source of class " + input.getClass().getName());
                }
                transferTo(reader, sw);
                String include;
                if (fragid != null) {
                    String scheme;
                    String integrity;
                    int scIdx = fragid.indexOf(';');
                    if (scIdx > 0) {
                        scheme = fragid.substring(0, scIdx);
                        integrity = fragid.substring(scIdx + 1);
                    } else {
                        scheme = fragid;
                        integrity = "";
                    }
                    if (scheme.startsWith("char=")) {
                        String str = scheme.substring("char=".length());
                        int idx = str.indexOf(',');
                        int min, max;
                        if (idx >= 0) {
                            min = idx == 0 ? 0 : Integer.parseInt(str.substring(0, idx));
                            max = idx == str.length() - 1 ? str.length() - 1 : Integer.parseInt(str.substring(idx + 1));
                        } else {
                            min = Integer.parseInt(str);
                            max = min;
                        }
                        include = sw.toString().substring(min, max);
                    } else if (scheme.startsWith("line=")) {
                        String str = scheme.substring("line=".length());
                        int idx = str.indexOf(',');
                        int min, max;
                        if (idx >= 0) {
                            min = idx == 0 ? 0 : Integer.parseInt(str.substring(0, idx));
                            max = idx == str.length() - 1 ? str.length() - 1 : Integer.parseInt(str.substring(idx + 1));
                        } else {
                            min = Integer.parseInt(str);
                            max = min;
                        }
                        BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
                        include = br.lines().skip(min).limit(max - min).collect(Collectors.joining("\n", "", "\n"));
                    } else {
                        throw new XMLStreamException("Unsupported text scheme in fragid: " + fragid);
                    }
                    if (!integrity.isEmpty()) {
                        String charset = null;
                        int idx = integrity.indexOf(',');
                        if (idx >= 0) {
                            charset = integrity.substring(idx + 1);
                            integrity = integrity.substring(0, idx);
                            if (integrity.startsWith("length=")) {
                                // TODO: implement
                            } else if (integrity.startsWith("md5=")) {
                                // TODO: implement
                            } else {
                                throw new XMLStreamException("Unsupported text integrity in fragid: " + fragid);
                            }
                        }
                    }
                } else {
                    include = sw.toString();
                }

                StreamReaderDelegate sr = new StreamReaderDelegate() {
                    int state = 0;

                    @Override
                    protected XMLStreamReader getDelegate() {
                        return null;
                    }

                    @Override
                    public int next() throws XMLStreamException {
                        state++;
                        return getEventType();
                    }

                    @Override
                    public int getEventType() {
                        switch (state) {
                            case 0:
                                return START_ELEMENT;
                            case 1:
                                return CHARACTERS;
                        }
                        return END_DOCUMENT;
                    }

                    @Override
                    public String getText() {
                        return include;
                    }

                    @Override
                    public Location getLocation() {
                        return XMLStreamLocation2.NOT_AVAILABLE;
                    }
                };
                pushContext(input.getSystemId(), sr);
                fallback = false;
            } catch (IOException e) {
                resourceError = e;
            }
        }

        // now skip fallback elements
        boolean hasFallback = false;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element)) {
                continue;
            }
            if (XINCLUDE_NAMESPACE.equals(child.getNamespaceURI())) {
                if (XINCLUDE_FALLBACK.equals(child.getLocalName())) {
                    if (hasFallback) {
                        throw new XMLStreamException("One one xi:fallback element can be present", startLocation);
                    }
                    hasFallback = true;
                    if (fallback) {
                        XMLStreamReader sr = factory.createXMLStreamReader(new DOMSource(child));
                        sr.nextTag(); // the fallback
                        sr.next(); // next
                        sr = new FragmentReader(sr);
                        pushContext(currentLocation, sr);
                        break;
                    }
                } else {
                    throw new XMLStreamException(
                            "Element " + child.getLocalName() + " cannot be present inside the include element",
                            startLocation);
                }
            }
        }
    }

    private String getAttribute(Element node, String attr) {
        Attr a = node.getAttributeNode(attr);
        return a != null ? a.getValue() : null;
    }

    private void pushContext(String location, XMLStreamReader reader) {
        contextStack.push(new EventContext(location, reader));
        firstElementInContext = true;
    }

    protected XMLStreamReader getDelegate() {
        return contextStack.peek().getReader();
    }

    private static final int TRANSFER_BUFFER_SIZE = 8192;

    private static long transferTo(Reader in, Writer out) throws IOException {
        Objects.requireNonNull(out, "out");
        long transferred = 0;
        char[] buffer = new char[TRANSFER_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, buffer.length)) >= 0) {
            out.write(buffer, 0, read);
            if (transferred < Long.MAX_VALUE) {
                try {
                    transferred = Math.addExact(transferred, read);
                } catch (ArithmeticException ignore) {
                    transferred = Long.MAX_VALUE;
                }
            }
        }
        return transferred;
    }

    static class EventContext {

        final String location;
        final XMLStreamReader reader;
        final String input;
        int depth;
        int startDepth;

        EventContext(String location, XMLStreamReader reader) {
            this.location = Objects.requireNonNull(location);
            this.reader = Objects.requireNonNull(reader);
            this.input = null;
        }

        EventContext(String location, String input) {
            this.location = Objects.requireNonNull(location);
            this.input = Objects.requireNonNull(input);
            this.reader = null;
        }

        public String getLocation() {
            return location;
        }

        public XMLStreamReader getReader() {
            return reader;
        }
    }
}
