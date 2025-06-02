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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import static org.apache.maven.cling.invoker.mvnup.goals.ModelVersionUtils.getSchemaLocationForModelVersion;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_0_0;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_1_0;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Namespaces.MAVEN_4_0_0_NAMESPACE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Namespaces.MAVEN_4_1_0_NAMESPACE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.SCHEMA_LOCATION;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.XSI_NAMESPACE_PREFIX;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.XSI_NAMESPACE_URI;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODULE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODULES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROFILE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROFILES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SUBPROJECT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SUBPROJECTS;

/**
 * Strategy for upgrading Maven model versions (e.g., 4.0.0 → 4.1.0).
 * Handles namespace updates, schema location changes, and element conversions.
 */
@Named
@Singleton
@Priority(40)
public class ModelUpgradeStrategy extends AbstractUpgradeStrategy {

    public ModelUpgradeStrategy() {
        // Target model version will be determined from context
    }

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        // Handle --all option (overrides individual options)
        if (options.all().orElse(false)) {
            return true;
        }

        String targetModel = determineTargetModelVersion(context);
        // Only applicable if we're not staying at 4.0.0
        return !MODEL_VERSION_4_0_0.equals(targetModel);
    }

    @Override
    public String getDescription() {
        return "Upgrading POM model version";
    }

    @Override
    public UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        String targetModelVersion = determineTargetModelVersion(context);

        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            String currentVersion = ModelVersionUtils.detectModelVersion(pomDocument);
            context.info(pomPath + " (current: " + currentVersion + ")");
            context.indent();

            try {
                if (currentVersion.equals(targetModelVersion)) {
                    context.success("Already at target version " + targetModelVersion);
                } else if (ModelVersionUtils.canUpgrade(currentVersion, targetModelVersion)) {
                    context.action("Upgrading from " + currentVersion + " to " + targetModelVersion);

                    // Perform the actual upgrade
                    context.indent();
                    try {
                        performModelUpgrade(pomDocument, context, currentVersion, targetModelVersion);
                    } finally {
                        context.unindent();
                    }
                    context.success("Model upgrade completed");
                    modifiedPoms.add(pomPath);
                } else {
                    context.warning("Cannot upgrade from " + currentVersion + " to " + targetModelVersion);
                }
            } catch (Exception e) {
                context.failure("Model upgrade failed: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Performs the core model upgrade from current version to target version.
     * This includes namespace updates and module conversion.
     */
    private void performModelUpgrade(
            Document pomDocument, UpgradeContext context, String currentVersion, String targetModelVersion) {
        // Update namespace and schema location to target version
        upgradeNamespaceAndSchemaLocation(pomDocument, context, targetModelVersion);

        // Convert modules to subprojects (for 4.1.0 and higher)
        if (ModelVersionUtils.isVersionGreaterOrEqual(targetModelVersion, MODEL_VERSION_4_1_0)) {
            convertModulesToSubprojects(pomDocument, context);
        }

        // Update modelVersion to target version (perhaps removed later during inference step)
        ModelVersionUtils.updateModelVersion(pomDocument, targetModelVersion);
        context.detail("Updated modelVersion to " + targetModelVersion);
    }

    /**
     * Updates namespace and schema location for the target model version.
     */
    private void upgradeNamespaceAndSchemaLocation(
            Document pomDocument, UpgradeContext context, String targetModelVersion) {
        Element root = pomDocument.getRootElement();

        // Update namespace based on target model version
        String targetNamespace = getNamespaceForModelVersion(targetModelVersion);
        Namespace newNamespace = Namespace.getNamespace(targetNamespace);
        updateElementNamespace(root, newNamespace);
        context.detail("Updated namespace to " + targetNamespace);

        // Update schema location
        Attribute schemaLocationAttr =
                root.getAttribute(SCHEMA_LOCATION, Namespace.getNamespace(XSI_NAMESPACE_PREFIX, XSI_NAMESPACE_URI));
        if (schemaLocationAttr != null) {
            schemaLocationAttr.setValue(getSchemaLocationForModelVersion(targetModelVersion));
            context.detail("Updated xsi:schemaLocation");
        }
    }

    /**
     * Recursively updates the namespace of an element and all its children.
     */
    private void updateElementNamespace(Element element, Namespace newNamespace) {
        element.setNamespace(newNamespace);
        for (Element child : element.getChildren()) {
            updateElementNamespace(child, newNamespace);
        }
    }

    /**
     * Converts modules to subprojects for 4.1.0 compatibility.
     */
    private void convertModulesToSubprojects(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Convert modules element to subprojects
        Element modulesElement = root.getChild(MODULES, namespace);
        if (modulesElement != null) {
            modulesElement.setName(SUBPROJECTS);
            context.detail("Converted <modules> to <subprojects>");

            // Convert all module children to subproject
            List<Element> moduleElements = modulesElement.getChildren(MODULE, namespace);
            for (Element moduleElement : moduleElements) {
                moduleElement.setName(SUBPROJECT);
            }

            if (!moduleElements.isEmpty()) {
                context.detail("Converted " + moduleElements.size() + " <module> elements to <subproject>");
            }
        }

        // Also check inside profiles
        Element profilesElement = root.getChild(PROFILES, namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren(PROFILE, namespace);
            for (Element profileElement : profileElements) {
                Element profileModulesElement = profileElement.getChild(MODULES, namespace);
                if (profileModulesElement != null) {
                    profileModulesElement.setName(SUBPROJECTS);

                    List<Element> profileModuleElements = profileModulesElement.getChildren(MODULE, namespace);
                    for (Element moduleElement : profileModuleElements) {
                        moduleElement.setName(SUBPROJECT);
                    }

                    if (!profileModuleElements.isEmpty()) {
                        context.detail("Converted " + profileModuleElements.size()
                                + " <module> elements to <subproject> in profiles");
                    }
                }
            }
        }
    }

    /**
     * Determines the target model version from the upgrade context.
     */
    private String determineTargetModelVersion(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        if (options.modelVersion().isPresent()) {
            return options.modelVersion().get();
        } else if (options.all().orElse(false)) {
            return MODEL_VERSION_4_1_0;
        } else {
            return MODEL_VERSION_4_0_0;
        }
    }

    /**
     * Gets the namespace URI for a model version.
     */
    private String getNamespaceForModelVersion(String modelVersion) {
        if (MODEL_VERSION_4_1_0.equals(modelVersion)) {
            return MAVEN_4_1_0_NAMESPACE;
        } else {
            return MAVEN_4_0_0_NAMESPACE;
        }
    }
}
