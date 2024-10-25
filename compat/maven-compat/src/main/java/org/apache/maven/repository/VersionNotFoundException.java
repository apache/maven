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
package org.apache.maven.repository;

import java.io.File;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Dependency;

/**
 * Thrown if a dependency has an invalid version.
 *
 */
@Deprecated
public class VersionNotFoundException extends Exception {
    private Dependency dependency;

    private String projectId;
    private File pomFile;
    private InvalidVersionSpecificationException cause;

    public VersionNotFoundException(
            String projectId, Dependency dependency, File pomFile, InvalidVersionSpecificationException cause) {
        super(
                projectId + ", " + formatLocationInPom(dependency) + " " + dependency.getVersion() + ", pom file "
                        + pomFile,
                cause);

        this.projectId = projectId;

        this.pomFile = pomFile;

        this.cause = cause;

        this.dependency = dependency;
    }

    private static String formatLocationInPom(Dependency dependency) {
        return "Dependency: " + ArtifactUtils.versionlessKey(dependency.getGroupId(), dependency.getArtifactId());
    }

    public Dependency getDependency() {
        return dependency;
    }

    public String getProjectId() {
        return projectId;
    }

    public File getPomFile() {
        return pomFile;
    }

    public InvalidVersionSpecificationException getCauseException() {
        return cause;
    }
}
