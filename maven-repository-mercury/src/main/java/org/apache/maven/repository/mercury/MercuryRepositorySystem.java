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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.plexus.PlexusMercury;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.util.Util;
import org.apache.maven.repository.LegacyRepositorySystem;
import org.apache.maven.repository.MetadataGraph;
import org.apache.maven.repository.MetadataResolutionRequest;
import org.apache.maven.repository.MetadataResolutionResult;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
@Component( role = RepositorySystem.class, hint = "mercury" )
public class MercuryRepositorySystem
    extends LegacyRepositorySystem
    implements RepositorySystem
{
    private static final Language LANG = new DefaultLanguage( MercuryRepositorySystem.class );

    @Requirement( hint = "maven" )
    DependencyProcessor _dependencyProcessor;

    @Requirement
    PlexusMercury _mercury;

    @Requirement
    ArtifactFactory _artifactFactory;

    @Override
    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        if ( request == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request" ) );

System.out.println("mercury: request for "+request.getArtifact()
+"("+request.getArtifactDependencies()+") repos="+request.getRemoteRepostories().size()
+" repos, map=" + request.getManagedVersionMap() 
);

        if ( request.getArtifact() == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request.artifact" ) );
        
        Map<String, ArtifactMetadata> versionMap = MercuryAdaptor.toMercuryVersionMap( (Map<String,Artifact>)request.getManagedVersionMap() );

        ArtifactResolutionResult result = new ArtifactResolutionResult();

        List<Repository> repos =
            MercuryAdaptor.toMercuryRepos(   request.getLocalRepository()
                                           , request.getRemoteRepostories()
                                           , _dependencyProcessor
                                         );

        try
        {
long start = System.currentTimeMillis();

            org.apache.maven.artifact.Artifact rootArtifact = request.getArtifact();
            
            org.apache.maven.artifact.Artifact mavenPluginArtifact = rootArtifact;
            
            Set<Artifact> artifacts = request.getArtifactDependencies();
            
            boolean isPlugin = "maven-plugin".equals( rootArtifact.getType() ); 
            
            ArtifactScopeEnum scope = MercuryAdaptor.extractScope( rootArtifact, isPlugin, request.getFilter() );
            
            if( isPlugin  )
                rootArtifact = createArtifact( rootArtifact.getGroupId()
                                                    , rootArtifact.getArtifactId()
                                                    , rootArtifact.getVersion()
                                                    , rootArtifact.getScope()
                                                    , "jar"
                                                  );

            ArtifactMetadata rootMd = MercuryAdaptor.toMercuryMetadata( rootArtifact );

            org.apache.maven.artifact.Artifact root = null;

            // copied from artifact resolver 
            if ( request.isResolveRoot() && rootArtifact.getFile() == null && Util.isEmpty( artifacts ) )
            {
                try
                {
                    List<ArtifactMetadata> mercuryMetadataList = new ArrayList<ArtifactMetadata>(1);
                    
                    mercuryMetadataList.add( rootMd );
                    
                    List<org.apache.maven.mercury.artifact.Artifact> mercuryArtifactList =
                        _mercury.read( repos, mercuryMetadataList );
                    
                    if( Util.isEmpty( mercuryArtifactList ) )
                    {
                        result.addErrorArtifactException( new ArtifactResolutionException( "scope="+scope, rootArtifact) );
                        return result;
                    }

                    root = isPlugin ? mavenPluginArtifact : rootArtifact;
                    
                    org.apache.maven.mercury.artifact.Artifact a = mercuryArtifactList.get( 0 );
                    
                    root.setFile( a.getFile() );
                    root.setResolved( true );
                    root.setResolvedVersion( a.getVersion() );
                    
                    result.addArtifact( rootArtifact );
                    result.addRequestedArtifact( rootArtifact );
                }
                catch ( Exception e )
                {
                    result.addMissingArtifact( request.getArtifact() );
                    return result;
                }
            }

            if ( Util.isEmpty( artifacts ) )
            {
                return result;
            } 
            
            List<ArtifactMetadata> mercuryMetadataList = null;

            if ( Util.isEmpty( artifacts ) )
                mercuryMetadataList = _mercury.resolve( repos, scope,  rootMd );
            else
            {
                List<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>( artifacts.size() + 1 );
                
                query.add( rootMd );
                
                for( Artifact a : artifacts )
                    query.add( MercuryAdaptor.toMercuryMetadata( a ) );

                mercuryMetadataList = _mercury.resolve( repos, scope, new ArtifactQueryList(query), null, null, versionMap );
            }

            List<org.apache.maven.mercury.artifact.Artifact> mercuryArtifactList =
                _mercury.read( repos, mercuryMetadataList );

long diff = System.currentTimeMillis() - start;
            
            if ( !Util.isEmpty( mercuryArtifactList ) )
            {
                for ( org.apache.maven.mercury.artifact.Artifact a : mercuryArtifactList )
                {
                    if( a.getGroupId().equals( rootMd.getGroupId() ) && a.getArtifactId().equals( rootMd.getArtifactId() ) )
                    { // root artifact processing
                        root = isPlugin ? mavenPluginArtifact : rootArtifact;
                        
                        root.setFile( a.getFile() );
                        root.setResolved( true );
                        root.setResolvedVersion( a.getVersion() );

                        result.addArtifact( root );
                        result.addRequestedArtifact( root );
                    }
                    else
                    {
                        Artifact ma = MercuryAdaptor.toMavenArtifact( _artifactFactory, a );
                        
                        result.addArtifact( ma );
                        result.addRequestedArtifact( ma );
                    }
                }

System.out.println("mercury: resolved("+diff+") "+root+"("+scope+") as file "+root.getFile() );
//for( Artifact a: result.getArtifacts() )
//System.out.println("mercury dependency: "+a+" as file "+a.getFile() );
            }
            else
            {
                result.addMissingArtifact( rootArtifact );
System.out.println("mercury: missing artifact("+diff+") "+rootArtifact+"("+scope+")" );
            }
            
        }
        catch ( RepositoryException e )
        {
            result.addErrorArtifactException( new ArtifactResolutionException( e.getMessage(), request.getArtifact(),
                                                                               request.getRemoteRepostories() ) );
        }

        return result;
    }
    
    

//    public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
//                                                            List<ArtifactRepository> remoteRepositories )
//        throws ArtifactMetadataRetrievalException
//    {
//
//        List<Repository> repos =
//            MercuryAdaptor.toMercuryRepos( localRepository, remoteRepositories, _dependencyProcessor );
//        
//        try
//        {
//            List<ArtifactBasicMetadata> vl = _mercury.readVersions( repos, MercuryAdaptor.toMercuryBasicMetadata( artifact ) );
//            
//            if( Util.isEmpty( vl ) )
//                return null;
//            
//            List<ArtifactVersion> res = new ArrayList<ArtifactVersion>( vl.size() );
//            
//            for( ArtifactBasicMetadata bmd : vl )
//                res.add( new DefaultArtifactVersion(bmd.getVersion()) );
//            
//            return res;
//        }
//        catch ( RepositoryException e )
//        {
//            throw new ArtifactMetadataRetrievalException(e);
//        }
//    }
    

    public MetadataResolutionResult resolveMetadata( MetadataResolutionRequest request )
    {
        if ( request == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request" ) );

        if ( request.getArtifactMetadata() == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.request.artifact" ) );

        List<Repository> repos =
            MercuryAdaptor.toMercuryRepos( request.getLocalRepository()
                                           , request.getRemoteRepostories()
                                           , _dependencyProcessor
                                         );

        MetadataResolutionResult res = new MetadataResolutionResult();
        
        ArtifactMetadata md = MercuryAdaptor.toMercuryArtifactMetadata( request.getArtifactMetadata() );
        
        try
        {
            MetadataTreeNode root = _mercury.resolveAsTree( repos, ArtifactScopeEnum.valueOf( request.getScope() ), new ArtifactQueryList(md), null, null );
            if( root != null )
            {
                MetadataGraph resTree = MercuryAdaptor.resolvedTreeToGraph( root );
                
                res.setResolvedTree( resTree );
            }
        }
        catch ( RepositoryException e )
        {
            res.addError( e );
        }
        
        return res;
    }

}
