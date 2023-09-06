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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import java.util.Objects;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import org.codehaus.stax2.XMLInputFactory2;

/**
 * XInclude support
 */
public class XInclude {

    /**
     * Creates a XML stream reader that supports XInclude.
     * <p>
     * External files will be loaded using the given {@code XMLResolver}
     * and called with {@code systemId} set to the URL to load and
     * {@code baseURI} set to the current document location (the initial
     * one will be the {@code systemId} of the input {@code Source}.
     *
     * @param source the XML source to parse
     * @param resolver the XML resolver to use when resolving external files
     * @return a XML stream reader that supports xinclude
     * @throws XMLStreamException if the stream reader cannot be created
     */
    public static XMLStreamReader xinclude(Source source, XMLResolver resolver) throws XMLStreamException {
        XMLInputFactory2 factory = new WstxInputFactory();
        factory.configureForRoundTripping();
        factory.setProperty(WstxInputProperties.P_TREAT_CHAR_REFS_AS_ENTS, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
        factory.setProperty(WstxInputProperties.P_ENTITY_RESOLVER, Objects.requireNonNull(resolver));

        XMLStreamReader reader = factory.createXMLStreamReader(source);
        return xinclude(factory, new WstxOutputFactory(), source.getSystemId(), reader);
    }

    public static XMLStreamReader xinclude(
            XMLInputFactory factory, XMLOutputFactory outputFactory, String location, XMLStreamReader reader) {
        return new XIncludeStreamReader(factory, outputFactory, location, reader);
    }
}
