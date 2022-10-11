package org.apache.maven.internal.impl;

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

import javax.inject.Inject;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.toolchain.DefaultToolchainManagerPrivate;
import org.apache.maven.toolchain.building.ToolchainsBuilder;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
public class TestApi
{

    Session session;

    @Inject
    RepositorySystem repositorySystem;

    @Inject
    org.apache.maven.project.ProjectBuilder projectBuilder;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    @Inject
    DefaultToolchainManagerPrivate toolchainManagerPrivate;

    @Inject
    PlexusContainer plexusContainer;

    @Inject
    MojoExecutionScope mojoExecutionScope;

    @Inject
    RuntimeInformation runtimeInformation;

    @Inject
    ArtifactHandlerManager artifactHandlerManager;

    @Inject
    SessionScope sessionScope;

    @BeforeEach
    void setup()
    {
        RepositorySystemSession rss = MavenRepositorySystemUtils.newSession();
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        MavenSession ms = new MavenSession( null, rss, mer, null );
        DefaultSession session = new DefaultSession( ms, repositorySystem,
                                                     Collections.emptyList(),
                                                     mavenRepositorySystem,
                                                     plexusContainer,
                                                     runtimeInformation );
        DefaultLocalRepository localRepository = new DefaultLocalRepository(
                new LocalRepository( "target/test-classes/apiv4-repo" ) );
        org.apache.maven.api.RemoteRepository remoteRepository = session.getRemoteRepository(
                new RemoteRepository.Builder( "mirror", "default",
                        "file:target/test-classes/repo" ).build() );
        this.session = session
                .withLocalRepository( localRepository )
                .withRemoteRepositories( Collections.singletonList( remoteRepository ) );

        sessionScope.enter();
        sessionScope.seed( DefaultSession.class, (DefaultSession) this.session );
    }

    @Test
    void testCreateAndResolveArtifact() throws Exception
    {
        ArtifactCoordinate coord =
                session.createArtifactCoordinate( "org.codehaus.plexus", "plexus-utils", "1.4.5", "pom" );

        Artifact resolved = session.resolveArtifact( coord );
        Optional<Path> op = session.getArtifactPath( resolved );
        assertTrue( op.isPresent() );
        assertNotNull( op.get() );

        Project project = session.getService( ProjectBuilder.class ).build(
                        ProjectBuilderRequest.builder().session( session ).path( op.get() )
                                .processPlugins( false ).resolveDependencies( false ).build() )
                .getProject().get();
        assertNotNull( project );

        Artifact artifact =
                session.createArtifact( "org.codehaus.plexus", "plexus-container-default", "1.0-alpha-32", "jar" );
        Node root = session.collectDependencies( artifact );
        assertNotNull( root );
    }


}
