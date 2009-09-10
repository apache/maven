package org.apache.maven.repository;

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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

public class MirrorProcessorTest
    extends PlexusTestCase
{
    private DefaultMirrorBuilder mirrorBuilder;
    private ArtifactRepositoryFactory repositorySystem;

    protected void setUp()
        throws Exception
    {
        mirrorBuilder = (DefaultMirrorBuilder) lookup( MirrorBuilder.class );
        repositorySystem = lookup( ArtifactRepositoryFactory.class );
        mirrorBuilder.clearMirrors();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        mirrorBuilder = null;
        super.tearDown();
    }

    public void testAddMirrorWithNullRepositoryId()
    {
        mirrorBuilder.addMirror( null, "test", "http://www.nowhere.com/", null );
    }

    public void testExternalURL()
    {
        assertTrue( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://somehost" ) ) );
        assertTrue( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://somehost:9090/somepath" ) ) );
        assertTrue( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "ftp://somehost" ) ) );
        assertTrue( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://192.168.101.1" ) ) );
        assertTrue( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://" ) ) );
        // these are local
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://localhost:8080" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://127.0.0.1:9090" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "file://localhost/somepath" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "file://localhost/D:/somepath" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://localhost" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "http://127.0.0.1" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "file:///somepath" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "file://D:/somepath" ) ) );

        // not a proper url so returns false;
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "192.168.101.1" ) ) );
        assertFalse( DefaultMirrorBuilder.isExternalRepo( getRepo( "foo", "" ) ) );
    }

    public void testMirrorLookup()
    {
        mirrorBuilder.addMirror( "a", "a", "http://a", null );
        mirrorBuilder.addMirror( "b", "b", "http://b", null );

        ArtifactRepository repo = null;
        repo = mirrorBuilder.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://c.c", repo.getUrl() );

    }

    public void testMirrorWildcardLookup()
    {
        mirrorBuilder.addMirror( "a", "a", "http://a", null );
        mirrorBuilder.addMirror( "b", "b", "http://b", null );
        mirrorBuilder.addMirror( "c", "*", "http://wildcard", null );

        ArtifactRepository repo = null;
        repo = mirrorBuilder.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://wildcard", repo.getUrl() );

    }

    public void testMirrorStopOnFirstMatch()
    {
        //exact matches win first
        mirrorBuilder.addMirror( "a2", "a,b", "http://a2", null );
        mirrorBuilder.addMirror( "a", "a", "http://a", null );
        //make sure repeated entries are skipped
        mirrorBuilder.addMirror( "a", "a", "http://a3", null );

        mirrorBuilder.addMirror( "b", "b", "http://b", null );
        mirrorBuilder.addMirror( "c", "d,e", "http://de", null );
        mirrorBuilder.addMirror( "c", "*", "http://wildcard", null );
        mirrorBuilder.addMirror( "c", "e,f", "http://ef", null );

        ArtifactRepository repo = null;
        repo = mirrorBuilder.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://wildcard", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "d", "http://d" ) );
        assertEquals( "http://de", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "e", "http://e" ) );
        assertEquals( "http://de", repo.getUrl() );

        repo = mirrorBuilder.getMirrorRepository( getRepo( "f", "http://f" ) );
        assertEquals( "http://wildcard", repo.getUrl() );

    }


    public void testPatterns()
    {
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "*," ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), ",*," ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "*," ) );

        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "a" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "a," ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), ",a," ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "a," ) );

        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "b" ), "a" ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "b" ), "a," ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "b" ), ",a" ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "b" ), ",a," ) );

        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "a,b" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "b" ), "a,b" ) );

        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "c" ), "a,b" ) );

        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "*,b" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "*,!b" ) );

        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "*,!a" ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "a" ), "!a,*" ) );

        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "c" ), "*,!a" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "c" ), "!a,*" ) );

        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "c" ), "!a,!c" ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "d" ), "!a,!c*" ) );
    }

    public void testPatternsWithExternal()
    {
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "*" ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "external:*" ) );

        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "external:*,a" ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "external:*,!a" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "a,external:*" ) );
        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "!a,external:*" ) );

        assertFalse( DefaultMirrorBuilder.matchPattern( getRepo( "c", "http://localhost" ), "!a,external:*" ) );
        assertTrue( DefaultMirrorBuilder.matchPattern( getRepo( "c", "http://somehost" ), "!a,external:*" ) );
    }

    public void testMirrorProperUrlAndProtocolAndBasedir()
    {
        mirrorBuilder.addMirror( "mirror-id", "central", "file:///tmp", null );

        List<ArtifactRepository> repos = Arrays.asList( getRepo( "central", "http://repo1.maven.org" ) );
        repos = mirrorBuilder.getMirrors( repos );

        ArtifactRepository repo = repos.get( 0 );
        assertEquals( "file:///tmp", repo.getUrl() );
        assertEquals( "file", repo.getProtocol() );
        assertEquals( File.separator + "tmp", repo.getBasedir() );
    }

    /**
     * Build an ArtifactRepository object.
     *
     * @param id
     * @param url
     * @return
     */
    private ArtifactRepository getRepo( String id, String url )
    {
        return repositorySystem.createArtifactRepository( id, url, new DefaultRepositoryLayout(), null, null );
    }

    /**
     * Build an ArtifactRepository object.
     *
     * @param id
     * @return
     */
    private ArtifactRepository getRepo( String id )
    {
        return getRepo( id, "http://something" );
    }
}
