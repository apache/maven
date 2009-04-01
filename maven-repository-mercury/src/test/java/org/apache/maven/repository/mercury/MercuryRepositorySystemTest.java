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

package org.apache.maven.repository.mercury;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.repository.AbstractMavenRepositorySystemTest;
import org.apache.maven.repository.MavenArtifactMetadata;
import org.apache.maven.repository.MetadataGraph;
import org.apache.maven.repository.MetadataGraphNode;
import org.apache.maven.repository.MetadataResolutionRequest;
import org.apache.maven.repository.MetadataResolutionResult;
import org.apache.maven.repository.RepositorySystem;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class MercuryRepositorySystemTest
    extends AbstractMavenRepositorySystemTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        _mrs = getContainer().lookup( RepositorySystem.class, "mercury" );
    }
    
    
    public void testResolveTree()
    throws IOException
    {
        MavenArtifactMetadata mad = MercuryAdaptor.toMavenArtifactMetadata( new ArtifactMetadata( "asm:asm-xml:3.0" ) );
        
        MetadataResolutionRequest request = new MetadataResolutionRequest();
        request.setLocalRepository( _localRepo );
        request.setRemoteRepostories( _remoteRepos );
        request.setArtifactMetadata( mad );
        request.setAsResolvedTree( true );
        request.setScope( "compile" );
        
        MetadataResolutionResult res = _mrs.resolveMetadata( request );
        
        assertNotNull( res );
        
        MetadataGraph resGraph = res.getResolvedTree();
        
        assertNotNull( resGraph );
        
        Collection<MetadataGraphNode> nodes = resGraph.getNodes();
        
        assertNotNull( nodes );
        
        assertEquals( 4, nodes.size() );
        
        assertTrue( nodes.contains( new MetadataGraphNode( MercuryAdaptor.toMavenArtifactMetadata( new ArtifactMetadata( "asm:asm-xml:3.0" ) ) ) ) );
        
        assertTrue( nodes.contains( new MetadataGraphNode( MercuryAdaptor.toMavenArtifactMetadata( new ArtifactMetadata( "asm:asm-util:3.0" ) ) ) ) );
        
        assertTrue( nodes.contains( new MetadataGraphNode( MercuryAdaptor.toMavenArtifactMetadata( new ArtifactMetadata( "asm:asm-tree:3.0" ) ) ) ) );
        
        assertTrue( nodes.contains( new MetadataGraphNode( MercuryAdaptor.toMavenArtifactMetadata( new ArtifactMetadata( "asm:asm:3.0" ) ) ) ) );
        
        assertFalse( nodes.contains( new MetadataGraphNode( MercuryAdaptor.toMavenArtifactMetadata( new ArtifactMetadata( "asm:asm-parent:3.0" ) ) ) ) );
        
//        for( Artifact a : as )
//        {
//            assertTrue( a.getFile().exists() );
//            
//            System.out.println( a.getFile().getCanonicalPath()+ " : "+ a.getFile().length()+" bytes");
//        }

    }
}
