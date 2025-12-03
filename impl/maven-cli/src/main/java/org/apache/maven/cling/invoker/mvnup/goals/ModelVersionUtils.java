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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODEL_VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_0_0;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_1_0;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_2_0;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Namespaces.MAVEN_4_0_0_NAMESPACE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Namespaces.MAVEN_4_1_0_NAMESPACE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Namespaces.MAVEN_4_2_0_NAMESPACE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.SchemaLocations.MAVEN_4_0_0_SCHEMA_LOCATION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.SchemaLocations.MAVEN_4_1_0_SCHEMA_LOCATION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.SchemaLocations.MAVEN_4_2_0_SCHEMA_LOCATION;

/**
 * Utility class for handling Maven model version operations during upgrades.
 *
 * <p>This class uses domtrip internally for superior formatting preservation
 * and simplified API while maintaining the same external interface.
 */
public final class ModelVersionUtils {

    private ModelVersionUtils() {
        // Utility class
    }

    /**
     * Detects the model version from a POM document.
     * Uses both the modelVersion element and namespace URI for detection.
     *
     * @param pomDocument the POM document (domtrip Document)
     * @return the detected model version
     */
    public static String detectModelVersion(Document pomDocument) {
        Editor editor = new Editor(pomDocument);
        Element root = editor.root();
        if (root == null) {
            return MODEL_VERSION_4_0_0;
        }

        // First try to get from modelVersion element
        Element modelVersionElement = root.child(MODEL_VERSION).orElse(null);
        if (modelVersionElement != null) {
            String modelVersion = modelVersionElement.textContentTrimmed();
            if (!modelVersion.isEmpty()) {
                return modelVersion;
            }
        }

        // Fallback to namespace URI detection
        String namespaceUri = root.namespaceDeclaration(null);
        if (MAVEN_4_2_0_NAMESPACE.equals(namespaceUri)) {
            return MODEL_VERSION_4_2_0;
        } else if (MAVEN_4_1_0_NAMESPACE.equals(namespaceUri)) {
            return MODEL_VERSION_4_1_0;
        } else if (MAVEN_4_0_0_NAMESPACE.equals(namespaceUri)) {
            return MODEL_VERSION_4_0_0;
        }

        // Default fallback
        return MODEL_VERSION_4_0_0;
    }

    /**
     * Checks if a model version is valid for upgrade operations.
     * Currently supports 4.0.0, 4.1.0, and 4.2.0.
     *
     * @param modelVersion the model version to validate
     * @return true if the model version is valid
     */
    public static boolean isValidModelVersion(String modelVersion) {
        return MODEL_VERSION_4_0_0.equals(modelVersion)
                || MODEL_VERSION_4_1_0.equals(modelVersion)
                || MODEL_VERSION_4_2_0.equals(modelVersion);
    }

    /**
     * Checks if an upgrade from one version to another is possible.
     *
     * @param fromVersion the source version
     * @param toVersion the target version
     * @return true if the upgrade is possible
     */
    public static boolean canUpgrade(String fromVersion, String toVersion) {
        if (fromVersion == null || toVersion == null) {
            return false;
        }

        // Support upgrades: 4.0.0 → 4.1.0, 4.0.0 → 4.2.0, 4.1.0 → 4.2.0
        if (MODEL_VERSION_4_0_0.equals(fromVersion)) {
            return MODEL_VERSION_4_1_0.equals(toVersion) || MODEL_VERSION_4_2_0.equals(toVersion);
        }
        if (MODEL_VERSION_4_1_0.equals(fromVersion)) {
            return MODEL_VERSION_4_2_0.equals(toVersion);
        }

        return false;
    }

    /**
     * Checks if a model version is eligible for inference optimizations.
     * Models 4.0.0+ are eligible (4.0.0 has limited inference, 4.1.0+ has full inference).
     *
     * @param modelVersion the model version to check
     * @return true if eligible for inference
     */
    public static boolean isEligibleForInference(String modelVersion) {
        return MODEL_VERSION_4_0_0.equals(modelVersion)
                || MODEL_VERSION_4_1_0.equals(modelVersion)
                || MODEL_VERSION_4_2_0.equals(modelVersion);
    }

    /**
     * Checks if a model version is newer than 4.1.0.
     *
     * @param modelVersion the model version to check
     * @return true if newer than 4.1.0
     */
    public static boolean isNewerThan410(String modelVersion) {
        if (modelVersion == null) {
            return false;
        }

        // Simple version comparison for now
        // This could be enhanced with proper version parsing if needed
        try {
            String[] parts = modelVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);

                if (major > 4) {
                    return true;
                }
                if (major == 4 && minor > 1) {
                    return true;
                }
                if (major == 4 && minor == 1 && parts.length > 2) {
                    int patch = Integer.parseInt(parts[2]);
                    return patch > 0;
                }
            }
        } catch (NumberFormatException e) {
            // If we can't parse it, assume it's not newer
            return false;
        }

        return false;
    }

    /**
     * Checks if a model version is greater than or equal to a target version.
     *
     * @param modelVersion the model version to check
     * @param targetVersion the target version to compare against
     * @return true if modelVersion >= targetVersion
     */
    public static boolean isVersionGreaterOrEqual(String modelVersion, String targetVersion) {
        if (modelVersion == null || targetVersion == null) {
            return false;
        }

        // Handle exact equality first
        if (modelVersion.equals(targetVersion)) {
            return true;
        }

        // For now, handle the specific cases we need
        if (MODEL_VERSION_4_1_0.equals(targetVersion)) {
            return isNewerThan410(modelVersion);
        }

        // Default to false for unknown comparisons
        return false;
    }

    /**
     * Updates the model version element in a POM document.
     *
     * @param pomDocument the POM document (domtrip Document)
     * @param newVersion the new model version
     */
    public static void updateModelVersion(Document pomDocument, String newVersion) {
        Editor editor = new Editor(pomDocument);
        Element root = editor.root();
        if (root == null) {
            return;
        }

        Element modelVersionElement = root.child(MODEL_VERSION).orElse(null);
        if (modelVersionElement != null) {
            editor.setTextContent(modelVersionElement, newVersion);
        } else {
            // Create new modelVersion element if it doesn't exist
            // domtrip will automatically handle proper positioning and formatting
            DomUtils.insertContentElement(root, MODEL_VERSION, newVersion);
        }
    }

    /**
     * Removes the model version element from a POM editor.
     * This is used during inference when the model version can be inferred.
     *
     * @param document the XML document
     * @return true if the element was removed, false if it didn't exist
     */
    public static boolean removeModelVersion(Document document) {
        Element root = document.root();
        if (root == null) {
            return false;
        }

        Element modelVersionElement = root.child(MODEL_VERSION).orElse(null);
        if (modelVersionElement != null) {
            return root.removeNode(modelVersionElement);
        }
        return false;
    }

    /**
     * Gets the schema location for a model version.
     *
     * @param modelVersion the model version
     * @return the schema location
     */
    public static String getSchemaLocationForModelVersion(String modelVersion) {
        if (MODEL_VERSION_4_2_0.equals(modelVersion)) {
            return MAVEN_4_2_0_SCHEMA_LOCATION;
        } else if (MODEL_VERSION_4_1_0.equals(modelVersion)) {
            return MAVEN_4_1_0_SCHEMA_LOCATION;
        } else if (isNewerThan410(modelVersion)) {
            // For versions newer than 4.1.0 but not specifically 4.2.0, use 4.2.0 schema
            return MAVEN_4_2_0_SCHEMA_LOCATION;
        }
        return MAVEN_4_0_0_SCHEMA_LOCATION;
    }
}
