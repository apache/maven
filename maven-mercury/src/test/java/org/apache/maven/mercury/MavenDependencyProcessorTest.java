package org.apache.maven.mercury;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
 
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
 
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.metadata.DependencyBuilder;
import org.apache.maven.mercury.metadata.DependencyBuilderFactory;
import org.apache.maven.mercury.repository.api.MetadataResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
 
/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class MavenDependencyProcessorTest
{
    LocalRepositoryM2 _localRepo;
 
    RemoteRepositoryM2 _remoteRepo;
 
    File _localRepoFile;
 
    static final String _remoteRepoDir = "./target/test-classes/repo";
 
    File _remoteRepoFile;
 
    static  String _remoteRepoUrlPrefix = "http://localhost:";
 
    static  String _remoteRepoUrlSufix = "/maven2";
 
//    HttpTestServer _jetty;
 
    int _port;
 
    DependencyBuilder _depBuilder; 
 
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp()
    throws Exception
    {
        MavenDependencyProcessor dp = new MavenDependencyProcessor();
 
        _localRepoFile = File.createTempFile( "maven-mercury-", "-test-repo" );
        FileUtil.delete( _localRepoFile );
        _localRepoFile.mkdirs();
        _localRepoFile.deleteOnExit();
        _localRepo = new LocalRepositoryM2( "localRepo", _localRepoFile, dp );
 
        _remoteRepoFile = new File( _remoteRepoDir );
//        _jetty = new HttpTestServer( _remoteRepoFile, _remoteRepoUrlSufix );
// FIXME 2009-02-12 Oleg: disabling not to mess with jetty server. Will move to Mercury ITs             
//        _jetty.start();
//        _port = _jetty.getPort();
 
//        Server server = new Server( "testRemote", new URL(_remoteRepoUrlPrefix + _port + _remoteRepoUrlSufix) );
 
        _remoteRepoUrlPrefix = "http://repo2.maven.org:";
        _port = 80;
        _remoteRepoUrlSufix = "/maven2";
        Server server = new Server( "testRemote", new URL(_remoteRepoUrlPrefix + _port + _remoteRepoUrlSufix) );
        _remoteRepo = new RemoteRepositoryM2( server, dp );
 
        ArrayList<Repository> repos = new ArrayList<Repository>(2);
 
        repos.add( _localRepo );
        repos.add( _remoteRepo );
 
        _depBuilder = DependencyBuilderFactory.create( DependencyBuilderFactory.JAVA_DEPENDENCY_MODEL, repos, null, null, null );
    }
 
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown()
    throws Exception
    {
//        if( _jetty != null )
//        {
// FIXME  2009-02-12 Oleg: disabling not to mess with jetty server. Will move to Mercury ITs             
//            _jetty.stop();
//            _jetty.destroy();
//
//            System.out.println( "Jetty on :" + _port + " destroyed\n<========\n\n" );
//        }
    }
 
    @Test
    public void testDummy()
    throws Exception
    {
 
    }
 
    /**
     * Test method for {@link org.apache.maven.mercury.MavenDependencyProcessor#getDependencies(org.apache.maven.mercury.artifact.ArtifactMetadata, org.apache.maven.mercury.builder.api.MetadataReader, java.util.Map, java.util.Map)}.
     */
    @Test
    public void testMavenVersion()
    throws Exception
    {
        RepositoryReader rr = _remoteRepo.getReader();
 
//        String gav = "org.apache.maven.plugins:maven-dependency-plugin:2.0";
        String gav = "asm:asm-xml:3.0";
 
        ArtifactMetadata bmd = new ArtifactMetadata( gav );
        ArrayList<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>(1);
        query.add( bmd );
 
        MetadataResults res = rr.readDependencies( query );
 
        assertNotNull( res );
 
        assertFalse( res.hasExceptions() );
 
        assertTrue( res.hasResults() );
 
        List<ArtifactMetadata> deps = res.getResult( bmd );
 
        assertNotNull( deps );
 
        assertFalse( deps.isEmpty() );
        
        ArtifactMetadata md = deps.get(0); 

        System.out.println("found "+gav+" dependencies: "+deps);
        
        assertEquals( "3.0", md.getVersion() );
        
        assertEquals( ArtifactScopeEnum.compile, md.getArtifactScope() );
    }
    @Test
    public void testForNPE()
    throws Exception
    {
        RepositoryReader rr = _remoteRepo.getReader();
 
        String gav = "org.codehaus.plexus:plexus-compiler-api:1.5.3::jar";
 
        ArtifactMetadata bmd = new ArtifactMetadata( gav );
        ArrayList<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>(1);
        query.add( bmd );
 
        MetadataResults res = rr.readDependencies( query );
 
        assertNotNull( res );
 
        assertFalse( res.hasExceptions() );
 
        assertTrue( res.hasResults() );
 
        List<ArtifactMetadata> deps = res.getResult( bmd );
 
        assertNotNull( deps );
 
        assertFalse( deps.isEmpty() );
        
        ArtifactMetadata md = deps.get(0); 

        System.out.println("found "+gav+" dependencies: "+deps);
    }

    @Test
    public void testForCompileScope()
    throws Exception
    {
        RepositoryReader rr = _remoteRepo.getReader();
 
        String gav = "org.codehaus.plexus:plexus-container-default:1.0-alpha-9";
 
        ArtifactMetadata bmd = new ArtifactMetadata( gav );
        ArrayList<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>(1);
        query.add( bmd );
 
        MetadataResults res = rr.readDependencies( query );
 
        assertNotNull( res );
 
        assertFalse( res.hasExceptions() );
 
        assertTrue( res.hasResults() );
 
        List<ArtifactMetadata> deps = res.getResult( bmd );
 
        assertNotNull( deps );
 
        assertFalse( deps.isEmpty() );
        
        System.out.println("found "+gav+" dependencies: "+deps);
        
        for( ArtifactMetadata md : deps )
        {
            System.out.println( "    "+md.toScopedString() );
            
            // junit has explicit "compile" scope, although it's parent defines it as "test" - see below 
            // http://repo2.maven.org/maven2/org/codehaus/plexus/plexus-container-default/1.0-alpha-9/plexus-container-default-1.0-alpha-9.pom
            if( "junit".equals( md.getArtifactId() ) )
                assertEquals( ArtifactScopeEnum.compile, md.getArtifactScope() );
        }
    }
    
}