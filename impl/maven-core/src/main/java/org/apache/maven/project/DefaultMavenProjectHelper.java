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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Resource;
import org.apache.maven.project.artifact.AttachedArtifact;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * DefaultMavenProjectHelper
 */
@SuppressWarnings("deprecation")
@Named
@Singleton
public class DefaultMavenProjectHelper extends AbstractLogEnabled implements MavenProjectHelper {
    private final ArtifactHandlerManager artifactHandlerManager;

    @Inject
    public DefaultMavenProjectHelper(ArtifactHandlerManager artifactHandlerManager) {
        this.artifactHandlerManager = artifactHandlerManager;
    }

    public void attachArtifact(
            MavenProject project, String artifactType, String artifactClassifier, File artifactFile) {
        ArtifactHandler handler = null;

        if (artifactType != null) {
            handler = artifactHandlerManager.getArtifactHandler(artifactType);
        }

        if (handler == null) {
            handler = artifactHandlerManager.getArtifactHandler("jar");
        }

        Artifact artifact = new AttachedArtifact(project.getArtifact(), artifactType, artifactClassifier, handler);

        artifact.setFile(artifactFile);
        artifact.setResolved(true);

        attachArtifact(project, artifact);
    }

    public void attachArtifact(MavenProject project, String artifactType, File artifactFile) {
        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(artifactType);

        Artifact artifact = new AttachedArtifact(project.getArtifact(), artifactType, handler);

        artifact.setFile(artifactFile);
        artifact.setResolved(true);

        attachArtifact(project, artifact);
    }

    public void attachArtifact(MavenProject project, File artifactFile, String artifactClassifier) {
        Artifact projectArtifact = project.getArtifact();

        Artifact artifact = new AttachedArtifact(
                projectArtifact, projectArtifact.getType(), artifactClassifier, projectArtifact.getArtifactHandler());

        artifact.setFile(artifactFile);
        artifact.setResolved(true);

        attachArtifact(project, artifact);
    }

    /**
     * Add an attached artifact or replace the file for an existing artifact.
     *
     * @see MavenProject#addAttachedArtifact(org.apache.maven.artifact.Artifact)
     * @param project project reference.
     * @param artifact artifact to add or replace.
     */
    public void attachArtifact(MavenProject project, Artifact artifact) {
        project.addAttachedArtifact(artifact);
    }

    public void addResource(
            MavenProject project, String resourceDirectory, List<String> includes, List<String> excludes) {
        Resource resource = new Resource();
        resource.setDirectory(resourceDirectory);
        resource.setIncludes(includes);
        resource.setExcludes(excludes);

        project.addResource(resource);
    }

    public void addTestResource(
            MavenProject project, String resourceDirectory, List<String> includes, List<String> excludes) {
        Resource resource = new Resource();
        resource.setDirectory(resourceDirectory);
        resource.setIncludes(includes);
        resource.setExcludes(excludes);

        project.addTestResource(resource);
    }
}
