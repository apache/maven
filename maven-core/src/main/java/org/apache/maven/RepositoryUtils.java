package org.apache.maven;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.artifact.ArtifactType;
import org.sonatype.aether.artifact.ArtifactTypeRegistry;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.util.artifact.ArtifactProperties;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.DefaultArtifactType;

/**
 * <strong>Warning:</strong> This is an internal utility class that is only public for technical reasons, it is not part
 * of the public API. In particular, this class can be changed or deleted without prior notice.
 * 
 * @author Benjamin Bentmann
 */
public class RepositoryUtils
{

    private static String nullify( String string )
    {
        return ( string == null || string.length() <= 0 ) ? null : string;
    }

    private static org.apache.maven.artifact.Artifact toArtifact( Dependency dependency )
    {
        if ( dependency == null )
        {
            return null;
        }

        org.apache.maven.artifact.Artifact result = toArtifact( dependency.getArtifact() );
        result.setScope( dependency.getScope() );
        result.setOptional( dependency.isOptional() );

        return result;
    }

    public static org.apache.maven.artifact.Artifact toArtifact( Artifact artifact )
    {
        if ( artifact == null )
        {
            return null;
        }

        ArtifactHandler handler = newHandler( artifact );

        /*
         * NOTE: From Artifact.hasClassifier(), an empty string and a null both denote "no classifier". However, some
         * plugins only check for null, so be sure to nullify an empty classifier.
         */
        org.apache.maven.artifact.Artifact result =
            new org.apache.maven.artifact.DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                           artifact.getVersion(), null,
                                                           artifact.getProperty( ArtifactProperties.TYPE,
                                                                                 artifact.getExtension() ),
                                                           nullify( artifact.getClassifier() ), handler );

        result.setFile( artifact.getFile() );
        result.setResolved( artifact.getFile() != null );

        List<String> trail = new ArrayList<String>( 1 );
        trail.add( result.getId() );
        result.setDependencyTrail( trail );

