package org.apache.maven.repository;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
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
    protected void tearDown() throws Exception {
            mirrorBuilder = null;
            super.tearDown();
    }
    
    public void testAddMirrorWithNullRepositoryId()
    {
        mirrorBuilder.addMirror( null, "test", "http://www.nowhere.com/" );
    }
        
    public void testExternalURL()
    {
        assertTrue( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://somehost" ) ) );
        assertTrue( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://somehost:9090/somepath" ) ) );
        assertTrue( mirrorBuilder.isExternalRepo( getRepo( "foo", "ftp://somehost" ) ) );
        assertTrue( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://192.168.101.1" ) ) );
        assertTrue( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://" ) ) );
        // these are local
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://localhost:8080" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://127.0.0.1:9090" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "file://localhost/somepath" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "file://localhost/D:/somepath" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://localhost" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "http://127.0.0.1" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "file:///somepath" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "file://D:/somepath" ) ) );

        // not a proper url so returns false;
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "192.168.101.1" ) ) );
        assertFalse( mirrorBuilder.isExternalRepo( getRepo( "foo", "" ) ) );
    }

    public void testMirrorLookup()
    {
        mirrorBuilder.addMirror( "a", "a", "http://a" );
        mirrorBuilder.addMirror( "b", "b", "http://b" );

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
        mirrorBuilder.addMirror( "a", "a", "http://a" );
        mirrorBuilder.addMirror( "b", "b", "http://b" );
        mirrorBuilder.addMirror( "c", "*", "http://wildcard" );

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
        mirrorBuilder.addMirror( "a2", "a,b", "http://a2" );
        mirrorBuilder.addMirror( "a", "a", "http://a" );
        //make sure repeated entries are skipped
        mirrorBuilder.addMirror( "a", "a", "http://a3" ); 
        
        mirrorBuilder.addMirror( "b", "b", "http://b" );
        mirrorBuilder.addMirror( "c", "d,e", "http://de" );
        mirrorBuilder.addMirror( "c", "*", "http://wildcard" );
        mirrorBuilder.addMirror( "c", "e,f", "http://ef" );        

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
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "*," ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), ",*," ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "*," ) );

        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "a" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "a," ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), ",a," ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "a," ) );

        assertFalse( mirrorBuilder.matchPattern( getRepo( "b" ), "a" ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "b" ), "a," ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "b" ), ",a" ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "b" ), ",a," ) );

        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "a,b" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "b" ), "a,b" ) );

        assertFalse( mirrorBuilder.matchPattern( getRepo( "c" ), "a,b" ) );

        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "*,b" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a" ), "*,!b" ) );

        assertFalse( mirrorBuilder.matchPattern( getRepo( "a" ), "*,!a" ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "a" ), "!a,*" ) );

        assertTrue( mirrorBuilder.matchPattern( getRepo( "c" ), "*,!a" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "c" ), "!a,*" ) );

        assertFalse( mirrorBuilder.matchPattern( getRepo( "c" ), "!a,!c" ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "d" ), "!a,!c*" ) );
    }

    public void testPatternsWithExternal()
    {
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "*" ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "external:*" ) );

        assertTrue( mirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "external:*,a" ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "external:*,!a" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "a,external:*" ) );
        assertFalse( mirrorBuilder.matchPattern( getRepo( "a", "http://localhost" ), "!a,external:*" ) );

        assertFalse( mirrorBuilder.matchPattern( getRepo( "c", "http://localhost" ), "!a,external:*" ) );
        assertTrue( mirrorBuilder.matchPattern( getRepo( "c", "http://somehost" ), "!a,external:*" ) );
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
