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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Creates an extension descriptor from some XML stream.
 *
 * @author Benjamin Bentmann
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
                try (InputStream is = new BufferedInputStream(new FileInputStream(pluginXml))) {
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

        Xpp3Dom dom;
        try {
            dom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(is));
        } catch (XmlPullParserException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }

        if (!"extension".equals(dom.getName())) {
            throw new IOException("Unexpected root element \"" + dom.getName() + "\", expected \"extension\"");
        }

        extensionDescriptor.setExportedPackages(parseStrings(dom.getChild("exportedPackages")));

        extensionDescriptor.setExportedArtifacts(parseStrings(dom.getChild("exportedArtifacts")));

        return extensionDescriptor;
    }

    private List<String> parseStrings(Xpp3Dom dom) {
        List<String> strings = null;

        if (dom != null) {
            strings = new ArrayList<>();

            for (Xpp3Dom child : dom.getChildren()) {
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
