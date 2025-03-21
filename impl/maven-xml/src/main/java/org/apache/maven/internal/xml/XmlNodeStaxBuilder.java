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
import org.apache.maven.api.xml.XmlService;

/**
 * All methods in this class attempt to fully parse the XML.
 * The caller is responsible for closing {@code InputStream} and {@code Reader} arguments.
 *
 * @deprecated use {@code org.apache.maven.api.xml.XmlService} instead
 */
@Deprecated
public class XmlNodeStaxBuilder {
    private static final boolean DEFAULT_TRIM = true;

    public static XmlNode build(InputStream stream, InputLocationBuilderStax locationBuilder)
            throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newFactory().createXMLStreamReader(stream);
        return build(parser, DEFAULT_TRIM, locationBuilder);
    }

    public static XmlNode build(Reader reader, InputLocationBuilderStax locationBuilder) 
            throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newFactory().createXMLStreamReader(reader);
        return build(parser, DEFAULT_TRIM, locationBuilder);
    }

    public static XmlNode build(XMLStreamReader parser) throws XMLStreamException {
        return build(parser, DEFAULT_TRIM, null);
    }

    public static XmlNode build(XMLStreamReader parser, InputLocationBuilderStax locationBuilder)
            throws XMLStreamException {
        return build(parser, DEFAULT_TRIM, locationBuilder);
    }

    public static XmlNode build(XMLStreamReader parser, boolean trim, InputLocationBuilderStax locationBuilder)
            throws XMLStreamException {
        return XmlService.read(parser, locationBuilder != null ? locationBuilder::toInputLocation : null);
    }

    @Deprecated
    public interface InputLocationBuilderStax {
        Object toInputLocation(XMLStreamReader parser);
    }
}
