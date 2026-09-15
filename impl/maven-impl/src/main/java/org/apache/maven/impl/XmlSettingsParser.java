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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.spi.SettingsParser;
import org.apache.maven.api.spi.SettingsParserException;

final class XmlSettingsParser implements SettingsParser {
    private final SettingsXmlFactory settingsXmlFactory;

    XmlSettingsParser(SettingsXmlFactory settingsXmlFactory) {
        this.settingsXmlFactory = settingsXmlFactory;
    }

    @Override
    public boolean supports(Source source) {
        return true;
    }

    @Override
    public Settings parse(Source source, Map<String, ?> options) throws IOException {
        try (InputStream stream = source.openStream()) {
            return settingsXmlFactory.read(XmlReaderRequest.builder()
                    .inputStream(stream)
                    .location(source.getLocation())
                    .strict(options == null || !Boolean.FALSE.equals(options.get(STRICT)))
                    .build());
        } catch (XmlReaderException e) {
            var location = e.getLocation();
            throw new SettingsParserException(
                    e.getMessage(),
                    location != null ? location.getLineNumber() : -1,
                    location != null ? location.getColumnNumber() : -1,
                    e);
        }
    }
}
