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
package org.apache.maven.interpolation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.maven.api.Session;
import org.apache.maven.api.model.Model;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.AbstractSession;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singleton;

/**
 * Enables to resolve a <b>direct</b> dependency path using <code>${project.dependencies.gav}</code> syntax.
 */
// dev note: could be a InterpolationPostProcessor but not strong reason as of today
@Named
@Singleton
public class ResolverValueSource extends AbstractValueSource {
    private final Provider<RepositorySystem> repositorySystemProvider;
    private final Provider<MavenSession> mavenSessionProvider;
    private Supplier<Model> modelSupplier;

    @Inject
    public ResolverValueSource(
            Provider<RepositorySystem> repositorySystemProvider, Provider<MavenSession> mavenSessionProvider) {
        super(false);
        this.repositorySystemProvider = repositorySystemProvider;
        this.mavenSessionProvider = mavenSessionProvider;
    }

    @Override
    public Object getValue(final String expression) {
        if (!expression.startsWith("project.dependencies.") || modelSupplier == null) {
            return null;
        }
        final String gav = expression.substring("project.dependencies.".length());
        try {
            final MavenSession mavenSession = mavenSessionProvider.get();
            final Artifact artifact = findArtifact(gav, modelSupplier.get());
            if (artifact == null) {
                LoggerFactory.getLogger(getClass().getName()) // lazy
                        .error("Can't resolve '" + gav + "', didn't find the dependency");
                return null;
            }

            final Session session = mavenSession.getSession();
            if (!(session instanceof AbstractSession)) {
                return null;
            }

            final AbstractSession as = (AbstractSession) session;
            final List<RemoteRepository> repositories = as.toRepositories(session.getRemoteRepositories());
            final ArtifactRequest request = new ArtifactRequest(artifact, repositories, null);
            final List<ArtifactResult> results = repositorySystemProvider
                    .get()
                    .resolveArtifacts(mavenSession.getRepositorySession(), singleton(request));
            for (final ArtifactResult result : results) {
                final File file = result.getArtifact().getFile();
                if (file != null) {
                    return file.getAbsolutePath();
                }
            }
        } catch (final RuntimeException | ArtifactResolutionException e) {
            LoggerFactory.getLogger(getClass().getName()) // lazy
                    .error("Can't resolve '" + gav + "'", e);
        }
        return null;
    }

    private Artifact findArtifact(final String gav, final Model model) {
        if (model == null) {
            return null;
        }
        // warn: for now it MUST be a *direct* dependency, ideally we would use currentProject but not set there
        return model.getDependencies().stream()
                .filter(
                        it -> { // this is sufficient due to the number of reference as of *today* and fast enough
                            if (!(gav.startsWith(it.getGroupId())
                                    && gav.length() > it.getGroupId().length()
                                    && gav.charAt(it.getGroupId().length()) == '.')) {
                                return false;
                            }

                            final String sub1 = gav.substring(it.getGroupId().length() + 1);
                            if (Objects.equals(sub1, it.getArtifactId())) {
                                return true;
                            }

                            if (it.getClassifier() == null || it.getClassifier().isEmpty()) {
                                return false;
                            }

                            if (!(sub1.startsWith(it.getArtifactId())
                                    && sub1.length() > it.getArtifactId().length()
                                    && sub1.charAt(it.getArtifactId().length()) == '.')) {
                                return false;
                            }

                            return sub1.substring(it.getArtifactId().length() + 1)
                                    .equals(it.getClassifier());
                        })
                .findFirst()
                .map(it -> new DefaultArtifact(
                        it.getGroupId(), it.getArtifactId(), it.getClassifier(), it.getType(), it.getVersion()))
                .orElse(null);
    }

    public void setModelProvider(final Supplier<Model> modelSupplier) {
        this.modelSupplier = modelSupplier;
    }
}
