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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Named
@Singleton
public class DefaultProjectDependenciesResolver implements ProjectDependenciesResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RepositorySystem repoSystem;
    private final List<RepositorySessionDecorator> decorators;

    @Inject
    public DefaultProjectDependenciesResolver(
            RepositorySystem repoSystem, List<RepositorySessionDecorator> decorators) {
        this.repoSystem = repoSystem;
        this.decorators = decorators;
    }

    public DependencyResolutionResult resolve(DependencyResolutionRequest request)
            throws DependencyResolutionException {
        final RequestTrace trace = RequestTrace.newChild(null, request);

        final DefaultDependencyResolutionResult result = new DefaultDependencyResolutionResult();

        final MavenProject project = request.getMavenProject();
        final DependencyFilter filter = request.getResolutionFilter();
        RepositorySystemSession session = request.getRepositorySession();
        ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

        if (logger.isDebugEnabled()
                && session.getConfigProperties().get(DependencyManagerUtils.CONFIG_PROP_VERBOSE) == null) {
            DefaultRepositorySystemSession verbose = new DefaultRepositorySystemSession(session);
            verbose.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, Boolean.TRUE);
            session = verbose;
        }

        for (RepositorySessionDecorator decorator : decorators) {
            RepositorySystemSession decorated = decorator.decorate(project, session);
            if (decorated != null) {
                session = decorated;
            }
        }

        CollectRequest collect = new CollectRequest();
        collect.setRootArtifact(RepositoryUtils.toArtifact(project.getArtifact()));
        collect.setRequestContext("project");
        collect.setRepositories(project.getRemoteProjectRepositories());

        if (project.getDependencyArtifacts() == null) {
            for (Dependency dependency : project.getDependencies()) {
                if (dependency.getGroupId() == null
                        || dependency.getGroupId().isEmpty()
                        || dependency.getArtifactId() == null
                        || dependency.getArtifactId().isEmpty()
                        || dependency.getVersion() == null
                        || dependency.getVersion().isEmpty()) {
                    // guard against case where best-effort resolution for invalid models is requested
                    continue;
                }
                collect.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
            }
        } else {
            Map<String, Dependency> dependencies = new HashMap<>();
            for (Dependency dependency : project.getDependencies()) {
                String classifier = dependency.getClassifier();
                if (classifier == null) {
                    ArtifactType type = stereotypes.get(dependency.getType());
                    if (type != null) {
                        classifier = type.getClassifier();
                    }
                }
                String key = ArtifactIdUtils.toVersionlessId(
                        dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), classifier);
                dependencies.put(key, dependency);
            }
            for (Artifact artifact : project.getDependencyArtifacts()) {
                String key = artifact.getDependencyConflictId();
                Dependency dependency = dependencies.get(key);
                Collection<Exclusion> exclusions = dependency != null ? dependency.getExclusions() : null;
                org.eclipse.aether.graph.Dependency dep = RepositoryUtils.toDependency(artifact, exclusions);
                if (!DependencyScope.SYSTEM.is(dep.getScope())
                        && dep.getArtifact().getFile() != null) {
                    // enable re-resolution
                    org.eclipse.aether.artifact.Artifact art = dep.getArtifact();
                    art = art.setFile(null).setVersion(art.getBaseVersion());
                    dep = dep.setArtifact(art);
                }
                collect.addDependency(dep);
            }
        }

        DependencyManagement depMgmt = project.getDependencyManagement();
        if (depMgmt != null) {
            for (Dependency dependency : depMgmt.getDependencies()) {
                collect.addManagedDependency(RepositoryUtils.toDependency(dependency, stereotypes));
            }
        }

        DependencyRequest depRequest = new DependencyRequest(collect, filter);
        depRequest.setTrace(trace);

        DependencyNode node;
        try {
            collect.setTrace(RequestTrace.newChild(trace, depRequest));
            node = repoSystem.collectDependencies(session, collect).getRoot();
            result.setDependencyGraph(node);
        } catch (DependencyCollectionException e) {
            result.setDependencyGraph(e.getResult().getRoot());
            result.setCollectionErrors(e.getResult().getExceptions());

            throw new DependencyResolutionException(
                    result, "Could not collect dependencies for project " + project.getId(), e);
        }

        depRequest.setRoot(node);

        if (logger.isWarnEnabled()) {
            for (DependencyNode child : node.getChildren()) {
                if (!child.getRelocations().isEmpty()) {
                    org.eclipse.aether.artifact.Artifact artifact =
                            child.getDependency().getArtifact();
                    String message =
                            artifact instanceof org.apache.maven.internal.impl.resolver.RelocatedArtifact relocated
                                    ? relocated.getMessage()
                                    : null;
                    logger.warn("The artifact " + child.getRelocations().get(0) + " has been relocated to " + artifact
                            + (message != null ? ": " + message : ""));
                }
            }
        }

        if (logger.isDebugEnabled()) {
            node.accept(new DependencyGraphDumper(logger::debug));
        }

        try {
            process(result, repoSystem.resolveDependencies(session, depRequest).getArtifactResults());
        } catch (org.eclipse.aether.resolution.DependencyResolutionException e) {
            process(result, e.getResult().getArtifactResults());

            throw new DependencyResolutionException(
                    result, "Could not resolve dependencies for project " + project.getId(), e);
        }

        return result;
    }

    private void process(DefaultDependencyResolutionResult result, Collection<ArtifactResult> results) {
        for (ArtifactResult ar : results) {
            DependencyNode node = ar.getRequest().getDependencyNode();
            if (ar.isResolved()) {
                result.addResolvedDependency(node.getDependency());
            } else {
                result.setResolutionErrors(node.getDependency(), ar.getExceptions());
            }
        }
    }
}
