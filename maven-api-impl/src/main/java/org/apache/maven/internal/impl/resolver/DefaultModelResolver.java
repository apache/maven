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
package org.apache.maven.internal.impl.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.ResolvedArtifact;
import org.apache.maven.api.Session;
import org.apache.maven.api.Version;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ModelResolver;
import org.apache.maven.api.services.ModelResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.VersionRangeResolverException;

/**
 * A model resolver to assist building of dependency POMs.
 *
 * @see DefaultArtifactDescriptorReader
 */
@Named
@Singleton
public class DefaultModelResolver implements ModelResolver {

    @Override
    public ModelSource resolveModel(
            Session session, String groupId, String artifactId, String version, Consumer<String> resolvedVersion)
            throws ModelResolverException {
        try {
            ArtifactCoordinate coord = session.createArtifactCoordinate(groupId, artifactId, version, "pom");
            if (coord.getVersionConstraint().getVersionRange() != null
                    && coord.getVersionConstraint().getVersionRange().getUpperBoundary() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new ModelResolverException(
                        String.format("The requested version range '%s' does not specify an upper bound", version),
                        groupId,
                        artifactId,
                        version);
            }
            List<Version> versions = session.resolveVersionRange(coord);
            if (versions.isEmpty()) {
                throw new ModelResolverException(
                        String.format("No versions matched the requested version range '%s'", version),
                        groupId,
                        artifactId,
                        version);
            }
            String newVersion = versions.get(versions.size() - 1).asString();
            if (!version.equals(newVersion)) {
                resolvedVersion.accept(newVersion);
            }

            ResolvedArtifact resolved =
                    session.resolveArtifact(session.createArtifactCoordinate(groupId, artifactId, newVersion, "pom"));
            Path path = resolved.getPath();
            String location = groupId + ":" + artifactId + ":" + newVersion;
            return new ModelSource() {
                @Override
                public ModelSource resolve(ModelLocator modelLocator, String relative) {
                    return null;
                }

                @Override
                public Path getPath() {
                    return null;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return Files.newInputStream(path);
                }

                @Override
                public String getLocation() {
                    return location;
                }

                @Override
                public Source resolve(String relative) {
                    return null;
                }
            };
        } catch (VersionRangeResolverException | ArtifactResolverException e) {
            throw new ModelResolverException(
                    e.getMessage() + " (remote repositories: "
                            + session.getRemoteRepositories().stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(", "))
                            + ")",
                    groupId,
                    artifactId,
                    version,
                    e);
        }
    }
}
