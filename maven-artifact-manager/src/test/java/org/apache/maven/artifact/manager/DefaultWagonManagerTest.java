package org.apache.maven.artifact.manager;

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
import java.io.IOException;
import java.util.Date;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.easymock.MockControl;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class DefaultWagonManagerTest
    extends PlexusTestCase
{
    private DefaultWagonManager wagonManager;

    private TransferListener transferListener = new Debug();

    private ArtifactFactory artifactFactory;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        wagonManager = (DefaultWagonManager) lookup( WagonManager.ROLE );
        
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    private Artifact createTestPomArtifact( String directory )
        throws IOException
    {
        File testData = getTestFile( directory );
        FileUtils.deleteDirectory( testData );
        testData.mkdirs();

        Artifact artifact = artifactFactory.createProjectArtifact( "test", "test", "1.0" );
        artifact.setFile( new File( testData, "test-1.0.pom" ) );
        assertFalse( artifact.getFile().exists() );
        return artifact;
    }
    
    private Artifact createTestArtifact( String directory, String type )
        throws IOException
    {
        File testData = getTestFile( directory );
        FileUtils.deleteDirectory( testData );
        testData.mkdirs();

        Artifact artifact = artifactFactory.createBuildArtifact( "test", "test", "1.0", type );
        artifact.setFile( new File( testData, "test-1.0." + artifact.getArtifactHandler().getExtension() ) );
        assertFalse( artifact.getFile().exists() );
        return artifact;
    }
    
    public void testGetArtifactSha1MissingMd5Present()
        throws IOException, UnsupportedProtocolException, TransferFailedException, ResourceDoesNotExistException
    {
        Artifact artifact = createTestPomArtifact( "target/test-data/get-remote-artifact" );

        ArtifactRepository repo = createStringRepo();

        StringWagon wagon = (StringWagon) wagonManager.getWagon( "string" );
        wagon.addExpectedContent( repo.getLayout().pathOf( artifact ), "expected" );
        wagon.addExpectedContent( repo.getLayout().pathOf( artifact ) + ".md5", "bad_checksum" );
        
        wagonManager.getArtifact( artifact, repo );

        assertTrue( artifact.getFile().exists() );
    }

    private ArtifactRepository createStringRepo()
    {
        ArtifactRepository repo =
            new DefaultArtifactRepository( "id", "string://url", new ArtifactRepositoryLayoutStub() );
        return repo;
    }
    
    /**
     * checks the handling of urls
     */
    public void testExternalURL()
    {
        DefaultWagonManager mgr = new DefaultWagonManager();
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://somehost" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://somehost:9090/somepath" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "ftp://somehost" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://192.168.101.1" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://" ) ) );
        // these are local
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://localhost:8080" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://127.0.0.1:9090" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file://localhost/somepath" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file://localhost/D:/somepath" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://localhost" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://127.0.0.1" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file:///somepath" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file://D:/somepath" ) ) );

        // not a proper url so returns false;
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "192.168.101.1" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "" ) ) );
    }

    /**
     * Check that lookups with exact matches work and that no matches don't corrupt the repo.
     */
    public void testMirrorLookup()
    {
        wagonManager.addMirror( "a", "a", "http://a" );
        wagonManager.addMirror( "b", "b", "http://b" );

        ArtifactRepository repo = null;
        repo = wagonManager.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://c.c", repo.getUrl() );

    }

    /**
     * Check that wildcards don't override exact id matches.
     */
    public void testMirrorWildcardLookup()
    {
        wagonManager.addMirror( "a", "a", "http://a" );
        wagonManager.addMirror( "b", "b", "http://b" );
        wagonManager.addMirror( "c", "*", "http://wildcard" );

        ArtifactRepository repo = null;
        repo = wagonManager.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://wildcard", repo.getUrl() );

    }

    /**
     * Check that patterns are processed correctly Valid patterns: * = everything external:* = everything not on the
     * localhost and not file based. repo,repo1 = repo or repo1 *,!repo1 = everything except repo1
     */
    public void testPatterns()
    {
        DefaultWagonManager mgr = new DefaultWagonManager();

        assertTrue( mgr.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), ",*," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*," ) );

        assertTrue( mgr.matchPattern( getRepo( "a" ), "a" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "a," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), ",a," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "a," ) );

        assertFalse( mgr.matchPattern( getRepo( "b" ), "a" ) );
        assertFalse( mgr.matchPattern( getRepo( "b" ), "a," ) );
        assertFalse( mgr.matchPattern( getRepo( "b" ), ",a" ) );
        assertFalse( mgr.matchPattern( getRepo( "b" ), ",a," ) );

        assertTrue( mgr.matchPattern( getRepo( "a" ), "a,b" ) );
        assertTrue( mgr.matchPattern( getRepo( "b" ), "a,b" ) );

        assertFalse( mgr.matchPattern( getRepo( "c" ), "a,b" ) );

        assertTrue( mgr.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*,b" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*,!b" ) );

        assertFalse( mgr.matchPattern( getRepo( "a" ), "*,!a" ) );
        assertFalse( mgr.matchPattern( getRepo( "a" ), "!a,*" ) );

        assertTrue( mgr.matchPattern( getRepo( "c" ), "*,!a" ) );
        assertTrue( mgr.matchPattern( getRepo( "c" ), "!a,*" ) );

        assertFalse( mgr.matchPattern( getRepo( "c" ), "!a,!c" ) );
        assertFalse( mgr.matchPattern( getRepo( "d" ), "!a,!c*" ) );
    }

    /**
     * make sure the external if is fully exercised. We can assume file and ips are also handled because they have a
     * separate test above.
     */
    public void testPatternsWithExternal()
    {
        DefaultWagonManager mgr = new DefaultWagonManager();

        assertTrue( mgr.matchPattern( getRepo( "a", "http://localhost" ), "*" ) );
        assertFalse( mgr.matchPattern( getRepo( "a", "http://localhost" ), "external:*" ) );

        assertTrue( mgr.matchPattern( getRepo( "a", "http://localhost" ), "external:*,a" ) );
        assertFalse( mgr.matchPattern( getRepo( "a", "http://localhost" ), "external:*,!a" ) );
        assertTrue( mgr.matchPattern( getRepo( "a", "http://localhost" ), "a,external:*" ) );
        assertFalse( mgr.matchPattern( getRepo( "a", "http://localhost" ), "!a,external:*" ) );

        assertFalse( mgr.matchPattern( getRepo( "c", "http://localhost" ), "!a,external:*" ) );
        assertTrue( mgr.matchPattern( getRepo( "c", "http://somehost" ), "!a,external:*" ) );
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
        return new DefaultArtifactRepository( id, url, new DefaultRepositoryLayout() );
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

    public void testDefaultWagonManager()
        throws Exception
    {
        assertWagon( "a" );

        assertWagon( "b1" );

        assertWagon( "b2" );

        assertWagon( "c" );

        assertWagon( "string" );

        try
        {
            assertWagon( "d" );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            // ok
            assertTrue( true );
        }
    }

    public void testGetWagonRepository()
        throws Exception
    {
        assertWagonRepository( "a" );

        assertWagonRepository( "b1" );

        assertWagonRepository( "b2" );

        assertWagonRepository( "c" );

        try
        {
            assertWagonRepository( "d" );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            // ok
            assertTrue( true );
        }
    }

    public void testGetWagonRepositoryNullProtocol()
        throws Exception
    {
        try
        {
            Repository repository = new Repository();

            repository.setProtocol( null );

            Wagon wagon = wagonManager.getWagon( repository );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            // ok
            assertTrue( true );
        }
    }

    /**
     * Checks the verification of checksums.
     */
    public void testChecksumVerification()
        throws Exception
    {
        ArtifactRepositoryPolicy policy =
            new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                          ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL );
        ArtifactRepository repo =
            new DefaultArtifactRepository( "id", "string://url", new ArtifactRepositoryLayoutStub(), policy, policy );

        Artifact artifact = createTestArtifact( "target/test-data/sample-art", "jar" );

        StringWagon wagon = (StringWagon) wagonManager.getWagon( "string" );
        
        wagon.clearExpectedContent();
        wagon.addExpectedContent( "path", "lower-case-checksum" );
        wagon.addExpectedContent( "path.sha1", "2a25dc564a3b34f68237fc849066cbc7bb7a36a1" );

        try
        {
            wagonManager.getArtifact( artifact, repo );
        }
        catch ( ChecksumFailedException e )
        {
            fail( "Checksum verification did not pass: " + e.getMessage() );
        }

        artifact.getFile().delete();
        wagon.clearExpectedContent();
        wagon.addExpectedContent( "path", "upper-case-checksum" );
        wagon.addExpectedContent( "path.sha1", "B7BB97D7D0B9244398D9B47296907F73313663E6" );

        try
        {
            wagonManager.getArtifact( artifact, repo );
        }
        catch ( ChecksumFailedException e )
        {
            fail( "Checksum verification did not pass: " + e.getMessage() );
        }

        artifact.getFile().delete();
        wagon.clearExpectedContent();
        wagon.addExpectedContent( "path", "expected-failure" );
        wagon.addExpectedContent( "path.sha1", "b7bb97d7d0b9244398d9b47296907f73313663e6" );

        try
        {
            wagonManager.getArtifact( artifact, repo );
            fail( "Checksum verification did not fail" );
        }
        catch ( ChecksumFailedException e )
        {
            // expected
        }

        artifact.getFile().delete();
        wagon.clearExpectedContent();
        wagon.addExpectedContent( "path", "lower-case-checksum" );
        wagon.addExpectedContent( "path.md5", "50b2cf50a103a965efac62b983035cac" );

        try
        {
            wagonManager.getArtifact( artifact, repo );
        }
        catch ( ChecksumFailedException e )
        {
            fail( "Checksum verification did not pass: " + e.getMessage() );
        }

        artifact.getFile().delete();
        wagon.clearExpectedContent();
        wagon.addExpectedContent( "path", "upper-case-checksum" );
        wagon.addExpectedContent( "path.md5", "842F568FCCFEB7E534DC72133D42FFDC" );

        try
        {
            wagonManager.getArtifact( artifact, repo );
        }
        catch ( ChecksumFailedException e )
        {
            fail( "Checksum verification did not pass: " + e.getMessage() );
        }

        artifact.getFile().delete();
        wagon.clearExpectedContent();
        wagon.addExpectedContent( "path", "expected-failure" );
        wagon.addExpectedContent( "path.md5", "b7bb97d7d0b9244398d9b47296907f73313663e6" );

        try
        {
            wagonManager.getArtifact( artifact, repo );
            fail( "Checksum verification did not fail" );
        }
        catch ( ChecksumFailedException e )
        {
            // expected
        }
    }

    private void assertWagon( String protocol )
        throws Exception
    {
        Wagon wagon = wagonManager.getWagon( protocol );

        assertNotNull( "Check wagon, protocol=" + protocol, wagon );
    }

    private void assertWagonRepository( String protocol )
        throws Exception
    {
        Repository repository = new Repository();

        String s = "value=" + protocol;

        repository.setId( "id=" + protocol );

        repository.setProtocol( protocol );

        Xpp3Dom conf = new Xpp3Dom( "configuration" );

        Xpp3Dom configurableField = new Xpp3Dom( "configurableField" );

        configurableField.setValue( s );

        conf.addChild( configurableField );

        wagonManager.addConfiguration( repository.getId(), conf );

        WagonMock wagon = (WagonMock) wagonManager.getWagon( repository );

        assertNotNull( "Check wagon, protocol=" + protocol, wagon );

        assertEquals( "Check configuration for wagon, protocol=" + protocol, s, wagon.getConfigurableField() );
    }

    private final class ArtifactRepositoryLayoutStub
        implements ArtifactRepositoryLayout
    {
        public String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata )
        {
            return "path";
        }

        public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
        {
            return "path";
        }

        public String pathOf( Artifact artifact )
        {
            return "path";
        }
    }

}
