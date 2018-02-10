package org.apache.maven;

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

import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * @deprecated As of 3.2.2, and there is no direct replacement. This is an internal class which was not marked as such,
 *             but should have been.
 * @author jvanzyl
 *
 */
@Deprecated
public interface ProjectDependenciesResolver
{

    /**
     * Resolves the transitive dependencies of the specified project.
     *
     * @param project         The project whose dependencies should be resolved, must not be {@code null}.
     * @param scopesToResolve The dependency scopes that should be resolved, may be {@code null}.
     * @param session         The current build session, must not be {@code null}.
     * @return The transitive dependencies of the specified project that match the requested scopes, never {@code null}.
     */
    Set<Artifact> resolve( MavenProject project, Collection<String> scopesToResolve, MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    /**
     * Resolves the transitive dependencies of the specified project.
     *
     * @param project         The project whose dependencies should be resolved, must not be {@code null}.
     * @param scopesToCollect The dependency scopes that should be collected, may be {@code null}.
     * @param scopesToResolve The dependency scopes that should be collected and also resolved, may be {@code null}.
     * @param session         The current build session, must not be {@code null}.
     * @return The transitive dependencies of the specified project that match the requested scopes, never {@code null}.
     */
    Set<Artifact> resolve( MavenProject project, Collection<String> scopesToCollect,
                           Collection<String> scopesToResolve, MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    /**
     * Resolves the transitive dependencies of the specified project.
     *
     * @param project             The project whose dependencies should be resolved, must not be {@code null}.
     * @param scopesToCollect     The dependency scopes that should be collected, may be {@code null}.
     * @param scopesToResolve     The dependency scopes that should be collected and also resolved, may be {@code null}.
     * @param session             The current build session, must not be {@code null}.
     * @param ignoreableArtifacts Artifacts that need not be resolved
     * @return The transitive dependencies of the specified project that match the requested scopes, never {@code null}.
     */
    Set<Artifact> resolve( MavenProject project, Collection<String> scopesToCollect,
                           Collection<String> scopesToResolve, MavenSession session, Set<Artifact> ignoreableArtifacts )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    /**
     * Resolves the transitive dependencies of the specified projects. Note that dependencies which can't be resolved
     * from any repository but are present among the set of specified projects will not cause an exception. Instead,
     * those unresolved artifacts will be returned in the result set, allowing the caller to take special care of
     * artifacts that haven't been build yet.
     *
     * @param projects The projects whose dependencies should be resolved, may be {@code null}.
     * @param scopes   The dependency scopes that should be resolved, may be {@code null}.
     * @param session  The current build session, must not be {@code null}.
     * @return The transitive dependencies of the specified projects that match the requested scopes, never
     *         {@code null}.
     */
    Set<Artifact> resolve( Collection<? extends MavenProject> projects, Collection<String> scopes,
                           MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException;

}
