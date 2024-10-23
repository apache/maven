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
package org.apache.maven.project;

import java.io.File;
import java.util.List;

/**
 * Convenience interface for plugins to add or replace artifacts and resources on projects.
 */
public interface MavenProjectHelper {
    String ROLE = MavenProjectHelper.class.getName();

    /**
     * See {@link #attachArtifact(MavenProject, String, String, java.io.File)}, but with type set to null.
     * @param project project reference.
     * @param artifactFile artifact file.
     * @param artifactClassifier artifact classifier.
     */
    void attachArtifact(MavenProject project, File artifactFile, String artifactClassifier);

    /**
     * * See {@link #attachArtifact(MavenProject, String, String, java.io.File)}, but with classifier set to null.
     * @param project project reference.
     * @param artifactType artifact type.
     * @param artifactFile artifact file.
     */
    void attachArtifact(MavenProject project, String artifactType, File artifactFile);

    /**
     * Add or replace an artifact to the current project.
     * @param project the project reference.
     * @param artifactType the type (e.g. jar) or null.
     * @param artifactClassifier the classifier or null.
     * @param artifactFile the file for the artifact.
     */
    void attachArtifact(MavenProject project, String artifactType, String artifactClassifier, File artifactFile);

    /**
     * Add a resource directory to the project.
     * @param project project reference.
     * @param resourceDirectory directory.
     * @param includes include patterns.
     * @param excludes exclude patterns.
     */
    void addResource(MavenProject project, String resourceDirectory, List<String> includes, List<String> excludes);

    /**
     * Add a test resource directory to the project.
     * @param project project reference.
     * @param resourceDirectory directory.
     * @param includes include patterns.
     * @param excludes exclude patterns.
     */
    void addTestResource(MavenProject project, String resourceDirectory, List<String> includes, List<String> excludes);
}
