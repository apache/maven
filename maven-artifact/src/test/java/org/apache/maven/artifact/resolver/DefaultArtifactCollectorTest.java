package org.apache.maven.artifact.resolver;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test the default artifact collector.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultArtifactCollectorTest
    extends PlexusTestCase
{
    private ArtifactCollector artifactCollector;

    private ArtifactFactory artifactFactory;

    private ArtifactSpec projectArtifact;

    private Source source;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        this.source = new Source();
        this.artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        this.artifactCollector = new DefaultArtifactCollector();

        this.projectArtifact = createArtifact( "project", "1.0" );
    }

    public void testCircularDependencyNotIncludingCurrentProject()
        throws ArtifactResolutionException
    {
        ArtifactSpec a = createArtifact( "a", "1.0" );
        ArtifactSpec b = a.addDependency( "b", "1.0" );
        b.addDependency( "a", "1.0" );
        try
        {
            collect( a );
//            fail( "Should have failed on cyclic dependency not involving project" );
        }
        catch ( CyclicDependencyException expected )
        {
            assertTrue( true );
        }
    }

    public void testCircularDependencyIncludingCurrentProject()
        throws ArtifactResolutionException
    {
        ArtifactSpec a = createArtifact( "a", "1.0" );
        ArtifactSpec b = a.addDependency( "b", "1.0" );
        b.addDependency( "project", "1.0" );
        try
        {
            collect( a );
//            fail( "Should have failed on cyclic dependency involving project" );
        }
        catch ( CyclicDependencyException expected )
        {
            assertTrue( true );
        }
    }

    public void testResolveNearest()
        throws ArtifactResolutionException
    {
        ArtifactSpec a = createArtifact( "a", "1.0" );
        ArtifactSpec b = a.addDependency( "b", "1.0" );
        ArtifactSpec c = a.addDependency( "c", "3.0" );

        b.addDependency( "c", "2.0" );

        ArtifactResolutionResult res = collect( a );
        assertEquals( "Check artifact list",
                      new HashSet( Arrays.asList( new Object[]{a.artifact, b.artifact, c.artifact} ) ),
                      res.getArtifacts() );
    }

    private ArtifactResolutionResult collect( ArtifactSpec a )
        throws ArtifactResolutionException
    {
        return artifactCollector.collect( Collections.singleton( a.artifact ), projectArtifact.artifact, null, null,
                                          source, null, artifactFactory );
    }

    private ArtifactSpec createArtifact( String id, String version )
    {
        ArtifactSpec spec = new ArtifactSpec();
        spec.artifact = artifactFactory.createArtifact( "test", id, version, null, "jar" );
        source.artifacts.put( spec.artifact.getId(), spec );
        return spec;
    }

    private class ArtifactSpec
    {
        Artifact artifact;

        Set dependencies = new HashSet();

        public ArtifactSpec addDependency( String id, String version )
        {
            ArtifactSpec dep = createArtifact( id, version );
            dependencies.add( dep.artifact );
            return dep;
        }
    }

    private static class Source
        implements ArtifactMetadataSource
    {
        Map artifacts = new HashMap();

        public Set retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
            throws ArtifactMetadataRetrievalException, ArtifactResolutionException
        {
            ArtifactSpec a = (ArtifactSpec) artifacts.get( artifact.getId() );
            return a.dependencies;
        }
    }
}
