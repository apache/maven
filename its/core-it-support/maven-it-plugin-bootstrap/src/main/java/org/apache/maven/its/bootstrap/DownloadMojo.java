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
package org.apache.maven.its.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;

/**
 * Boostrap plugin to download all required dependencies (provided in file) or to collect lifecycle bound build plugin
 * versions.
 */
@Mojo(name = "download")
public class DownloadMojo extends AbstractMojo {

    /**
     * A list of artifacts coordinates.
     */
    @Parameter
    private Set<Dependency> dependencies = new HashSet<>();

    /**
     * A list of string of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter
    private Set<String> artifacts = new HashSet<>();

    /**
     * A file containing lines of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter
    private File file;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoFailureException {
        // this or that: either resolver file listed artifacts or collect lifecycle packaging plugins
        if (file != null && file.exists()) {
            System.out.println("Collecting artifacts from file: " + file);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                reader.lines()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                        .forEach(artifacts::add);
            } catch (IOException e) {
                throw new MojoFailureException("Unable to read dependencies: " + file, e);
            }
        } else {
            MavenProject project = session.getCurrentProject();
            System.out.println("Collecting build plugins from packaging: " + project.getPackaging());
            for (Plugin plugin : project.getBuildPlugins()) {
                artifacts.add(plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
            }
        }

        for (String artifact : artifacts) {
            if (artifact != null) {
                dependencies.add(toDependency(artifact));
            }
        }

        ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
        RepositorySystemSession repositorySystemSession = projectBuildingRequest.getRepositorySession();
        List<RemoteRepository> repos = RepositoryUtils.toRepos(projectBuildingRequest.getRemoteRepositories());

        for (Dependency dependency : dependencies) {
            try {
                org.eclipse.aether.graph.Dependency root =
                        RepositoryUtils.toDependency(dependency, repositorySystemSession.getArtifactTypeRegistry());
                CollectRequest collectRequest = new CollectRequest(root, null, repos);
                collectRequest.setRequestContext("bootstrap");
                DependencyRequest request = new DependencyRequest(collectRequest, null);
                System.out.println("Resolving: " + root.getArtifact());
                repositorySystem.resolveDependencies(repositorySystemSession, request);
            } catch (Exception e) {
                throw new MojoFailureException("Unable to resolve dependency: " + dependency, e);
            }
        }
    }

    static Dependency toDependency(String artifact) throws MojoFailureException {
        Dependency coordinate = new Dependency();
        String[] tokens = artifact.split(":");
        if (tokens.length < 3 || tokens.length > 5) {
            throw new MojoFailureException("Invalid artifact, you must specify "
                    + "groupId:artifactId:version[:packaging[:classifier]] " + artifact);
        }
        coordinate.setGroupId(tokens[0]);
        coordinate.setArtifactId(tokens[1]);
        coordinate.setVersion(tokens[2]);
        if (tokens.length >= 4) {
            coordinate.setType(tokens[3]);
        }
        if (tokens.length == 5) {
            coordinate.setClassifier(tokens[4]);
        }
        return coordinate;
    }
}
