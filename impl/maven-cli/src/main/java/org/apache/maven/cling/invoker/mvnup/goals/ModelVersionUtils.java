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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_0_0;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_1_0;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Namespaces.MAVEN_4_0_0_NAMESPACE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Namespaces.MAVEN_4_1_0_NAMESPACE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.SchemaLocations.MAVEN_4_1_0_SCHEMA_LOCATION;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODEL_VERSION;

/**
 * Utility class for handling Maven model version operations during upgrades.
 */
public final class ModelVersionUtils {

    private ModelVersionUtils() {
        // Utility class
    }

    /**
     * Detects the model version from a POM document.
     * Uses both the modelVersion element and namespace URI for detection.
     *
     * @param pomDocument the POM document
     * @return the detected model version
     */
    public static String detectModelVersion(Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // First try to get from modelVersion element
        Element modelVersionElement = root.getChild(MODEL_VERSION, namespace);
        if (modelVersionElement != null) {
            String modelVersion = modelVersionElement.getTextTrim();
            if (!modelVersion.isEmpty()) {
                return modelVersion;
            }
        }

        // Fallback to namespace URI detection
        String namespaceUri = namespace.getURI();
        if (MAVEN_4_1_0_NAMESPACE.equals(namespaceUri)) {
            return MODEL_VERSION_4_1_0;
        } else if (MAVEN_4_0_0_NAMESPACE.equals(namespaceUri)) {
            return MODEL_VERSION_4_0_0;
        }

        // Default fallback
        return MODEL_VERSION_4_0_0;
    }

    /**
     * Checks if a model version is valid for upgrade operations.
     * Currently only supports 4.0.0 and 4.1.0.
     *
     * @param modelVersion the model version to validate
     * @return true if the model version is valid
     */
    public static boolean isValidModelVersion(String modelVersion) {
        return MODEL_VERSION_4_0_0.equals(modelVersion) || MODEL_VERSION_4_1_0.equals(modelVersion);
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

        // Currently only support 4.0.0 → 4.1.0 upgrade
        return MODEL_VERSION_4_0_0.equals(fromVersion) && MODEL_VERSION_4_1_0.equals(toVersion);
    }

    /**
     * Checks if a model version is eligible for inference optimizations.
     * Models 4.0.0+ are eligible (4.0.0 has limited inference, 4.1.0+ has full inference).
     *
     * @param modelVersion the model version to check
     * @return true if eligible for inference
     */
    public static boolean isEligibleForInference(String modelVersion) {
        return MODEL_VERSION_4_0_0.equals(modelVersion) || MODEL_VERSION_4_1_0.equals(modelVersion);
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
            return MODEL_VERSION_4_1_0.equals(modelVersion) || isNewerThan410(modelVersion);
        }

        // Default to false for unknown comparisons
        return false;
    }

    /**
     * Updates the model version element in a POM document.
     *
     * @param pomDocument the POM document
     * @param newVersion the new model version
     */
    public static void updateModelVersion(Document pomDocument, String newVersion) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        Element modelVersionElement = root.getChild(MODEL_VERSION, namespace);
        if (modelVersionElement != null) {
            modelVersionElement.setText(newVersion);
        } else {
            // Create new modelVersion element if it doesn't exist
            Element newModelVersionElement = new Element(MODEL_VERSION, namespace);
            newModelVersionElement.setText(newVersion);

            // Insert at the beginning of the document
            root.addContent(0, newModelVersionElement);
        }
    }

    /**
     * Removes the model version element from a POM document.
     * This is used during inference when the model version can be inferred.
     *
     * @param pomDocument the POM document
     * @return true if the element was removed, false if it didn't exist
     */
    public static boolean removeModelVersion(Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        Element modelVersionElement = root.getChild(MODEL_VERSION, namespace);
        if (modelVersionElement != null) {
            return root.removeContent(modelVersionElement);
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
        if (MODEL_VERSION_4_1_0.equals(modelVersion) || isNewerThan410(modelVersion)) {
            return MAVEN_4_1_0_SCHEMA_LOCATION;
        }
        return UpgradeConstants.SchemaLocations.MAVEN_4_0_0_SCHEMA_LOCATION;
    }
}
