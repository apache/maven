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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelRepositoryHolder;
import org.apache.maven.api.services.ModelResolver;
import org.apache.maven.api.services.ModelResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.internal.impl.model.ModelProblemUtils;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicyRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default artifact descriptor reader.
 */
@Named
@Singleton
public class DefaultArtifactDescriptorReader implements ArtifactDescriptorReader {
    private final RemoteRepositoryManager remoteRepositoryManager;
    private final VersionResolver versionResolver;
    private final VersionRangeResolver versionRangeResolver;
    private final ArtifactResolver artifactResolver;
    private final RepositoryEventDispatcher repositoryEventDispatcher;
    private final ModelBuilder modelBuilder;
    private final Map<String, MavenArtifactRelocationSource> artifactRelocationSources;
    private final ArtifactDescriptorReaderDelegate delegate;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public DefaultArtifactDescriptorReader(
            RemoteRepositoryManager remoteRepositoryManager,
            VersionResolver versionResolver,
            VersionRangeResolver versionRangeResolver,
            ArtifactResolver artifactResolver,
            ModelBuilder modelBuilder,
            RepositoryEventDispatcher repositoryEventDispatcher,
            Map<String, MavenArtifactRelocationSource> artifactRelocationSources) {
        this.remoteRepositoryManager =
                Objects.requireNonNull(remoteRepositoryManager, "remoteRepositoryManager cannot be null");
        this.versionResolver = Objects.requireNonNull(versionResolver, "versionResolver cannot be null");
        this.versionRangeResolver = Objects.requireNonNull(versionRangeResolver, "versionRangeResolver cannot be null");
        this.artifactResolver = Objects.requireNonNull(artifactResolver, "artifactResolver cannot be null");
        this.modelBuilder = Objects.requireNonNull(modelBuilder, "modelBuilder cannot be null");
        this.repositoryEventDispatcher =
                Objects.requireNonNull(repositoryEventDispatcher, "repositoryEventDispatcher cannot be null");
        this.artifactRelocationSources =
                Objects.requireNonNull(artifactRelocationSources, "artifactRelocationSources cannot be null");
        this.delegate = new ArtifactDescriptorReaderDelegate();
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(
            RepositorySystemSession session, ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        ArtifactDescriptorResult result = new ArtifactDescriptorResult(request);

        Model model = loadPom(session, request, result);
        if (model != null) {
            Map<String, Object> config = session.getConfigProperties();
            ArtifactDescriptorReaderDelegate delegate =
                    (ArtifactDescriptorReaderDelegate) config.get(ArtifactDescriptorReaderDelegate.class.getName());

            if (delegate == null) {
                delegate = this.delegate;
            }

            delegate.populateResult(InternalSession.from(session), result, model);
        }

        return result;
    }

    @SuppressWarnings("MethodLength")
    private Model loadPom(
            RepositorySystemSession session, ArtifactDescriptorRequest request, ArtifactDescriptorResult result)
            throws ArtifactDescriptorException {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        LinkedHashSet<String> visited = new LinkedHashSet<>();
        for (Artifact a = request.getArtifact(); ; ) {
            Artifact pomArtifact = ArtifactDescriptorUtils.toPomArtifactUnconditionally(a);
            try {
                VersionRequest versionRequest =
                        new VersionRequest(a, request.getRepositories(), request.getRequestContext());
                versionRequest.setTrace(trace);
                VersionResult versionResult = versionResolver.resolveVersion(session, versionRequest);

                a = a.setVersion(versionResult.getVersion());

                versionRequest =
                        new VersionRequest(pomArtifact, request.getRepositories(), request.getRequestContext());
                versionRequest.setTrace(trace);
                versionResult = versionResolver.resolveVersion(session, versionRequest);

                pomArtifact = pomArtifact.setVersion(versionResult.getVersion());
            } catch (VersionResolutionException e) {
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            if (!visited.add(a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getBaseVersion())) {
                RepositoryException exception =
                        new RepositoryException("Artifact relocations form a cycle: " + visited);
                invalidDescriptor(session, trace, a, exception);
                if ((getPolicy(session, a, request) & ArtifactDescriptorPolicy.IGNORE_INVALID) != 0) {
                    return null;
                }
                result.addException(exception);
                throw new ArtifactDescriptorException(result);
            }

            ArtifactResult resolveResult;
            try {
                ArtifactRequest resolveRequest =
                        new ArtifactRequest(pomArtifact, request.getRepositories(), request.getRequestContext());
                resolveRequest.setTrace(trace);
                resolveResult = artifactResolver.resolveArtifact(session, resolveRequest);
                pomArtifact = resolveResult.getArtifact();
                result.setRepository(resolveResult.getRepository());
            } catch (ArtifactResolutionException e) {
                if (e.getCause() instanceof ArtifactNotFoundException) {
                    missingDescriptor(session, trace, a, (Exception) e.getCause());
                    if ((getPolicy(session, a, request) & ArtifactDescriptorPolicy.IGNORE_MISSING) != 0) {
                        return null;
                    }
                }
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            Model model;

            // TODO hack: don't rebuild model if it was already loaded during reactor resolution
            final WorkspaceReader workspace = session.getWorkspaceReader();
            if (workspace instanceof MavenWorkspaceReader) {
                model = ((MavenWorkspaceReader) workspace).findModel(pomArtifact);
                if (model != null) {
                    return model;
                }
            }

            try {
                InternalSession iSession = InternalSession.from(session);
                List<RemoteRepository> repositories = request.getRepositories().stream()
                        .map(iSession::getRemoteRepository)
                        .toList();
                String gav =
                        pomArtifact.getGroupId() + ":" + pomArtifact.getArtifactId() + ":" + pomArtifact.getVersion();
                ModelResolver modelResolver = new DefaultModelResolver();
                ModelRepositoryHolder modelRepositoryHolder = new DefaultModelRepositoryHolder(
                        iSession, DefaultModelRepositoryHolder.RepositoryMerging.REQUEST_DOMINANT, repositories);
                ModelBuilderRequest modelRequest = ModelBuilderRequest.builder()
                        .session(iSession)
                        .projectBuild(false)
                        .processPlugins(false)
                        .twoPhaseBuilding(false)
                        .source(ModelSource.fromPath(pomArtifact.getPath(), gav))
                        // This merge is on purpose because otherwise user properties would override model
                        // properties in dependencies the user does not know. See MNG-7563 for details.
                        .systemProperties(toProperties(session.getUserProperties(), session.getSystemProperties()))
                        .userProperties(Map.of())
                        .modelResolver(modelResolver)
                        .modelRepositoryHolder(modelRepositoryHolder)
                        .repositories(repositories)
                        .build();

                ModelBuilderResult modelResult = modelBuilder.build(modelRequest);
                // ModelBuildingEx is thrown only on FATAL and ERROR severities, but we still can have WARNs
                // that may lead to unexpected build failure, log them
                if (!modelResult.getProblems().isEmpty()) {
                    List<ModelProblem> problems = modelResult.getProblems();
                    if (logger.isDebugEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(problems.size())
                                .append(" ")
                                .append((problems.size() == 1) ? "problem was" : "problems were")
                                .append(" encountered while building the effective model for ")
                                .append(request.getArtifact())
                                .append(" during ")
                                .append(RequestTraceHelper.interpretTrace(true, request.getTrace()))
                                .append("\n")
                                .append((problems.size() == 1) ? "Problem" : "Problems");
                        for (ModelProblem modelProblem : problems) {
                            sb.append("\n* ")
                                    .append(modelProblem.getMessage())
                                    .append(" @ ")
                                    .append(ModelProblemUtils.formatLocation(modelProblem, null));
                        }
                        logger.warn(sb.toString());
                    } else {
                        logger.warn(
                                "{} {} encountered while building the effective model for {} during {} (use -X to see details)",
                                problems.size(),
                                (problems.size() == 1) ? "problem was" : "problems were",
                                request.getArtifact(),
                                RequestTraceHelper.interpretTrace(false, request.getTrace()));
                    }
                }
                model = modelResult.getEffectiveModel();
            } catch (ModelBuilderException e) {
                for (ModelProblem problem : e.getResult().getProblems()) {
                    if (problem.getException() instanceof ModelResolverException) {
                        result.addException(problem.getException());
                        throw new ArtifactDescriptorException(result);
                    }
                }
                invalidDescriptor(session, trace, a, e);
                if ((getPolicy(session, a, request) & ArtifactDescriptorPolicy.IGNORE_INVALID) != 0) {
                    return null;
                }
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            Artifact relocatedArtifact = getRelocation(session, result, model);
            if (relocatedArtifact != null) {
                if (withinSameGav(relocatedArtifact, a)) {
                    result.setArtifact(relocatedArtifact);
                    return model; // they share same model
                } else {
                    result.addRelocation(a);
                    a = relocatedArtifact;
                    result.setArtifact(a);
                }
            } else {
                return model;
            }
        }
    }

    private boolean withinSameGav(Artifact a1, Artifact a2) {
        return Objects.equals(a1.getGroupId(), a2.getGroupId())
                && Objects.equals(a1.getArtifactId(), a2.getArtifactId())
                && Objects.equals(a1.getVersion(), a2.getVersion());
    }

    private Map<String, String> toProperties(Map<String, String> dominant, Map<String, String> recessive) {
        Map<String, String> props = new HashMap<>();
        if (recessive != null) {
            props.putAll(recessive);
        }
        if (dominant != null) {
            props.putAll(dominant);
        }
        return props;
    }

    private Artifact getRelocation(
            RepositorySystemSession session, ArtifactDescriptorResult artifactDescriptorResult, Model model)
            throws ArtifactDescriptorException {
        Artifact result = null;
        for (MavenArtifactRelocationSource source : artifactRelocationSources.values()) {
            result = source.relocatedTarget(session, artifactDescriptorResult, model);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    private void missingDescriptor(
            RepositorySystemSession session, RequestTrace trace, Artifact artifact, Exception exception) {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, EventType.ARTIFACT_DESCRIPTOR_MISSING);
        event.setTrace(trace);
        event.setArtifact(artifact);
        event.setException(exception);

        repositoryEventDispatcher.dispatch(event.build());
    }

    private void invalidDescriptor(
            RepositorySystemSession session, RequestTrace trace, Artifact artifact, Exception exception) {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, EventType.ARTIFACT_DESCRIPTOR_INVALID);
        event.setTrace(trace);
        event.setArtifact(artifact);
        event.setException(exception);

        repositoryEventDispatcher.dispatch(event.build());
    }

    private int getPolicy(RepositorySystemSession session, Artifact a, ArtifactDescriptorRequest request) {
        ArtifactDescriptorPolicy policy = session.getArtifactDescriptorPolicy();
        if (policy == null) {
            return ArtifactDescriptorPolicy.STRICT;
        }
        return policy.getPolicy(session, new ArtifactDescriptorPolicyRequest(a, request.getRequestContext()));
    }
}
