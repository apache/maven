/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.maven.repository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.mercury.util.FileUtil;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.sonatype.plexus.jetty.DefaultServletContainer;
import org.sonatype.plexus.webcontainer.ServletContainer;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public abstract class AbstractMavenRepositorySystemTest
    extends PlexusTestCase
{
    File _localBase;
    protected ArtifactRepository _localRepo;
    
    File _remoteBase;
    protected List<ArtifactRepository> _remoteRepos;
    
    String _port = "32825"; // from src/test/plexus/webdav.xml
    
    ArtifactFactory _artifactFactory;
    
    protected RepositorySystem _mrs;
    
    protected Startable _server;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        _remoteBase = new File("./target/remote-repo");
        FileUtil.delete( _remoteBase );
        FileUtil.unZip( getClassLoader().getResourceAsStream( "remoteRepo.jar" ), new File("target") );
        
        assertTrue( _remoteBase.exists() );
        
        // context comes from src/test/plexus/webdav.xml
        ArtifactRepository remoteRepo = new DefaultArtifactRepository("remote", "http://localhost:"+_port+"/webdav", new DefaultRepositoryLayout() );
        
        _remoteRepos = new ArrayList<ArtifactRepository>(1);
        
        _remoteRepos.add( remoteRepo );

        _localBase = new File("./target/local-repo");
        FileUtil.delete( _localBase );
        _localBase.mkdirs();

        _localRepo = new DefaultArtifactRepository("local", _localBase.getCanonicalPath(), new DefaultRepositoryLayout() );
        
        _artifactFactory = getContainer().lookup( ArtifactFactory.class );
        
        _server = (Startable) getContainer().lookup( ServletContainer.class );
        
        _server.start();
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        
        if( _server != null )
            _server.stop();
        
        _server = null;
    }

    protected void customizeContainerConfiguration( ContainerConfiguration containerConfiguration )
    {
        URL url = getClassLoader().getResource( "webdav.xml" );
        containerConfiguration.setContainerConfigurationURL( url );
    }
    
    private boolean checkExists( Collection<Artifact> arts, String name )
    {
        StringTokenizer st = new StringTokenizer( name, ":" );
        
        String groupId = st.nextToken();
        
        String artifactId = st.nextToken();
        
        String version = st.nextToken();
        
        for( Artifact a : arts )
            if( groupId.equals( a.getGroupId() )
              && artifactId.equals( a.getArtifactId() )
              && version.equals( a.getVersion() )
              )
                return true;
        
        return false;
    }
    
    public void testRetrieve() throws IOException
    {
        Artifact artifact = _artifactFactory.createArtifact( "asm", "asm-xml", "3.0", "compile", "jar" );
        
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setLocalRepository( _localRepo );
        request.setRemoteRepostories( _remoteRepos );
        request.setArtifact( artifact );
        request.setResolveRoot( true );
        
        ArtifactResolutionResult res = _mrs.resolve( request );
        
        assertNotNull( res );
        
        Set<Artifact> as = res.getArtifacts();
        
        assertNotNull( as );
        
        assertEquals( 4, as.size() );
        
        assertTrue( checkExists( as, "asm:asm-xml:3.0" ) );
        
        assertTrue( checkExists( as, "asm:asm-util:3.0" ) );
        
        assertTrue( checkExists( as, "asm:asm-tree:3.0" ) );
        
        assertTrue( checkExists( as, "asm:asm:3.0" ) );
        
        assertFalse( checkExists( as, "asm:asm-parent:3.0" ) );
        
        for( Artifact a : as )
        {
            assertTrue( a.getFile().exists() );
            
            System.out.println( a.getFile().getCanonicalPath()+ " : "+ a.getFile().length()+" bytes");
        }

    }

    
//    public void testRetrieveVersions() throws Exception
//    {
//        Artifact artifact = _artifactFactory.createArtifact( "asm", "asm", "[3.0,3.2)", "compile", "jar" );
//        
//        List<ArtifactVersion> res = _mrs.retrieveAvailableVersions( artifact, _localRepo, _remoteRepos );
//        
//        assertNotNull( res );
//        
//        assertEquals( 2, res.size() );
//        
//        assertTrue( res.contains( new DefaultArtifactVersion("3.0") ) );
//        
//        assertTrue( res.contains( new DefaultArtifactVersion("3.1") ) );
//
//    }

    
}
