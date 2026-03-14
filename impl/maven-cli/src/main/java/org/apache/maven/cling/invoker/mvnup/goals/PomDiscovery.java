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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.DomTripException;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Parser;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODULE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODULES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Files.POM_XML;

/**
 * Discovers and loads Maven POM files in a Maven project hierarchy.
 *
 * <p>This class recursively discovers all POM files in a Maven project hierarchy
 * and loads them as domtrip Documents. Individual strategies can create domtrip Editors
 * from these Documents as needed for superior formatting preservation.
 */
public class PomDiscovery {

    private PomDiscovery() {
        // noop
    }

    /**
     * Discovers and loads all POM files starting from the given directory.
     *
     * @param startDirectory the directory to start discovery from
     * @return a map of Path to Document for all discovered POM files
     * @throws IOException if there's an error reading files
     * @throws DomTripException if there's an error parsing XML
     */
    public static Map<Path, Document> discoverPoms(Path startDirectory) throws IOException, DomTripException {
        Map<Path, Document> pomMap = new HashMap<>();

        // Find and load the root POM
        Path rootPomPath = startDirectory.resolve(POM_XML);
        if (!Files.exists(rootPomPath)) {
            throw new IOException("No pom.xml found in directory: " + startDirectory);
        }

        Document rootPom = loadPom(rootPomPath);
        pomMap.put(rootPomPath, rootPom);

        // Recursively discover modules
        discoverModules(startDirectory, rootPom, pomMap);

        return pomMap;
    }

    /**
     * Loads a POM file as a domtrip Document.
     *
     * @param pomPath the path to the POM file
     * @return the parsed Document
     * @throws IOException if there's an error reading the file
     * @throws DomTripException if there's an error parsing the XML
     */
    private static Document loadPom(Path pomPath) throws IOException, DomTripException {
        String content = Files.readString(pomPath);
        return new Parser().parse(content);
    }

    /**
     * Recursively discovers module POMs from the given parent POM.
     *
     * @param baseDirectory the base directory for resolving module paths
     * @param parentPom the parent POM document
     * @param pomMap the map to store discovered POMs
     * @throws IOException if there's an error reading files
     * @throws DomTripException if there's an error parsing XML
     */
    private static void discoverModules(Path baseDirectory, Document parentPom, Map<Path, Document> pomMap)
            throws IOException, DomTripException {

        Element rootElement = parentPom.root();
        if (rootElement == null) {
            return;
        }

        // Find modules element
        Element modulesElement = rootElement.child(MODULES).orElse(null);
        if (modulesElement == null) {
            return;
        }

        // Process each module
        List<Element> moduleElements = modulesElement.children(MODULE).toList();
        for (Element moduleElement : moduleElements) {
            String moduleName = moduleElement.textContentTrimmed();
            if (moduleName.isEmpty()) {
                continue;
            }

            Path moduleDirectory = baseDirectory.resolve(moduleName);
            Path modulePomPath = moduleDirectory.resolve(POM_XML);

            if (Files.exists(modulePomPath) && !pomMap.containsKey(modulePomPath)) {
                Document modulePom = loadPom(modulePomPath);
                pomMap.put(modulePomPath, modulePom);

                // Recursively discover sub-modules
                discoverModules(moduleDirectory, modulePom, pomMap);
            }
        }
    }
}