        return result;
    }

    public static void toArtifacts( Collection<org.apache.maven.artifact.Artifact> artifacts,
                                    Collection<? extends DependencyNode> nodes, List<String> trail,
                                    DependencyFilter filter )
    {
        for ( DependencyNode node : nodes )
        {
            org.apache.maven.artifact.Artifact artifact = toArtifact( node.getDependency() );

            List<String> nodeTrail = new ArrayList<String>( trail.size() + 1 );
            nodeTrail.addAll( trail );
            nodeTrail.add( artifact.getId() );

            if ( filter == null || filter.accept( node, Collections.<DependencyNode> emptyList() ) )
            {
                artifact.setDependencyTrail( nodeTrail );
                artifacts.add( artifact );
            }

            toArtifacts( artifacts, node.getChildren(), nodeTrail, filter );
        }
    }

    public static Artifact toArtifact( org.apache.maven.artifact.Artifact artifact )
    {
        if ( artifact == null )
        {
            return null;
        }

        String version = artifact.getVersion();
        if ( version == null && artifact.getVersionRange() != null )
        {
            version = artifact.getVersionRange().toString();
        }

        Map<String, String> props = null;
        if ( org.apache.maven.artifact.Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            String localPath = ( artifact.getFile() != null ) ? artifact.getFile().getPath() : "";
            props = Collections.singletonMap( ArtifactProperties.LOCAL_PATH, localPath );
        }

        Artifact result =
            new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                                 artifact.getArtifactHandler().getExtension(), version, props,
                                 newArtifactType( artifact.getType(), artifact.getArtifactHandler() ) );
        result = result.setFile( artifact.getFile() );

        return result;
    }

    public static Dependency toDependency( org.apache.maven.artifact.Artifact artifact,
                                           Collection<org.apache.maven.model.Exclusion> exclusions )
    {
        if ( artifact == null )
        {
            return null;
        }

        Artifact result = toArtifact( artifact );

        List<Exclusion> excl = null;
        if ( exclusions != null )
        {
            excl = new ArrayList<Exclusion>( exclusions.size() );
            for ( org.apache.maven.model.Exclusion exclusion : exclusions )
            {
                excl.add( toExclusion( exclusion ) );
            }
        }

        return new Dependency( result, artifact.getScope(), artifact.isOptional(), excl );
    }

    public static List<RemoteRepository> toRepos( List<ArtifactRepository> repos )
    {
        if ( repos == null )
        {
            return null;
        }

        List<RemoteRepository> results = new ArrayList<RemoteRepository>( repos.size() );
        for ( ArtifactRepository repo : repos )
        {
            results.add( toRepo( repo ) );
        }
        return results;
    }

    public static RemoteRepository toRepo( ArtifactRepository repo )
    {
        RemoteRepository result = null;
        if ( repo != null )
        {
            result = new RemoteRepository( repo.getId(), getLayout( repo ), repo.getUrl() );
            result.setPolicy( true, toPolicy( repo.getSnapshots() ) );
            result.setPolicy( false, toPolicy( repo.getReleases() ) );
            result.setAuthentication( toAuthentication( repo.getAuthentication() ) );
            result.setProxy( toProxy( repo.getProxy() ) );
            result.setMirroredRepositories( toRepos( repo.getMirroredRepositories() ) );
        }
        return result;
    }

    public static String getLayout( ArtifactRepository repo )
    {
        try
        {
            return repo.getLayout().getId();
        }
        catch ( LinkageError e )
        {
            /*
             * NOTE: getId() was added in 3.x and is as such not implemented by plugins compiled against 2.x APIs.
             */
            String className = repo.getLayout().getClass().getSimpleName();
            if ( className.endsWith( "RepositoryLayout" ) )
            {
                String layout = className.substring( 0, className.length() - "RepositoryLayout".length() );
                if ( layout.length() > 0 )
                {
                    layout = Character.toLowerCase( layout.charAt( 0 ) ) + layout.substring( 1 );
                    return layout;
                }
            }
            return "";
        }
    }

    private static RepositoryPolicy toPolicy( ArtifactRepositoryPolicy policy )
    {
        RepositoryPolicy result = null;
        if ( policy != null )
        {
            result = new RepositoryPolicy( policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy() );
        }
        return result;
    }

    private static Authentication toAuthentication( org.apache.maven.artifact.repository.Authentication auth )
    {
        Authentication result = null;
        if ( auth != null )
        {
            result =
                new Authentication( auth.getUsername(), auth.getPassword(), auth.getPrivateKey(), auth.getPassphrase() );
        }
        return result;
    }

    private static Proxy toProxy( org.apache.maven.repository.Proxy proxy )
    {
        Proxy result = null;
        if ( proxy != null )
        {
            Authentication auth = new Authentication( proxy.getUserName(), proxy.getPassword() );
            result = new Proxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth );
        }
        return result;
    }

    public static ArtifactHandler newHandler( Artifact artifact )
    {
        String type = artifact.getProperty( ArtifactProperties.TYPE, artifact.getExtension() );
        DefaultArtifactHandler handler = new DefaultArtifactHandler( type );
        handler.setExtension( artifact.getExtension() );
        handler.setLanguage( artifact.getProperty( ArtifactProperties.LANGUAGE, null ) );
        handler.setAddedToClasspath( Boolean.parseBoolean( artifact.getProperty( ArtifactProperties.CONSTITUTES_BUILD_PATH,
                                                                                 "" ) ) );
        handler.setIncludesDependencies( Boolean.parseBoolean( artifact.getProperty( ArtifactProperties.INCLUDES_DEPENDENCIES,
                                                                                     "" ) ) );
        return handler;
    }

    public static ArtifactType newArtifactType( String id, ArtifactHandler handler )
    {
        return new DefaultArtifactType( id, handler.getExtension(), handler.getClassifier(), handler.getLanguage(),
                                        handler.isAddedToClasspath(), handler.isIncludesDependencies() );
    }

    public static Dependency toDependency( org.apache.maven.model.Dependency dependency,
                                           ArtifactTypeRegistry stereotypes )
    {
        ArtifactType stereotype = stereotypes.get( dependency.getType() );
        if ( stereotype == null )
        {
            stereotype = new DefaultArtifactType( dependency.getType() );
        }

        boolean system = dependency.getSystemPath() != null && dependency.getSystemPath().length() > 0;

        Map<String, String> props = null;
        if ( system )
        {
            props = Collections.singletonMap( ArtifactProperties.LOCAL_PATH, dependency.getSystemPath() );
        }

        Artifact artifact =
            new DefaultArtifact( dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), null,
                                 dependency.getVersion(), props, stereotype );

        List<Exclusion> exclusions = new ArrayList<Exclusion>( dependency.getExclusions().size() );
        for ( org.apache.maven.model.Exclusion exclusion : dependency.getExclusions() )
        {
            exclusions.add( toExclusion( exclusion ) );
        }

        Dependency result = new Dependency( artifact, dependency.getScope(), dependency.isOptional(), exclusions );

        return result;
    }

    private static Exclusion toExclusion( org.apache.maven.model.Exclusion exclusion )
    {
        return new Exclusion( exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*" );
    }

    public static ArtifactTypeRegistry newArtifactTypeRegistry( ArtifactHandlerManager handlerManager )
    {
        return new MavenArtifactTypeRegistry( handlerManager );
    }

    static class MavenArtifactTypeRegistry
        implements ArtifactTypeRegistry
    {

        private final ArtifactHandlerManager handlerManager;

        public MavenArtifactTypeRegistry( ArtifactHandlerManager handlerManager )
        {
            this.handlerManager = handlerManager;
        }

        public ArtifactType get( String stereotypeId )
        {
            ArtifactHandler handler = handlerManager.getArtifactHandler( stereotypeId );
            return newArtifactType( stereotypeId, handler );
        }

    }

}
