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

import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.internal.impl.CoreUtils.cast;

public interface InternalMavenSession extends InternalSession {

    static InternalMavenSession from(Session session) {
        return cast(InternalMavenSession.class, session, "session should be an " + InternalMavenSession.class);
    }

    static InternalMavenSession from(RepositorySystemSession session) {
        return cast(InternalMavenSession.class, session.getData().get(InternalSession.class), "session");
    }

    List<Project> getProjects(List<MavenProject> projects);

    /**
     * May return null if the input project is null or is not part of the reactor.
     */
    @Nullable
    Project getProject(MavenProject project);

    List<ArtifactRepository> toArtifactRepositories(
            List<RemoteRepository> repositories);

    ArtifactRepository toArtifactRepository(RemoteRepository repository);

    MavenSession getMavenSession();
}
