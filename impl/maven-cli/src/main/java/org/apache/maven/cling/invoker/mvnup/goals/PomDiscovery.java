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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Files.POM_XML;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_0_0;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_1_0;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODEL_VERSION;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODULE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODULES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROFILE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROFILES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SUBPROJECT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SUBPROJECTS;

/**
 * Utility class for discovering and loading POM files in a Maven project hierarchy.
 */
public class PomDiscovery {

    /**
     * Discovers and loads all POM files starting from the given directory.
     *
     * @param startDirectory the directory to start discovery from
     * @return a map of Path to Document for all discovered POM files
     * @throws IOException if there's an error reading files
     * @throws JDOMException if there's an error parsing XML
     */
    public static Map<Path, Document> discoverPoms(Path startDirectory) throws IOException, JDOMException {
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
     * Recursively discovers modules from a POM document.
     * Enhanced for 4.1.0 models to support subprojects, profiles, and directory scanning.
     *
     * @param currentDirectory the current directory being processed
     * @param pomDocument the POM document to extract modules from
     * @param pomMap the map to add discovered POMs to
     * @throws IOException if there's an error reading files
     * @throws JDOMException if there's an error parsing XML
     */
    private static void discoverModules(Path currentDirectory, Document pomDocument, Map<Path, Document> pomMap)
            throws IOException, JDOMException {

        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Detect model version to determine discovery strategy
        String modelVersion = detectModelVersion(pomDocument);
        boolean is410OrLater = MODEL_VERSION_4_1_0.equals(modelVersion) || isNewerThan410(modelVersion);

        boolean foundModulesOrSubprojects = false;

        // Look for modules element (both 4.0.0 and 4.1.0)
        foundModulesOrSubprojects |= discoverFromModules(currentDirectory, root, namespace, pomMap);

        // For 4.1.0+ models, also check subprojects/subproject elements
        if (is410OrLater) {
            foundModulesOrSubprojects |= discoverFromSubprojects(currentDirectory, root, namespace, pomMap);
        }

        // Check inside profiles for both 4.0.0 and 4.1.0
        foundModulesOrSubprojects |= discoverFromProfiles(currentDirectory, root, namespace, pomMap, is410OrLater);

        // For 4.1.0 models, if no modules or subprojects defined, scan direct child directories
        if (is410OrLater && !foundModulesOrSubprojects) {
            discoverFromDirectories(currentDirectory, pomMap);
        }
    }

    /**
     * Detects the model version from a POM document.
     * The explicit modelVersion element takes precedence over namespace URI.
     */
    private static String detectModelVersion(Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        String explicitVersion = null;
        String namespaceVersion = null;

        // Check explicit modelVersion element first (takes precedence)
        Element modelVersionElement = root.getChild(MODEL_VERSION, namespace);
        if (modelVersionElement != null) {
            explicitVersion = modelVersionElement.getTextTrim();
        }

        // Check namespace URI for 4.1.0+ models
        if (namespace != null && namespace.getURI() != null) {
            String namespaceUri = namespace.getURI();
            if (namespaceUri.contains(MODEL_VERSION_4_1_0)) {
                namespaceVersion = MODEL_VERSION_4_1_0;
            }
        }

        // Explicit version takes precedence
        if (explicitVersion != null && !explicitVersion.isEmpty()) {
            // Check for mismatch between explicit version and namespace
            if (namespaceVersion != null && !explicitVersion.equals(namespaceVersion)) {
                System.err.println("WARNING: Model version mismatch in POM - explicit: " + explicitVersion
                        + ", namespace suggests: " + namespaceVersion + ". Using explicit version.");
            }
            return explicitVersion;
        }

        // Fall back to namespace-inferred version
        if (namespaceVersion != null) {
            return namespaceVersion;
        }

        // Default to 4.0.0 with warning
        System.err.println("WARNING: No model version found in POM, falling back to 4.0.0");
        return MODEL_VERSION_4_0_0;
    }

    /**
     * Checks if a model version is newer than 4.1.0.
     */
    private static boolean isNewerThan410(String modelVersion) {
        // Future versions like 4.2.0, 4.3.0, etc.
        return modelVersion.compareTo("4.1.0") > 0;
    }

    /**
     * Discovers modules from the modules element.
     */
    private static boolean discoverFromModules(
            Path currentDirectory, Element root, Namespace namespace, Map<Path, Document> pomMap)
            throws IOException, JDOMException {
        Element modulesElement = root.getChild(MODULES, namespace);
        if (modulesElement != null) {
            List<Element> moduleElements = modulesElement.getChildren(MODULE, namespace);

            for (Element moduleElement : moduleElements) {
                String modulePath = moduleElement.getTextTrim();
                if (!modulePath.isEmpty()) {
                    discoverModule(currentDirectory, modulePath, pomMap);
                }
            }
            return !moduleElements.isEmpty();
        }
        return false;
    }

    /**
     * Discovers subprojects from the subprojects element (4.1.0+ models).
     */
    private static boolean discoverFromSubprojects(
            Path currentDirectory, Element root, Namespace namespace, Map<Path, Document> pomMap)
            throws IOException, JDOMException {
        Element subprojectsElement = root.getChild(SUBPROJECTS, namespace);
        if (subprojectsElement != null) {
            List<Element> subprojectElements = subprojectsElement.getChildren(SUBPROJECT, namespace);

            for (Element subprojectElement : subprojectElements) {
                String subprojectPath = subprojectElement.getTextTrim();
                if (!subprojectPath.isEmpty()) {
                    discoverModule(currentDirectory, subprojectPath, pomMap);
                }
            }
            return !subprojectElements.isEmpty();
        }
        return false;
    }

    /**
     * Discovers modules/subprojects from profiles.
     */
    private static boolean discoverFromProfiles(
            Path currentDirectory, Element root, Namespace namespace, Map<Path, Document> pomMap, boolean is410OrLater)
            throws IOException, JDOMException {
        boolean foundAny = false;
        Element profilesElement = root.getChild(PROFILES, namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren(PROFILE, namespace);

            for (Element profileElement : profileElements) {
                // Check modules in profiles
                foundAny |= discoverFromModules(currentDirectory, profileElement, namespace, pomMap);

                // For 4.1.0+ models, also check subprojects in profiles
                if (is410OrLater) {
                    foundAny |= discoverFromSubprojects(currentDirectory, profileElement, namespace, pomMap);
                }
            }
        }
        return foundAny;
    }

    /**
     * Discovers POM files by scanning direct child directories (4.1.0+ fallback).
     */
    private static void discoverFromDirectories(Path currentDirectory, Map<Path, Document> pomMap)
            throws IOException, JDOMException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDirectory, Files::isDirectory)) {
            for (Path childDir : stream) {
                Path childPomPath = childDir.resolve(POM_XML);
                if (Files.exists(childPomPath) && !pomMap.containsKey(childPomPath)) {
                    Document childPom = loadPom(childPomPath);
                    pomMap.put(childPomPath, childPom);

                    // Recursively discover from this child
                    discoverModules(childDir, childPom, pomMap);
                }
            }
        }
    }

    /**
     * Discovers a single module/subproject.
     * The modulePath may point directly at a pom.xml file or a directory containing one.
     */
    private static void discoverModule(Path currentDirectory, String modulePath, Map<Path, Document> pomMap)
            throws IOException, JDOMException {
        Path resolvedPath = currentDirectory.resolve(modulePath);
        Path modulePomPath;
        Path moduleDirectory;

        // Check if modulePath points directly to a pom.xml file
        if (modulePath.endsWith(POM_XML) || (Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath))) {
            modulePomPath = resolvedPath;
            moduleDirectory = resolvedPath.getParent();
        } else {
            // modulePath points to a directory
            moduleDirectory = resolvedPath;
            modulePomPath = moduleDirectory.resolve(POM_XML);
        }

        if (Files.exists(modulePomPath) && !pomMap.containsKey(modulePomPath)) {
            Document modulePom = loadPom(modulePomPath);
            pomMap.put(modulePomPath, modulePom);

            // Recursively discover sub-modules
            discoverModules(moduleDirectory, modulePom, pomMap);
        }
    }

    /**
     * Loads a POM file using JDOM.
     *
     * @param pomPath the path to the POM file
     * @return the parsed Document
     * @throws IOException if there's an error reading the file
     * @throws JDOMException if there's an error parsing the XML
     */
    private static Document loadPom(Path pomPath) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(pomPath.toFile());
    }
}
