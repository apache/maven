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
package org.apache.maven.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Repository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cache.BatchRequestException;
import org.apache.maven.api.cache.MavenExecutionException;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverRequest;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

@Named
@Singleton
public class DefaultArtifactResolver implements ArtifactResolver {

    @Override
    public ArtifactResolverResult resolve(ArtifactResolverRequest request)
            throws ArtifactResolverException, IllegalArgumentException {
        Objects.requireNonNull(request, "request cannot be null");
        InternalSession session = InternalSession.from(request.getSession());
        return session.request(request, this::doResolve);
    }

    record ResolverRequest(Session session, RequestTrace trace, ArtifactRequest request) implements Request<Session> {
        @Nonnull
        @Override
        public Session getSession() {
            return session;
        }

        @Nullable
        @Override
        public RequestTrace getTrace() {
            return trace;
        }
    }

    record ResolverResult(ResolverRequest request, ArtifactResult result) implements Result<ResolverRequest> {
        @Nonnull
        @Override
        public ResolverRequest getRequest() {
            return request;
        }
    }

    protected ArtifactResolverResult doResolve(ArtifactResolverRequest request) {
        InternalSession session = InternalSession.from(request.getSession());
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
        try {
            List<RemoteRepository> repositories = session.toRepositories(
                    request.getRepositories() != null ? request.getRepositories() : session.getRemoteRepositories());

            List<ResolverRequest> requests = new ArrayList<>();
            for (ArtifactCoordinates coords : request.getCoordinates()) {
                ArtifactRequest req = new ArtifactRequest();
                req.setRepositories(repositories);
                req.setArtifact(session.toArtifact(coords));
                req.setRequestContext(trace.context());
                req.setTrace(trace.trace());
                requests.add(new ResolverRequest(session, trace.mvnTrace(), req));
            }
            List<ResolverResult> results = session.requests(requests, list -> {
                try {
                    List<ArtifactRequest> resolverRequests =
                            list.stream().map(ResolverRequest::request).toList();
                    List<ArtifactResult> resolverResults =
                            session.getRepositorySystem().resolveArtifacts(session.getSession(), resolverRequests);
                    List<ResolverResult> res = new ArrayList<>(resolverResults.size());
                    for (int i = 0; i < resolverResults.size(); i++) {
                        res.add(new ResolverResult(list.get(i), resolverResults.get(i)));
                    }
                    return res;
                } catch (ArtifactResolutionException e) {
                    throw new MavenExecutionException(e);
                }
            });

            return toResult(request, results.stream());
        } catch (BatchRequestException e) {
            String message;
            if (e.getResults().size() == 1) {
                message = e.getResults().iterator().next().error().getMessage();
            } else {
                message = "Unable to resolve artifacts: " + e.getMessage();
            }
            throw new ArtifactResolverException(message, e, toResult(request, e));
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    ArtifactResolverResult toResult(ArtifactResolverRequest request, BatchRequestException exception) {
        return toResult(
                request,
                exception.getResults().stream()
                        .map(rr -> {
                            if (rr.result() != null) {
                                return rr.result();
                            } else if (rr.error() != null) {
                                return new ResolverResult(null, ((ArtifactResolutionException) rr.error()).getResult());
                            } else {
                                throw new IllegalStateException("Unexpected result: " + rr);
                            }
                        })
                        .map(ResolverResult.class::cast));
    }

    ArtifactResolverResult toResult(ArtifactResolverRequest request, Stream<ResolverResult> results) {
        InternalSession session = InternalSession.from(request.getSession());
        Map<ArtifactCoordinates, ArtifactResolverResult.ResultItem> items = results.map(resolverResult -> {
                    ArtifactResult result = resolverResult.result();
                    DownloadedArtifact artifact = result.getArtifact() != null
                            ? session.getArtifact(DownloadedArtifact.class, result.getArtifact())
                            : null;
                    ArtifactCoordinates coordinates = session.getArtifact(
                                    result.getRequest().getArtifact())
                            .toCoordinates();
                    Repository repository =
                            result.getRepository() != null ? session.getRepository(result.getRepository()) : null;
                    Map<Repository, List<Exception>> mappedExceptions = result.getMappedExceptions().entrySet().stream()
                            .collect(Collectors.toMap(
                                    entry -> session.getRepository(entry.getKey()), Map.Entry::getValue));
                    return new DefaultArtifactResolverResultItem(
                            coordinates,
                            artifact,
                            mappedExceptions,
                            repository,
                            result.getArtifact() != null ? result.getArtifact().getPath() : null);
                })
                .collect(Collectors.toMap(DefaultArtifactResolverResultItem::coordinates, Function.identity()));

        return new DefaultArtifactResolverResult(request, items);
    }

    record DefaultArtifactResolverResultItem(
            @Nonnull ArtifactCoordinates coordinates,
            @Nullable DownloadedArtifact artifact,
            @Nonnull Map<Repository, List<Exception>> exceptions,
            @Nullable Repository repository,
            Path path)
            implements ArtifactResolverResult.ResultItem {
        @Override
        public ArtifactCoordinates getCoordinates() {
            return coordinates;
        }

        @Override
        public DownloadedArtifact getArtifact() {
            return artifact;
        }

        @Override
        public Map<Repository, List<Exception>> getExceptions() {
            return exceptions;
        }

        @Override
        public Repository getRepository() {
            return repository;
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public boolean isResolved() {
            return getPath() != null;
        }

        @Override
        public boolean isMissing() {
            return exceptions.values().stream()
                            .flatMap(List::stream)
                            .allMatch(e -> e instanceof ArtifactNotFoundException)
                    && !isResolved();
        }
    }

    record DefaultArtifactResolverResult(
            ArtifactResolverRequest request, Map<ArtifactCoordinates, ArtifactResolverResult.ResultItem> results)
            implements ArtifactResolverResult {

        DefaultArtifactResolverResult(ArtifactResolverRequest request, Map<ArtifactCoordinates, ResultItem> results) {
            this.request = request;
            this.results = Map.copyOf(results);
        }

        @Override
        @Nonnull
        public ArtifactResolverRequest getRequest() {
            return request;
        }

        @Nonnull
        @Override
        public Collection<DownloadedArtifact> getArtifacts() {
            return results.values().stream().map(ResultItem::getArtifact).toList();
        }

        @Override
        public Path getPath(@Nonnull Artifact artifact) {
            ResultItem resultItem = results.get(artifact.toCoordinates());
            return resultItem != null ? resultItem.getPath() : null;
        }

        @Override
        public @Nonnull Map<? extends ArtifactCoordinates, ResultItem> getResults() {
            return results;
        }

        @Override
        public ResultItem getResult(ArtifactCoordinates coordinates) {
            return results.get(coordinates);
        }
    }
}
