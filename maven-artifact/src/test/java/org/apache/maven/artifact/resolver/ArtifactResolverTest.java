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

import org.apache.maven.artifact.ArtifactComponentTestCase;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */

// It would be cool if there was a hook that i could use to setup a test environment.
// I want to setup a local/remote repositories for testing but i don't want to have
// to change them when i change the layout of the repositories. So i want to generate
// the structure i want to test by using the artifact handler manager which dictates
// the layout used for a particular artifact type.

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
        Artifact b = createLocalArtifact( "b", "1.0" );

        artifactResolver.resolve( b, remoteRepositories(), localRepository() );

        assertLocalArtifactPresent( b );
    }

    public void testResolutionOfASetOfArtifactsWhereTheArtifactsArePresentInTheLocalRepository()
        throws Exception
    {
        Set artifacts = new HashSet();

        Artifact c = createLocalArtifact( "c", "1.0" );

        Artifact d = createLocalArtifact( "d", "1.0" );

        artifacts.add( c );

        artifacts.add( d );

        Set resolvedArtifacts = artifactResolver.resolve( artifacts, remoteRepositories(), localRepository() );

        assertEquals( 2, resolvedArtifacts.size() );

        // The artifacts have undergone no transformations and they are present so the original
        // artifacts sent into the resolver should be returned as they were sent in.

        assertTrue( resolvedArtifacts.contains( c ) );

        assertTrue( resolvedArtifacts.contains( d ) );
    }

    public void testResolutionOfASetOfArtifactsWhereTheArtifactsAreNotPresentInTheLocalRepositoryAndMustBeRetrievedFromTheRemoteRepository()
        throws Exception
    {
        Set artifacts = new HashSet();

        Artifact e = createRemoteArtifact( "e", "1.0" );

        Artifact f = createRemoteArtifact( "f", "1.0" );

        artifacts.add( e );

        artifacts.add( f );

        Set resolvedArtifacts = artifactResolver.resolve( artifacts, remoteRepositories(), localRepository() );

        assertEquals( 2, resolvedArtifacts.size() );

        // The artifacts have undergone no transformations and they are present so the original
        // artifacts sent into the resolver should be returned as they were sent in.

        assertTrue( resolvedArtifacts.contains( e ) );

        assertTrue( resolvedArtifacts.contains( f ) );
    }


    public void testTransitiveResolutionWhereAllArtifactsArePresentInTheLocalRepository()
        throws Exception
    {
        Artifact g = createLocalArtifact( "g", "1.0" );

        Artifact h = createLocalArtifact( "h", "1.0" );

        ArtifactMetadataSource mds = new ArtifactMetadataSource()
        {
            public Set retrieve( Artifact artifact )
                throws ArtifactMetadataRetrievalException
            {
                Set dependencies = new HashSet();

                if ( artifact.getArtifactId().equals( "g" ) )
                {
                    try
                    {
                        dependencies.add( new DefaultArtifact( "maven", "h", "1.0", "jar" ) );
                    }
                    catch ( Exception e )
                    {
                       throw new ArtifactMetadataRetrievalException( "Cannot retrieve metadata." );
                    }
                }

                return dependencies;
            }
        };

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( g,
                                                                                remoteRepositories(),
                                                                                localRepository(),
                                                                                mds );

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

        Artifact j = createRemoteArtifact( "j", "1.0" );

        ArtifactMetadataSource mds = new ArtifactMetadataSource()
        {
            public Set retrieve( Artifact artifact )
                throws ArtifactMetadataRetrievalException
            {
                Set dependencies = new HashSet();

                if ( artifact.getArtifactId().equals( "i" ) )
                {
                    try
                    {
                        dependencies.add( new DefaultArtifact( "maven", "j", "1.0", "jar" ) );
                    }
                    catch ( Exception e )
                    {
                       throw new ArtifactMetadataRetrievalException( "Cannot retrieve metadata." );
                    }
                }

                return dependencies;
            }
        };

        ArtifactResolutionResult result = artifactResolver.resolveTransitively( i,
                                                                                remoteRepositories(),
                                                                                localRepository(),
                                                                                mds );

        assertEquals( 2, result.getArtifacts().size() );

        assertTrue( result.getArtifacts().containsKey( i.getId() ) );

        assertTrue( result.getArtifacts().containsKey( j.getId() ) );

        assertLocalArtifactPresent( i );

        assertLocalArtifactPresent( j );
    }
}