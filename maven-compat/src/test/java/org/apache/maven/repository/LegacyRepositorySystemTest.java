package org.apache.maven.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.legacy.LegacyRepositorySystem;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

/**
 * Tests {@link LegacyRepositorySystem}.
 *
 * @author Benjamin Bentmann
 */
@PlexusTest
public class LegacyRepositorySystemTest
{
    @Inject
    private RepositorySystem repositorySystem;
    @Inject
    private ResolutionErrorHandler resolutionErrorHandler;
    @Inject
    private PlexusContainer container;

    protected List<ArtifactRepository> getRemoteRepositories()
        throws Exception
    {
        File repoDir = new File( getBasedir(), "src/test/remote-repo" ).getAbsoluteFile();

        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled( true );
        policy.setChecksumPolicy( "ignore" );
        policy.setUpdatePolicy( "always" );

        Repository repository = new Repository();
        repository.setId( RepositorySystem.DEFAULT_REMOTE_REPO_ID );
        repository.setUrl( "file://" + repoDir.toURI().getPath() );
        repository.setReleases( policy );
        repository.setSnapshots( policy );

        return Arrays.asList( repositorySystem.buildArtifactRepository( repository ) );
    }

    protected ArtifactRepository getLocalRepository()
        throws Exception
    {
        File repoDir = new File( getBasedir(), "target/local-repo" ).getAbsoluteFile();

        return repositorySystem.createLocalRepository( repoDir );
    }

    @Test
    public void testThatASystemScopedDependencyIsNotResolvedFromRepositories()
        throws Exception
    {
        //
        // We should get a whole slew of dependencies resolving this artifact transitively
        //
        Dependency d = new Dependency();
        d.setGroupId( "org.apache.maven.its" );
        d.setArtifactId( "b" );
        d.setVersion( "0.1" );
        d.setScope( Artifact.SCOPE_COMPILE );
        Artifact artifact = repositorySystem.createDependencyArtifact( d );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setResolveRoot( true )
            .setResolveTransitively( true )
            .setRemoteRepositories( getRemoteRepositories() )
            .setLocalRepository( getLocalRepository() );

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        LocalRepository localRepo = new LocalRepository( request.getLocalRepository().getBasedir() );
        session.setLocalRepositoryManager( new SimpleLocalRepositoryManagerFactory().newInstance( session, localRepo ) );
        LegacySupport legacySupport = container.lookup( LegacySupport.class );
        legacySupport.setSession( new MavenSession( container, session, new DefaultMavenExecutionRequest(),
                                                    new DefaultMavenExecutionResult() ) );

        ArtifactResolutionResult result = repositorySystem.resolve( request );
        resolutionErrorHandler.throwErrors( request, result );
        assertEquals( 2, result.getArtifacts().size() );

        //
        // System scoped version which should
        //
        d.setScope( Artifact.SCOPE_SYSTEM );
        File file = new File( getBasedir(), "src/test/repository-system/maven-core-2.1.0.jar" );
        assertTrue( file.exists() );
        d.setSystemPath( file.getCanonicalPath() );

        artifact = repositorySystem.createDependencyArtifact( d );

        //
        // The request has not set any local or remote repositories as the system scoped dependency being resolved should only
        // give us the dependency off the disk and nothing more.
        //
        request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setResolveRoot( true )
            .setResolveTransitively( true );

        result = repositorySystem.resolve( request );
        resolutionErrorHandler.throwErrors( request, result );
        assertEquals( 1, result.getArtifacts().size() );

        //
        // Put in a bogus file to make sure missing files cause the resolution to fail.
        //
        file = new File( getBasedir(), "src/test/repository-system/maven-monkey-2.1.0.jar" );
        assertFalse( file.exists() );
        d.setSystemPath( file.getCanonicalPath() );
        artifact = repositorySystem.createDependencyArtifact( d );

        //
        // The request has not set any local or remote repositories as the system scoped dependency being resolved should only
        // give us the dependency off the disk and nothing more.
        //
        request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setResolveRoot( true )
            .setResolveTransitively( true );

        try
        {
            result = repositorySystem.resolve( request );
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch( Exception e )
        {
            assertTrue( result.hasMissingArtifacts() );
        }
    }

    @Test
    public void testLocalRepositoryBasedir()
        throws Exception
    {
        File localRepoDir = new File( "" ).getAbsoluteFile();

        ArtifactRepository localRepo = repositorySystem.createLocalRepository( localRepoDir );

        String basedir = localRepo.getBasedir();

        assertFalse( basedir.endsWith( "/" ) );
        assertFalse( basedir.endsWith( "\\" ) );

        assertEquals( localRepoDir, new File( basedir ) );

        assertEquals( localRepoDir.getPath(), basedir );
    }

}
