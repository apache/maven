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
package org.apache.maven.impl;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.Function;

import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.api.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSettingsXmlFactoryTest {

    private DefaultSettingsXmlFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultSettingsXmlFactory();
    }

    @Test
    void testWriteWithoutFormatterDisablesLocationTracking() throws Exception {
        String xml = """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
                  <localRepository>/path/to/local/repo</localRepository>
                  <mirrors>
                    <mirror>
                      <mirrorOf>external:http:*</mirrorOf>
                      <name>Pseudo repository</name>
                      <url>http://0.0.0.0/</url>
                      <id>pseudo</id>
                    </mirror>
                  </mirrors>
                </settings>""";

        Settings settings = factory.read(XmlReaderRequest.builder()
                .reader(new StringReader(xml))
                .strict(true)
                .build());

        StringWriter out = new StringWriter();
        factory.write(XmlWriterRequest.<Settings>builder()
                .writer(out)
                .content(settings)
                .build());

        String result = out.toString();
        assertTrue(result.contains("<localRepository>"), "Expected settings content to be written to output");
        assertFalse(result.contains("<!--"), "No XML comments should be present when no formatter is provided");
    }

    @Test
    void testWriteWithFormatterEnablesLocationTracking() throws Exception {
        String xml = """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
                  <localRepository>/path/to/local/repo</localRepository>
                  <mirrors>
                    <mirror>
                      <mirrorOf>external:http:*</mirrorOf>
                      <name>Pseudo repository</name>
                      <url>http://0.0.0.0/</url>
                      <id>pseudo</id>
                    </mirror>
                  </mirrors>
                </settings>""";

        Settings settings = factory.read(XmlReaderRequest.builder()
                .reader(new StringReader(xml))
                .location("settings.xml")
                .strict(true)
                .build());

        StringWriter out = new StringWriter();
        Function<Object, String> formatter = o -> "LOC_MARK";

        factory.write(XmlWriterRequest.<Settings>builder()
                .writer(out)
                .content(settings)
                .inputLocationFormatter(formatter)
                .build());

        String result = out.toString();
        assertTrue(result.contains("<localRepository>"), "Expected settings content to be written to output");
        assertTrue(result.contains("LOC_MARK"), "Expected formatter marker in output when tracking is enabled");
    }
}
