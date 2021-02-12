package org.apache.maven.artifact.resolver;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

// It would be cool if there was a hook that i could use to setup a test environment.
// I want to setup a local/remote repositories for testing but i don't want to have
// to change them when i change the layout of the repositories. So i want to generate
// the structure i want to test by using the artifact handler manager which dictates
// the layout used for a particular artifact type.

/**
 * @author Jason van Zyl
 */
public class ArtifactResolverTest
    extends AbstractArtifactComponentTestCase
{
    @Inject
    private ArtifactResolver artifactResolver;

    private Artifact projectArtifact;

    @BeforeEach
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        projectArtifact = createLocalArtifact( "project", "3.0" );
    }

    @Override
    protected String component()
    {
        return "resolver";
    }

    @Test
    public void testResolutionOfASingleArtifactWhereTheArtifactIsPresentInTheLocalRepository()
        throws Exception
    {
        Artifact a = createLocalArtifact( "a", "1.0" );

        artifactResolver.resolve( a, remoteRepositories(), localRepository() );

        assertLocalArtifactPresent( a );
    }

    @Test
    public void testResolutionOfASingleArtifactWhereTheArtifactIsNotPresentLocallyAndMustBeRetrievedFromTheRemoteRepository()
        throws Exception
    {
        Artifact b = createRemoteArtifact( "b", "1.0-SNAPSHOT" );
        deleteLocalArtifact( b );
        artifactResolver.resolve( b, remoteRepositories(), localRepository() );
        assertLocalArtifactPresent( b );
    }

    @Override
    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws Exception
    {
        // for the anonymous classes
        return super.createArtifact( groupId, artifactId, version, type );
    }

    @Test
    public void testTransitiveResolutionWhereAllArtifactsArePresentInTheLocalRepository()
        throws Exception
    {
        Artifact g = createLocalArtifact( "g", "1.0" );

        Artifact h = createLocalArtifact( "h", "1.0" );

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( g ), projectArtifact, remoteRepositories(), localRepository(), null );

        printErrors( result );

        assertEquals( 2, result.getArtifacts().size() );

        assertTrue( result.getArtifacts().contains( g ) );

        assertTrue( result.getArtifacts().contains( h ) );

        assertLocalArtifactPresent( g );

        assertLocalArtifactPresent( h );
    }

    @Test
    public void testTransitiveResolutionWhereAllArtifactsAreNotPresentInTheLocalRepositoryAndMustBeRetrievedFromTheRemoteRepository()
        throws Exception
    {
        Artifact i = createRemoteArtifact( "i", "1.0-SNAPSHOT" );
        deleteLocalArtifact( i );

        Artifact j = createRemoteArtifact( "j", "1.0-SNAPSHOT" );
        deleteLocalArtifact( j );

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( i ), projectArtifact, remoteRepositories(), localRepository(), null );

        printErrors( result );

        assertEquals( 2, result.getArtifacts().size() );

        assertTrue( result.getArtifacts().contains( i ) );

        assertTrue( result.getArtifacts().contains( j ) );

        assertLocalArtifactPresent( i );

        assertLocalArtifactPresent( j );
    }

    @Test
    public void testResolutionFailureWhenArtifactNotPresentInRemoteRepository()
        throws Exception
    {
        Artifact k = createArtifact( "k", "1.0" );

        assertThrows(
                ArtifactNotFoundException.class,
                () -> artifactResolver.resolve( k, remoteRepositories(), localRepository() ),
                "Resolution succeeded when it should have failed" );
    }

    @Test
    public void testResolutionOfAnArtifactWhereOneRemoteRepositoryIsBadButOneIsGood()
        throws Exception
    {
        Artifact l = createRemoteArtifact( "l", "1.0-SNAPSHOT" );
        deleteLocalArtifact( l );

        List<ArtifactRepository> repositories = new ArrayList<>();
        repositories.add( remoteRepository() );
        repositories.add( badRemoteRepository() );

        artifactResolver.resolve( l, repositories, localRepository() );

        assertLocalArtifactPresent( l );
    }

    @Test
    public void testTransitiveResolutionOrder()
        throws Exception
    {
        Artifact m = createLocalArtifact( "m", "1.0" );

        Artifact n = createLocalArtifact( "n", "1.0" );

        ArtifactMetadataSource mds = new ArtifactMetadataSource()
        {
            public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                             List<ArtifactRepository> remoteRepositories )
            {
                Set<Artifact> dependencies = new HashSet<>();

                return new ResolutionGroup( artifact, dependencies, remoteRepositories );
            }

            public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact,
                                                                    ArtifactRepository localRepository,
                                                                    List<ArtifactRepository> remoteRepositories )
            {
                throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
            }

            public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
                                                                                            Artifact artifact,
                                                                                            ArtifactRepository localRepository,
                                                                                            ArtifactRepository remoteRepository )
            {
                throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
            }

            public ResolutionGroup retrieve( MetadataResolutionRequest request )
            {
                return retrieve( request.getArtifact(), request.getLocalRepository(), request.getRemoteRepositories() );
            }

            public List<ArtifactVersion> retrieveAvailableVersions( MetadataResolutionRequest request )
            {
                return retrieveAvailableVersions( request.getArtifact(), request.getLocalRepository(), request.getRemoteRepositories() );
            }
        };

        ArtifactResolutionResult result = null;

        Set<Artifact> set = new LinkedHashSet<>();
        set.add( n );
        set.add( m );

        result =
            artifactResolver.resolveTransitively( set, projectArtifact, remoteRepositories(), localRepository(), mds );

        printErrors( result );

        Iterator<Artifact> i = result.getArtifacts().iterator();
        assertEquals( n, i.next(), "n should be first" );
        assertEquals( m, i.next(), "m should be second" );

        // inverse order
        set = new LinkedHashSet<>();
        set.add( m );
        set.add( n );

        result =
            artifactResolver.resolveTransitively( set, projectArtifact, remoteRepositories(), localRepository(), mds );

        printErrors( result );

        i = result.getArtifacts().iterator();
        assertEquals( m, i.next(), "m should be first" );
        assertEquals( n, i.next(), "n should be second" );
    }

    private void printErrors( ArtifactResolutionResult result )
    {
        if ( result.hasMissingArtifacts() )
        {
            for ( Artifact artifact : result.getMissingArtifacts() )
            {
                System.err.println( "Missing: " + artifact );
            }
        }

        if ( result.hasExceptions() )
        {
            for ( Exception e : result.getExceptions() )
            {
                e.printStackTrace();
            }
        }
    }

}
