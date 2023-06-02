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
package org.apache.maven.plugin.coreit;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Injects artifacts from the plugin into the dependency artifacts of the project.
 *
 * @author Benjamin Bentmann
 *
 */
@Mojo(name = "inject")
public class InjectMojo extends AbstractMojo {

    /**
     * The version-less keys in the form <code>groupId:artifactId</code> of the plugin artifacts to inject into
     * dependency artifacts of the project.
     */
    @Parameter
    private String[] artifacts;

    /**
     */
    @Parameter(defaultValue = "${plugin.artifacts}", readonly = true)
    private Collection pluginArtifacts;

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The artifact factory.
     */
    @Component
    private ArtifactFactory factory;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If an error occured.
     */
    public void execute() throws MojoExecutionException {
        Set artifactKeys = new LinkedHashSet();

        if (artifacts != null) {
            artifactKeys.addAll(Arrays.asList(artifacts));
        }

        Set dependencyArtifacts = project.getDependencyArtifacts();

        if (dependencyArtifacts != null) {
            dependencyArtifacts = new LinkedHashSet(dependencyArtifacts);
        } else {
            dependencyArtifacts = new LinkedHashSet();
        }

        for (Object pluginArtifact : pluginArtifacts) {
            Artifact artifact = (Artifact) pluginArtifact;

            String artifactKey = artifact.getGroupId() + ':' + artifact.getArtifactId();

            if (artifactKeys.remove(artifactKey)) {
                artifact = factory.createArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        Artifact.SCOPE_COMPILE,
                        artifact.getType());

                getLog().info("[MAVEN-CORE-IT-LOG] Injecting dependency artifact " + artifact);

                dependencyArtifacts.add(artifact);
            }
        }

        project.setDependencyArtifacts(dependencyArtifacts);

        getLog().info("[MAVEN-CORE-IT-LOG] Set dependency artifacts to " + dependencyArtifacts);

        if (!artifactKeys.isEmpty()) {
            getLog().warn("[MAVEN-CORE-IT-LOG] These artifacts were not found " + artifactKeys);
        }
    }
}
