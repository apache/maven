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
package org.apache.maven.project;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeBuilder;

/**
 * Creates an extension descriptor from some XML stream.
 *
 */
public class ExtensionDescriptorBuilder {

    /**
     * @since 3.3.0
     */
    public String getExtensionDescriptorLocation() {
        return "META-INF/maven/extension.xml";
    }

    /**
     * Extracts the extension descriptor (if any) from the specified JAR file.
     *
     * @param extensionJar The JAR file or directory to extract the descriptor from, must not be {@code null}.
     * @return The extracted descriptor or {@code null} if no descriptor was found.
     * @throws IOException If the descriptor is present but could not be parsed.
     */
    public ExtensionDescriptor build(File extensionJar) throws IOException {
        ExtensionDescriptor extensionDescriptor = null;

        if (extensionJar.isFile()) {
            try (JarFile pluginJar = new JarFile(extensionJar, false)) {
                ZipEntry pluginDescriptorEntry = pluginJar.getEntry(getExtensionDescriptorLocation());

                if (pluginDescriptorEntry != null) {
                    try (InputStream is = pluginJar.getInputStream(pluginDescriptorEntry)) {
                        extensionDescriptor = build(is);
                    }
                }
            }
        } else {
            File pluginXml = new File(extensionJar, getExtensionDescriptorLocation());

            if (pluginXml.canRead()) {
                try (InputStream is = Files.newInputStream(pluginXml.toPath())) {
                    extensionDescriptor = build(is);
                }
            }
        }

        return extensionDescriptor;
    }

    /**
     * @since 3.3.0
     */
    public ExtensionDescriptor build(InputStream is) throws IOException {
        ExtensionDescriptor extensionDescriptor = new ExtensionDescriptor();

        XmlNode dom;
        try {
            XMLStreamReader reader = WstxInputFactory.newFactory().createXMLStreamReader(is);
            dom = XmlNodeBuilder.build(reader);
        } catch (XMLStreamException e) {
            throw new IOException(e.getMessage(), e);
        }

        if (!"extension".equals(dom.getName())) {
            throw new IOException("Unexpected root element \"" + dom.getName() + "\", expected \"extension\"");
        }

        extensionDescriptor.setExportedPackages(parseStrings(dom.getChild("exportedPackages")));

        extensionDescriptor.setExportedArtifacts(parseStrings(dom.getChild("exportedArtifacts")));

        return extensionDescriptor;
    }

    private List<String> parseStrings(XmlNode dom) {
        List<String> strings = null;

        if (dom != null) {
            strings = new ArrayList<>();

            for (XmlNode child : dom.getChildren()) {
                String string = child.getValue();
                if (string != null) {
                    string = string.trim();
                    if (string.length() > 0) {
                        strings.add(string);
                    }
                }
            }
        }

        return strings;
    }
}
