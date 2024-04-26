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
package org.apache.maven.internal.impl;

import java.util.Collection;
import java.util.List;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.DependencyCollector;
import org.apache.maven.api.services.DependencyCollectorException;
import org.apache.maven.api.services.DependencyCollectorRequest;
import org.apache.maven.api.services.DependencyCollectorResult;
import org.apache.maven.api.services.ProjectManager;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultDependencyCollector implements DependencyCollector {

    @Nonnull
    @Override
    public DependencyCollectorResult collect(@Nonnull DependencyCollectorRequest request)
            throws DependencyCollectorException, IllegalArgumentException {
        nonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());

        Artifact rootArtifact;
        DependencyCoordinate root;
        Collection<DependencyCoordinate> dependencies;
        Collection<DependencyCoordinate> managedDependencies;
        List<RemoteRepository> remoteRepositories;
        if (request.getProject().isPresent()) {
            Project project = request.getProject().get();
            rootArtifact = project.getPomArtifact();
            root = null;
            dependencies = project.getDependencies();
            managedDependencies = project.getManagedDependencies();
            remoteRepositories = session.getService(ProjectManager.class).getRemoteProjectRepositories(project);
        } else {
            rootArtifact = request.getRootArtifact().orElse(null);
            root = request.getRoot().orElse(null);
            dependencies = request.getDependencies();
            managedDependencies = request.getManagedDependencies();
            remoteRepositories = session.getRemoteRepositories();
        }
        CollectRequest collectRequest = new CollectRequest()
                .setRootArtifact(rootArtifact != null ? session.toArtifact(rootArtifact) : null)
                .setRoot(root != null ? session.toDependency(root, false) : null)
                .setDependencies(session.toDependencies(dependencies, false))
                .setManagedDependencies(session.toDependencies(managedDependencies, true))
                .setRepositories(session.toRepositories(remoteRepositories));

        RepositorySystemSession systemSession = session.getSession();
        if (request.getVerbose()) {
            systemSession = new DefaultRepositorySystemSession(systemSession)
                    .setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true)
                    .setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        }

        try {
            final CollectResult result =
                    session.getRepositorySystem().collectDependencies(systemSession, collectRequest);
            return new DependencyCollectorResult() {
                @Override
                public List<Exception> getExceptions() {
                    return result.getExceptions();
                }

                @Override
                public Node getRoot() {
                    return session.getNode(result.getRoot(), request.getVerbose());
                }
            };
        } catch (DependencyCollectionException e) {
            throw new DependencyCollectorException("Unable to collect dependencies", e);
        }
    }
}
