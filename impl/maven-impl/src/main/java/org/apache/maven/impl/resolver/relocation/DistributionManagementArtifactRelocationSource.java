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
package org.apache.maven.impl.resolver.relocation;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Relocation;
import org.apache.maven.impl.resolver.MavenArtifactRelocationSource;
import org.apache.maven.impl.resolver.MetadataInputValidator;
import org.apache.maven.impl.resolver.RelocatedArtifact;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relocation source from standard distribution management. This is the "one and only" relocation implementation that
 * existed in Maven 3 land, uses POM distributionManagement/relocation.
 * <p>
 * Note: this component should kick-in last regarding relocations.
 *
 * @since 4.0.0
 */
@Singleton
@Named(DistributionManagementArtifactRelocationSource.NAME)
@Priority(5)
public final class DistributionManagementArtifactRelocationSource implements MavenArtifactRelocationSource {
    public static final String NAME = "distributionManagement";
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionManagementArtifactRelocationSource.class);

    @Override
    public Artifact relocatedTarget(
            RepositorySystemSession session, ArtifactDescriptorResult artifactDescriptorResult, Model model)
            throws ArtifactDescriptorException {
        DistributionManagement distMgmt = model.getDistributionManagement();
        if (distMgmt != null) {
            Relocation relocation = distMgmt.getRelocation();
            if (relocation != null) {
                Artifact original = artifactDescriptorResult.getRequest().getArtifact();
                validateCoordinateComponent(relocation.getGroupId(), "groupId", artifactDescriptorResult);
                validateCoordinateComponent(relocation.getArtifactId(), "artifactId", artifactDescriptorResult);
                validateCoordinateComponent(relocation.getVersion(), "version", artifactDescriptorResult);
                Artifact result = new RelocatedArtifact(
                        original,
                        relocation.getGroupId(),
                        relocation.getArtifactId(),
                        null,
                        null,
                        relocation.getVersion(),
                        relocation.getMessage());
                LOGGER.debug("The artifact {} has been relocated to {}: {}", original, result, relocation.getMessage());
                return result;
            }
        }
        return null;
    }

    /**
     * Checks that a relocation coordinate component is usable as an artifact coordinate. Components outside
     * the coordinate character set are rejected so that only well-formed coordinates enter resolution.
     */
    private static void validateCoordinateComponent(
            String value, String component, ArtifactDescriptorResult artifactDescriptorResult)
            throws ArtifactDescriptorException {
        if (value == null || value.isEmpty()) {
            return; // component is not relocated: the original artifact's value is kept
        }
        if (MetadataInputValidator.isInvalidCoordinateComponent(value)) {
            IllegalArgumentException cause = new IllegalArgumentException("Invalid relocation " + component + " '"
                    + value + "' in artifact descriptor for "
                    + artifactDescriptorResult.getRequest().getArtifact()
                    + ": not a valid artifact coordinate component");
            artifactDescriptorResult.addException(cause);
            throw new ArtifactDescriptorException(artifactDescriptorResult, cause.getMessage(), cause);
        }
    }
}
