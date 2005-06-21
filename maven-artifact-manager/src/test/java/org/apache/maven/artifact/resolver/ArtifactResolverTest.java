package org.apache.maven.artifact.resolver;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactComponentTestCase;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.ArrayList;
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
    extends ArtifactComponentTestCase
{
    private ArtifactResolver artifactResolver;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactResolver = (ArtifactResolver) lookup( ArtifactResolver.ROLE );
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
            public Set retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
            {
                Set dependencies = new HashSet();

                if ( artifact.getArtifactId().equals( "g" ) )
                {
                    dependencies.add( createArtifact( "org.apache.maven", "h", "1.0", "jar" ) );
                }

                return dependencies;
            }
        };

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( g, remoteRepositories(),
                                                                                localRepository(), mds );

        assertEquals( 2, result.getArtifacts().size() );

        assertTrue( result.getArtifacts().containsKey( g.getId() ) );

        assertTrue( result.getArtifacts().containsKey( h.getId() ) );

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
            public Set retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
            {
                Set dependencies = new HashSet();

                if ( artifact.getArtifactId().equals( "i" ) )
                {
                    dependencies.add( createArtifact( "org.apache.maven", "j", "1.0", "jar" ) );
                }

                return dependencies;
            }
        };

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( i, remoteRepositories(),
                                                                                localRepository(), mds );

        assertEquals( 2, result.getArtifacts().size() );

        assertTrue( result.getArtifacts().containsKey( i.getId() ) );

        assertTrue( result.getArtifacts().containsKey( j.getId() ) );

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
        catch ( ArtifactResolutionException expected )
        {
            assertTrue( true );
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

