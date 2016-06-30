package org.apache.maven.project.artifact;

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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.project.artifact.DefaultMavenMetadataCache.CacheKey;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author Igor Fedorenko
 */
public class DefaultMavenMetadataCacheTest
    extends PlexusTestCase
{
    private RepositorySystem repositorySystem;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        repositorySystem = lookup( RepositorySystem.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        repositorySystem = null;
        super.tearDown();
    }

    @SuppressWarnings( "deprecation" )
    public void testCacheKey()
        throws Exception
    {
        Artifact a1 = repositorySystem.createArtifact( "testGroup", "testArtifact", "1.2.3", "jar" );
        ArtifactRepository lr1 =
            new DelegatingLocalArtifactRepository( repositorySystem.createDefaultLocalRepository() );

        ArtifactRepository rr1 = new MavenArtifactRepository( "test",
                                                              "file://" + new File( System.getProperty( "basedir", "" ),
                                                                                    "src/test/remote-repo" ).toURI().
                                                              getPath(),
                                                              new DefaultRepositoryLayout(),
                                                              new ArtifactRepositoryPolicy(),
                                                              new ArtifactRepositoryPolicy() );

        a1.setDependencyFilter( new ExcludesArtifactFilter( Arrays.asList( "foo" ) ) );

        Artifact a2 = repositorySystem.createArtifact( "testGroup", "testArtifact", "1.2.3", "jar" );
        ArtifactRepository lr2 =
            new DelegatingLocalArtifactRepository( repositorySystem.createDefaultLocalRepository() );

        ArtifactRepository rr2 = new MavenArtifactRepository( "test",
                                                              "file://" + new File( System.getProperty( "basedir", "" ),
                                                                                    "src/test/remote-repo" ).toURI().
                                                              getPath(),
                                                              new DefaultRepositoryLayout(),
                                                              new ArtifactRepositoryPolicy(),
                                                              new ArtifactRepositoryPolicy() );

        a2.setDependencyFilter( new ExcludesArtifactFilter( Arrays.asList( "foo" ) ) );

        // sanity checks
        assertNotSame( a1, a2 );
        assertNotSame( lr1, lr2 );
        assertNotSame( rr1, rr2 );

        CacheKey k1 = new CacheKey( a1, false, lr1, Collections.singletonList( rr1 ) );
        CacheKey k2 = new CacheKey( a2, false, lr2, Collections.singletonList( rr2 ) );

        assertEquals( k1.hashCode(), k2.hashCode() );
    }

}
