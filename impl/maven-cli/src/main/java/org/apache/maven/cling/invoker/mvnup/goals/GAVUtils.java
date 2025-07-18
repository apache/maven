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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ARTIFACT_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PARENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.VERSION;

/**
 * Utility class for handling GroupId, ArtifactId, Version (GAV) operations
 * in Maven POM files during the upgrade process.
 */
public final class GAVUtils {

    private GAVUtils() {
        // Utility class
    }

    /**
     * Computes all GAVs from all POMs in the multi-module project for inference.
     * This includes resolving parent inheritance and relative path parents.
     *
     * @param context the upgrade context
     * @param pomMap map of all POM files in the project
     * @return set of all GAVs in the project
     */
    public static Set<GAV> computeAllGAVs(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<GAV> gavs = new HashSet<>();

        context.info("Computing GAVs for inference from " + pomMap.size() + " POM(s)...");

        // Extract GAV from all POMs in the project
        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();

            GAV gav = extractGAVWithParentResolution(context, pomDocument);
            if (gav != null) {
                gavs.add(gav);
                context.debug("Found GAV: " + gav + " from " + pomPath);
            }
        }

        context.info("Computed " + gavs.size() + " unique GAV(s) for inference");
        return gavs;
    }

    /**
     * Extracts GAV from a POM document with parent resolution.
     * If groupId or version are missing, attempts to resolve from parent.
     *
     * @param context     the upgrade context for logging
     * @param pomDocument the POM document
     * @return the GAV or null if it cannot be determined
     */
    public static GAV extractGAVWithParentResolution(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Extract direct values
        String groupId = getElementText(root, GROUP_ID, namespace);
        String artifactId = getElementText(root, ARTIFACT_ID, namespace);
        String version = getElementText(root, VERSION, namespace);

        // If groupId or version is missing, try to get from parent
        if (groupId == null || version == null) {
            Element parentElement = root.getChild(PARENT, namespace);
            if (parentElement != null) {
                if (groupId == null) {
                    groupId = getElementText(parentElement, GROUP_ID, namespace);
                }
                if (version == null) {
                    version = getElementText(parentElement, VERSION, namespace);
                }
            }
        }

        // ArtifactId is required and cannot be inherited
        if (artifactId == null || artifactId.isEmpty()) {
            context.debug("Cannot determine artifactId for POM");
            return null;
        }

        // GroupId and version can be inherited, but if still null, we can't create a valid GAV
        if (groupId == null || groupId.isEmpty() || version == null || version.isEmpty()) {
            context.debug("Cannot determine complete GAV for artifactId: " + artifactId);
            return null;
        }

        return new GAV(groupId, artifactId, version);
    }

    /**
     * Gets the text content of a child element.
     *
     * @param parent the parent element
     * @param elementName the name of the child element
     * @param namespace the namespace
     * @return the text content or null if element doesn't exist
     */
    private static String getElementText(Element parent, String elementName, Namespace namespace) {
        Element element = parent.getChild(elementName, namespace);
        return element != null ? element.getTextTrim() : null;
    }
}
