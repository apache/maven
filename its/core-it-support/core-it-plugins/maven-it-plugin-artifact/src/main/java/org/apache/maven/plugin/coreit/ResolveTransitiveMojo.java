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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Resolves user-specified artifacts transitively. As an additional exercise, the resolution happens in a forked thread
 * to test access to any shared session state.
 *
 * @author Benjamin Bentmann
 */
@Mojo(name = "resolve-transitive")
public class ResolveTransitiveMojo extends AbstractMojo {

    /**
     * The local repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    /**
     * The remote repositories of the current Maven project.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List remoteRepositories;

    /**
     * The artifact resolver.
     *
     */
    @Component
    private ArtifactResolver resolver;

    /**
     * The artifact factory.
     *
     */
    @Component
    private ArtifactFactory factory;

    /**
     * The metadata source.
     *
     */
    @Component
    private ArtifactMetadataSource metadataSource;

    /**
     * The dependencies to resolve.
     *
     */
    @Parameter
    private Dependency[] dependencies;

    /**
     * The path to a properties file to store the resolved artifact paths in.
     *
     */
    @Parameter
    private File propertiesFile;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the artifacts couldn't be resolved.
     */
    public void execute() throws MojoExecutionException {
        getLog().info("[MAVEN-CORE-IT-LOG] Resolving artifacts");

        ResolverThread thread = new ResolverThread();
        thread.start();
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (thread.error != null) {
            throw new MojoExecutionException("Failed to resolve artifacts: " + thread.error.getMessage(), thread.error);
        }

        if (propertiesFile != null) {
            getLog().info("[MAVEN-CORE-IT-LOG] Creating properties file " + propertiesFile);

            try {
                propertiesFile.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
                    thread.props.store(fos, "MAVEN-CORE-IT");
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to create properties file: " + e.getMessage(), e);
            }
        }
    }

    private String getId(Artifact artifact) {
        artifact.isSnapshot(); // decouple from MNG-2961
        return artifact.getId();
    }

    class ResolverThread extends Thread {

        Properties props = new Properties();

        Exception error;

        public void run() {
            if (dependencies != null) {
                try {
                    Set artifacts = new LinkedHashSet();

                    for (Dependency dependency : dependencies) {
                        Artifact artifact = factory.createArtifactWithClassifier(
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion(),
                                dependency.getType(),
                                dependency.getClassifier());

                        getLog().info("[MAVEN-CORE-IT-LOG] Resolving " + ResolveTransitiveMojo.this.getId(artifact));

                        artifacts.add(artifact);
                    }

                    Artifact origin = factory.createArtifact("it", "it", "0.1", null, "pom");

                    artifacts = resolver.resolveTransitively(
                                    artifacts, origin, remoteRepositories, localRepository, metadataSource)
                            .getArtifacts();

                    for (Object artifact1 : artifacts) {
                        Artifact artifact = (Artifact) artifact1;

                        if (artifact.getFile() != null) {
                            props.setProperty(
                                    ResolveTransitiveMojo.this.getId(artifact),
                                    artifact.getFile().getPath());
                        }

                        getLog().info("[MAVEN-CORE-IT-LOG]   " + artifact.getFile());
                    }
                } catch (Exception e) {
                    error = e;
                }
            }
        }
    }
}
