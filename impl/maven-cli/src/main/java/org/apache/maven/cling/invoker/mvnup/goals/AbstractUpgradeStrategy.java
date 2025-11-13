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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.Coordinates;
import eu.maveniverse.domtrip.maven.MavenPomElements;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PARENT;

/**
 * Abstract base class for upgrade strategies that provides common functionality
 * and reduces code duplication across strategy implementations.
 *
 * <p>Strategies work with domtrip Documents for perfect formatting preservation.
 * Subclasses can create domtrip Editors from Documents as needed:
 * <pre>
 * Editor editor = new Editor(document);
 * // ... perform domtrip operations ...
 * // Document is automatically updated
 * </pre>
 */
public abstract class AbstractUpgradeStrategy implements UpgradeStrategy {

    /**
     * Template method that handles common logging and error handling.
     * Subclasses implement the actual upgrade logic in doApply().
     */
    @Override
    public final UpgradeResult apply(UpgradeContext context, Map<Path, Document> pomMap) {
        context.info(getDescription());
        context.indent();

        try {
            UpgradeResult result = doApply(context, pomMap);

            // Log summary
            logSummary(context, result);

            return result;
        } catch (Exception e) {
            context.failure("Strategy execution failed: " + e.getMessage());
            return UpgradeResult.failure(pomMap.keySet(), Set.of());
        } finally {
            context.unindent();
        }
    }

    /**
     * Subclasses implement the actual upgrade logic here.
     *
     * @param context the upgrade context
     * @param pomMap map of all POM files in the project
     * @return the result of the upgrade operation
     */
    protected abstract UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap);

    /**
     * Gets the upgrade options from the context.
     *
     * @param context the upgrade context
     * @return the upgrade options
     */
    protected final UpgradeOptions getOptions(UpgradeContext context) {
        return context.options();
    }

    /**
     * Logs a summary of the upgrade results.
     *
     * @param context the upgrade context
     * @param result the upgrade result
     */
    protected void logSummary(UpgradeContext context, UpgradeResult result) {
        context.println();
        context.info(getDescription() + " Summary:");
        context.indent();
        context.info(result.modifiedCount() + " POM(s) modified");
        context.info(result.unmodifiedCount() + " POM(s) needed no changes");
        if (result.errorCount() > 0) {
            context.info(result.errorCount() + " POM(s) had errors");
        }
        context.unindent();
    }

    /**
     * Extracts an Artifact from a POM document with parent resolution.
     * If groupId or version are missing, attempts to resolve from parent.
     *
     * <p>This method handles Maven's inheritance mechanism where groupId and version
     * can be inherited from the parent POM.
     *
     * @param context the upgrade context for logging
     * @param pomDocument the POM document
     * @return the Artifact or null if it cannot be determined
     */
    public static Coordinates extractArtifactCoordinatesWithParentResolution(
            UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.root();

        // Extract direct values
        String groupId = root.childTextTrimmed(MavenPomElements.Elements.GROUP_ID);
        String artifactId = root.childTextTrimmed(MavenPomElements.Elements.ARTIFACT_ID);
        String version = root.childTextTrimmed(MavenPomElements.Elements.VERSION);

        // If groupId or version is missing, try to get from parent
        if (groupId == null || version == null) {
            Element parentElement = root.child(PARENT).orElse(null);
            if (parentElement != null) {
                if (groupId == null) {
                    groupId = parentElement.childTextTrimmed(MavenPomElements.Elements.GROUP_ID);
                }
                if (version == null) {
                    version = parentElement.childTextTrimmed(MavenPomElements.Elements.VERSION);
                }
            }
        }

        // ArtifactId is required and cannot be inherited
        if (artifactId == null || artifactId.isEmpty()) {
            context.debug("Cannot determine artifactId for POM");
            return null;
        }

        // GroupId and version can be inherited, but if still null, we can't create a valid Artifact
        if (groupId == null || groupId.isEmpty() || version == null || version.isEmpty()) {
            context.debug("Cannot determine complete GAV for artifactId: " + artifactId);
            return null;
        }

        return Coordinates.of(groupId, artifactId, version);
    }

    /**
     * Computes all artifacts from all POMs in a multi-module project.
     * This includes resolving parent inheritance.
     *
     * @param context the upgrade context for logging
     * @param pomMap map of all POM files in the project
     * @return set of all Artifacts in the project
     */
    public static Set<Coordinates> computeAllArtifactCoordinates(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Coordinates> coordinates = new HashSet<>();

        context.info("Computing artifacts for inference from " + pomMap.size() + " POM(s)...");

        // Extract artifact from all POMs in the project
        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();

            Coordinates coordinate =
                    AbstractUpgradeStrategy.extractArtifactCoordinatesWithParentResolution(context, pomDocument);
            if (coordinate != null) {
                coordinates.add(coordinate);
                context.debug("Found artifact: " + coordinate.toGAV() + " from " + pomPath);
            }
        }

        context.info("Computed " + coordinates.size() + " unique artifact(s) for inference");
        return coordinates;
    }
}
