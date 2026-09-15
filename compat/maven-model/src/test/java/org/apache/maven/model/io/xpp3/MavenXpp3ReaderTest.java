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
package org.apache.maven.model.io.xpp3;

import javax.xml.stream.XMLStreamException;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenXpp3ReaderTest {
    private static final String MALFORMED_XML = "<project><description>p&#9a9;</description></project>";

    @Test
    void malformedLazyXmlFailureIsReportedAsXmlPullParserException() {
        MavenXpp3Reader reader = new MavenXpp3Reader();

        XmlPullParserException readerEx =
                assertThrows(XmlPullParserException.class, () -> reader.read(new StringReader(MALFORMED_XML)));
        assertInstanceOf(XMLStreamException.class, readerEx.getCause(), "cause should be the XMLStreamException");

        XmlPullParserException streamEx = assertThrows(
                XmlPullParserException.class,
                () -> reader.read(new ByteArrayInputStream(MALFORMED_XML.getBytes(StandardCharsets.UTF_8))));
        assertInstanceOf(XMLStreamException.class, streamEx.getCause(), "cause should be the XMLStreamException");
    }
}
