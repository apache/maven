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

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// It would be cool if there was a hook that i could use to setup a test environment.
// I want to setup a local/remote repositories for testing but i don't want to have
// to change them when i change the layout of the repositories. So i want to generate
// the structure i want to test by using the artifact handler manager which dictates
// the layout used for a particular artifact type.

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArtifactResolverTest
    extends AbstractArtifactComponentTestCase
{
    private ArtifactResolver artifactResolver;

    private Artifact projectArtifact;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactResolver = (ArtifactResolver) lookup( ArtifactResolver.ROLE );

        projectArtifact = createLocalArtifact( "project", "3.0" );
    }

    protected String component()
    {
        return "resolver";
    }

    public void testResolutionOfASingleArtifactWhereTheArtifactIsPresentInTheLocalRepository()
        throws Exception
    {
        Artifact a = createLocalArtifact( "a", "1.0" );

        artifactResolver.resolve( a, remoteRepositories(), localRepository() );

        assertLocalArtifactPresent( a );
    }

    public void testResolutionOfASingleArtifactWhereTheArtifactIsNotPresentLocallyAndMustBeRetrievedFromTheRemoteRepository()
        throws Exception
    {
        Artifact b = createRemoteArtifact( "b", "1.0" );
        deleteLocalArtifact( b );

        artifactResolver.resolve( b, remoteRepositories(), localRepository() );

        assertLocalArtifactPresent( b );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws Exception
    {
        // for the anonymous classes
        return super.createArtifact( groupId, artifactId, version, type );
    }

    public void testTransitiveResolutionWhereAllArtifactsArePresentInTheLocalRepository()
        throws Exception
    {
        Artifact g = createLocalArtifact( "g", "1.0" );

        Artifact h = createLocalArtifact( "h", "1.0" );

        ArtifactMetadataSource mds = new ArtifactMetadataSource()
        {
            public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                             List remoteRepositories )
                throws ArtifactMetadataRetrievalException
            {
                Set dependencies = new HashSet();

                if ( "g".equals( artifact.getArtifactId() ) )
                {
                    Artifact a = null;
                    try
                    {
                        a = createArtifact( "org.apache.maven", "h", "1.0", "jar" );
                        dependencies.add( a );
                    }
                    catch ( Exception e )
                    {
                        throw new ArtifactMetadataRetrievalException( "Error retrieving metadata", e, a );
                    }
                }

                return new ResolutionGroup( artifact, dependencies, remoteRepositories );
            }

            public List retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
                                                   List remoteRepositories )
            {
                throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
            }
        };

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( g ),
                                                                                projectArtifact, remoteRepositories(),
                                                                                localRepository(), mds );

        assertEquals( 2, result.getArtifacts().size() );

        assertTrue( result.getArtifacts().contains( g ) );

        assertTrue( result.getArtifacts().contains( h ) );

        assertLocalArtifactPresent( g );

        assertLocalArtifactPresent( h );
    }

    public void testTransitiveResolutionWhereAllArtifactsAreNotPresentInTheLocalRepositoryAndMustBeRetrievedFromTheRemoteRepository()
        throws Exception
    {
        Artifact i = createRemoteArtifact( "i", "1.0" );
        deleteLocalArtifact( i );

        Artifact j = createRemoteArtifact( "j", "1.0" );
        deleteLocalArtifact( j );

        ArtifactMetadataSource mds = new ArtifactMetadataSource()
        {
            public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                             List remoteRepositories )
                throws ArtifactMetadataRetrievalException
            {
                Set dependencies = new HashSet();

                if ( "i".equals( artifact.getArtifactId() ) )
                {
                    Artifact a = null;
                    try
                    {
                        a = createArtifact( "org.apache.maven", "j", "1.0", "jar" );
                        dependencies.add( a );
                    }
                    catch ( Exception e )
                    {
                        throw new ArtifactMetadataRetrievalException( "Error retrieving metadata", e, a );
                    }
                }

                return new ResolutionGroup( artifact, dependencies, remoteRepositories );
            }

            public List retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
                                                   List remoteRepositories )
            {
                throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
            }
        };

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( i ),
                                                                                projectArtifact, remoteRepositories(),
                                                                                localRepository(), mds );

        assertEquals( 2, result.getArtifacts().size() );

        assertTrue( result.getArtifacts().contains( i ) );

        assertTrue( result.getArtifacts().contains( j ) );

        assertLocalArtifactPresent( i );

        assertLocalArtifactPresent( j );
    }

    public void testResolutionFailureWhenArtifactNotPresentInRemoteRepository()
        throws Exception
    {
        Artifact k = createArtifact( "k", "1.0" );

        try
        {
            artifactResolver.resolve( k, remoteRepositories(), localRepository() );
            fail( "Resolution succeeded when it should have failed" );
        }
        catch ( ArtifactNotFoundException expected )
        {
            List repos = expected.getRemoteRepositories();
            assertEquals( 1, repos.size() );
            assertEquals( "test", ( (ArtifactRepository) repos.get( 0 ) ).getId() );
        }
    }

    public void testResolutionFailureWhenArtifactNotPresentInRemoteRepositoryWithMirrors()
        throws Exception
    {
        ArtifactRepository repository = remoteRepository();

        WagonManager wagonManager = (WagonManager) lookup( WagonManager.ROLE );
        wagonManager.addMirror( "mirror", "test", repository.getUrl() );

        Artifact k = createArtifact( "k", "1.0" );

        try
        {
            artifactResolver.resolve( k, Collections.singletonList( repository ), localRepository() );
            fail( "Resolution succeeded when it should have failed" );
        }
        catch ( ArtifactNotFoundException expected )
        {
            List repos = expected.getRemoteRepositories();
            assertEquals( 1, repos.size() );
            assertEquals( "mirror", ( (ArtifactRepository) repos.get( 0 ) ).getId() );
        }
    }

    public void testResolutionOfAnArtifactWhereOneRemoteRepositoryIsBadButOneIsGood()
        throws Exception
    {
        Artifact l = createRemoteArtifact( "l", "1.0" );
        deleteLocalArtifact( l );

        List repositories = new ArrayList();
        repositories.add( remoteRepository() );
        repositories.add( badRemoteRepository() );

        artifactResolver.resolve( l, repositories, localRepository() );

        assertLocalArtifactPresent( l );
    }

    /*
     public void testResolutionOfASingleArtifactWhereTheArtifactIsNotPresentLocallyAndMustBeRetrievedFromTheRemoteRepositoryAndLocalCannotBeCreated()
     throws Exception
     {
     Artifact m = createRemoteArtifact( "m", "1.0" );

     artifactResolver.resolve( m, remoteRepositories(), badLocalRepository() );

     // TODO [failing test case]: throw and handle a more informative exception
     }
     */

}

