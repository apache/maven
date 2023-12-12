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
package org.apache.maven.repository.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;

/**
 * Relocation source from standard distribution management. This is the "one and only" relocation implementation that
 * existed in Maven 3 land, uses POM distributionManagement/relocation.
 *
 * @since 4.0.0
 */
@Singleton
@Named
public final class DistributionManagementArtifactRelocationSource implements MavenArtifactRelocationSource {
    @Override
    public float getPriority() {
        return 5;
    }

    @Override
    public Artifact relocatedTarget(RepositorySystemSession session, Artifact artifact) {
        return null;
    }

    @Override
    public Artifact relocatedTarget(RepositorySystemSession session, Artifact artifact, Model model) {
        DistributionManagement distMgmt = model.getDistributionManagement();
        if (distMgmt != null) {
            Relocation relocation = distMgmt.getRelocation();
            if (relocation != null) {
                return new RelocatedArtifact(
                        artifact,
                        relocation.getGroupId(),
                        relocation.getArtifactId(),
                        relocation.getVersion(),
                        relocation.getMessage());
            }
        }
        return null;
    }
}
