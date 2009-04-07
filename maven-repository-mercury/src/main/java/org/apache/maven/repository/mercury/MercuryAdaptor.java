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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.Util;
import org.apache.maven.repository.MavenArtifactMetadata;
import org.apache.maven.repository.MetadataGraph;
import org.apache.maven.repository.MetadataGraphNode;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class MercuryAdaptor
{
    
    private static Map<String, Repository> _repos = Collections.synchronizedMap(  new HashMap<String, Repository>() );
    
    public static List<Repository> toMercuryRepos( ArtifactRepository localRepository,
                                                   List<ArtifactRepository> remoteRepositories,
                                                   DependencyProcessor dependencyProcessor
                                                 )
    {
        if ( localRepository == null && Util.isEmpty( remoteRepositories ) )
            return null;

        int nRepos =
            ( localRepository == null ? 0 : 1 ) + ( Util.isEmpty( remoteRepositories ) ? 0 : remoteRepositories.size() );

        List<Repository> res = new ArrayList<Repository>( nRepos );

        if ( localRepository != null )
        {
            String url = localRepository.getUrl();
            
            LocalRepositoryM2 lr = (LocalRepositoryM2) _repos.get( url );
            
            if( lr == null )
                try
                {
                    URI rootURI = new URI( url );
                    
                    File localRepoDir =  new File( rootURI );
                    
                    lr = new LocalRepositoryM2( localRepository.getId(), localRepoDir, dependencyProcessor );
                    
                    _repos.put( url, lr );
                }
                catch ( URISyntaxException e )
                {
                    throw new IllegalArgumentException( e );
                }
            res.add( lr );
        }

        if ( !Util.isEmpty( remoteRepositories ) )
        {
            for ( ArtifactRepository ar : remoteRepositories )
            {
                String url = ar.getUrl();
                
                RemoteRepositoryM2 rr = (RemoteRepositoryM2) _repos.get( url );
                
                if( rr == null )
                {
                    Server server;
                    try
                    {
                        server = new Server( ar.getId(), new URL( url ) );
                    }
                    catch ( MalformedURLException e )
                    {
                        throw new IllegalArgumentException( e );
                    }
                    rr = new RemoteRepositoryM2( server, dependencyProcessor );
                    _repos.put( url, rr );
                }

                res.add( rr );
            }
        }

        return res;
    }

    public static ArtifactMetadata toMercuryBasicMetadata( Artifact a )
    {
        ArtifactMetadata md = new ArtifactMetadata();
        md.setGroupId( a.getGroupId() );
        md.setArtifactId( a.getArtifactId() );
        md.setVersion( a.getVersion() );
        md.setType( a.getType() );
        md.setScope( a.getScope() );

        return md;
    }

    public static ArtifactMetadata toMercuryMetadata( Artifact a )
    {
        ArtifactMetadata md = new ArtifactMetadata();
        md.setGroupId( a.getGroupId() );
        md.setArtifactId( a.getArtifactId() );
        md.setVersion( a.getVersion() );
        md.setType( a.getType() );
        md.setScope( a.getScope() );

        return md;
    }

    public static Artifact toMavenArtifact( ArtifactFactory af, org.apache.maven.mercury.artifact.Artifact a )
    {
        Artifact ma = a.getClassifier() == null 
                        ? af.createArtifact( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getScope(), a.getType() )
                        : af.createArtifactWithClassifier( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getType(), a.getClassifier() )
                        ;
        ma.setScope( a.getScope() );
        
        ma.setFile( a.getFile() );

        return ma;
    }
    
    public static Artifact toMavenArtifact( ArtifactFactory af, org.apache.maven.mercury.artifact.ArtifactMetadata a )
    {
        Artifact ma = a.getClassifier() == null 
                                ? af.createArtifact( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getScope(), a.getType() )
                                : af.createArtifactWithClassifier( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getType(), a.getClassifier() )
                                ;
        ma.setScope( a.getScope() );

        return ma;
    }
    
    public static Artifact toMavenArtifact( ArtifactFactory af, String name )
    {
        return toMavenArtifact( af, new ArtifactMetadata(name) );
    }
    
    public static ArtifactMetadata toMercuryArtifactMetadata( MavenArtifactMetadata md )
    {
        ArtifactMetadata mmd = new ArtifactMetadata();
        mmd.setGroupId( md.getGroupId() );
        mmd.setArtifactId( md.getArtifactId() );
        mmd.setVersion( md.getVersion() );
        mmd.setClassifier( md.getClassifier() );
        mmd.setType( md.getType() );

        return mmd;
    }
    
    public static MavenArtifactMetadata toMavenArtifactMetadata( ArtifactMetadata md )
    {
        MavenArtifactMetadata mmd = new MavenArtifactMetadata();
        mmd.setGroupId( md.getGroupId() );
        mmd.setArtifactId( md.getArtifactId() );
        mmd.setVersion( md.getVersion() );
        mmd.setClassifier( md.getClassifier() );
        mmd.setType( md.getType() );

        return mmd;
    }
    
    public static MavenArtifactMetadata toMavenArtifactMetadata( Artifact md )
    {
        MavenArtifactMetadata mmd = new MavenArtifactMetadata();
        mmd.setGroupId( md.getGroupId() );
        mmd.setArtifactId( md.getArtifactId() );
        mmd.setVersion( md.getVersion() );
        mmd.setClassifier( md.getClassifier() );
        mmd.setType( md.getType() );

        return mmd;
    }
    
    public static MetadataGraph resolvedTreeToGraph( MetadataTreeNode root )
    {
        if( root == null )
            return null;
        
        MetadataGraphNode entry = new MetadataGraphNode( toMavenArtifactMetadata( root.getMd() ) );
        
        MetadataGraph graph = new MetadataGraph(entry);
        
        graph.addNode( entry );
        
        addKids( root, entry, graph );
        
        return graph;
    }
    
    private static final void addKids( MetadataTreeNode tParent, MetadataGraphNode gParent, MetadataGraph graph )
    {
        if( !tParent.hasChildren() )
            return;
        
        for( MetadataTreeNode kid : tParent.getChildren() )
        {
            MavenArtifactMetadata mmd = toMavenArtifactMetadata( kid.getMd() );
            
            MetadataGraphNode node = graph.findNode( mmd );
            
            node.addIncident( gParent );
            
            gParent.addIncident( node );
            
            addKids( kid, node, graph );
        }
    }

}
