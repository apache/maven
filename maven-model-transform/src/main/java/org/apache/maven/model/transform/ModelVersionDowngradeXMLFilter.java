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
package org.apache.maven.model.transform;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.transform.stax.BufferingParser;
import org.apache.maven.model.v4.MavenModelVersion;
import org.apache.maven.model.v4.MavenStaxReader;

public class ModelVersionDowngradeXMLFilter extends BufferingParser {

    public static final String NAMESPACE_PREFIX = "http://maven.apache.org/POM/";

    private final List<Event> buffer = new ArrayList<>();

    public ModelVersionDowngradeXMLFilter(XMLStreamReader delegate) {
        super(delegate);
    }

    @Override
    protected boolean accept() throws XMLStreamException {
        Event e = bufferEvent();
        buffer.add(e);
        if (e.event == XMLStreamReader.END_DOCUMENT) {
            ReplayParser p = new ReplayParser(this);
            buffer.forEach(p::pushEvent);
            p.next();
            String version;
            Model model = new MavenStaxReader().read(p, false, null);
            if (model.isPreserveModelVersion()) {
                version = model.getModelVersion();
            } else {
                model = model.withPreserveModelVersion(false);
                version = new MavenModelVersion().getModelVersion(model);
            }
            int depth = 0;
            boolean isModelVersion = false;
            for (Event event : buffer) {
                event.namespace = NAMESPACE_PREFIX + version;
                // rewrite namespace
                if (event.namespaces != null) {
                    for (int i = 0; i < event.namespaces.length; i++) {
                        if (event.namespaces[i].uri.startsWith(NAMESPACE_PREFIX)) {
                            event.namespaces[i].uri = event.namespace;
                        }
                    }
                }
                // rewrite xsi:schemaLocation attribute
                if (event.attributes != null) {
                    for (Attribute attribute : event.attributes) {
                        if (attribute.namespace.equals("http://www.w3.org/2001/XMLSchema-instance")
                                && attribute.name.equals("schemaLocation")) {
                            attribute.value = attribute
                                    .value
                                    .replaceAll(
                                            "\\Q" + NAMESPACE_PREFIX + "\\E[0-9]\\.[0-9]\\.[0-9]",
                                            NAMESPACE_PREFIX + version)
                                    .replaceAll(
                                            "http(s?)://maven\\.apache\\.org/xsd/maven-[0-9]\\.[0-9]\\.[0-9]\\.xsd",
                                            "https://maven.apache.org/xsd/maven-" + version + ".xsd");
                        }
                    }
                }
                // Rewrite modelVersion
                if (event.event == XMLStreamReader.START_ELEMENT) {
                    depth++;
                    isModelVersion = depth == 2 && event.name.equals("modelVersion");
                }
                if (event.event == XMLStreamReader.CHARACTERS && isModelVersion) {
                    event.text = version;
                }
                if (event.event == XMLStreamReader.END_ELEMENT) {
                    depth--;
                    isModelVersion = false;
                }
                pushEvent(event);
            }
        }
        return false;
    }

    static class ReplayParser extends BufferingParser {
        ReplayParser(XMLStreamReader delegate) {
            super(delegate);
        }

        public void pushEvent(Event e) {
            super.pushEvent(e);
        }
    }
}
