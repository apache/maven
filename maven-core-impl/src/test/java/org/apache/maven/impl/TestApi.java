package org.apache.maven.impl;

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

import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.toolchain.DefaultToolchainManagerPrivate;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
public class TestApi
{

    Session session;

    @Inject
    RepositorySystem repositorySystem;

    @Inject
    LocalRepositoryManagerFactory localRepositoryManagerFactory;

    @Inject
    org.apache.maven.project.ProjectBuilder projectBuilder;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    @Inject
    DefaultToolchainManagerPrivate toolchainManagerPrivate;

    @BeforeEach
    void setup()
    {
        RepositorySystemSession rss = MavenRepositorySystemUtils.newSession();
        DefaultSession session = new DefaultSession( rss, repositorySystem, localRepositoryManagerFactory,
                                                     Collections.emptyList(), projectBuilder, mavenRepositorySystem,
                                                     toolchainManagerPrivate );
        DefaultLocalRepository localRepository = new DefaultLocalRepository(
                new LocalRepository( "target/repo" ) );
        org.apache.maven.api.RemoteRepository remoteRepository = session.getRemoteRepository(
                new RemoteRepository.Builder( "mirror", "default",
                        "file:target/test-classes/repo" ).build() );
        this.session = session
                .withLocalRepository( session.getService( RepositoryFactory.class ).createLocal( Paths.get( "target/repo" ) ) )
                .withLocalRepository( localRepository )
                .withRemoteRepositories( Collections.singletonList( remoteRepository ) );
    }

    @Test
    void testCreateAndResolveArtifact() throws Exception
    {
        Artifact artifact = session.createArtifact( "org.codehaus.plexus", "plexus-utils", "1.4.5", "pom" );
        assertFalse( artifact.getPath().isPresent() );

        Artifact resolved =  session.resolveArtifact( artifact ).getArtifact();
        assertNotSame( resolved, artifact );
        assertTrue( resolved.getPath().isPresent() );
        assertNotNull( resolved.getPath().get() );

        Project project = session.getService( ProjectBuilder.class ).build(
                        ProjectBuilderRequest.builder().session( session ).path( resolved.getPath().get() )
                                .processPlugins( false ).resolveDependencies( false ).build() )
                .getProject().get();
        assertNotNull( project );

        artifact = session.createArtifact( "org.codehaus.plexus", "plexus-container-default", "1.0-alpha-32", "jar" );
        Node root = session.collectDependencies( artifact ).getRoot();
        assertNotNull( root );
    }


}
