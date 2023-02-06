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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;

// This class needs to stick around because it was exposed the the remote resources plugin started using it instead of
// getting the repositories from the project.

/**
 * ProjectUtils
 */
@Deprecated
public final class ProjectUtils {

    private ProjectUtils() {}

    public static List<ArtifactRepository> buildArtifactRepositories(
            List<Repository> repositories, ArtifactRepositoryFactory artifactRepositoryFactory, PlexusContainer c)
            throws InvalidRepositoryException {

        List<ArtifactRepository> remoteRepositories = new ArrayList<>();

        for (Repository r : repositories) {
            remoteRepositories.add(buildArtifactRepository(r, artifactRepositoryFactory, c));
        }

        return remoteRepositories;
    }

    public static ArtifactRepository buildDeploymentArtifactRepository(
            DeploymentRepository repo, ArtifactRepositoryFactory artifactRepositoryFactory, PlexusContainer c)
            throws InvalidRepositoryException {
        return buildArtifactRepository(repo, artifactRepositoryFactory, c);
    }

    public static ArtifactRepository buildArtifactRepository(
            Repository repo, ArtifactRepositoryFactory artifactRepositoryFactory, PlexusContainer c)
            throws InvalidRepositoryException {
        RepositorySystem repositorySystem = rs(c);
        RepositorySystemSession session = rss(c);

        ArtifactRepository repository = repositorySystem.buildArtifactRepository(repo);

        if (session != null) {
            repositorySystem.injectMirror(session, Arrays.asList(repository));
            repositorySystem.injectProxy(session, Arrays.asList(repository));
            repositorySystem.injectAuthentication(session, Arrays.asList(repository));
        }

        return repository;
    }

    private static RepositorySystem rs(PlexusContainer c) {
        try {
            return c.lookup(RepositorySystem.class);
        } catch (ComponentLookupException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RepositorySystemSession rss(PlexusContainer c) {
        try {
            LegacySupport legacySupport = c.lookup(LegacySupport.class);

            return legacySupport.getRepositorySession();
        } catch (ComponentLookupException e) {
            throw new IllegalStateException(e);
        }
    }
}
