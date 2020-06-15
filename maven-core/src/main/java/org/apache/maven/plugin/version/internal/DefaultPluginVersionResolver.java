package org.apache.maven.plugin.version.internal;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

/**
 * Resolves a version for a plugin.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultPluginVersionResolver
    implements PluginVersionResolver
{

    private static final String REPOSITORY_CONTEXT = "plugin";

    @Inject
    private Logger logger;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private MetadataReader metadataReader;

    @Inject
    private MavenPluginManager pluginManager;

    public PluginVersionResult resolve( PluginVersionRequest request )
        throws PluginVersionResolutionException
    {
        PluginVersionResult result = resolveFromProject( request );

        if ( result == null )
        {
            result = resolveFromRepository( request );

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Resolved plugin version for " + request.getGroupId() + ":" + request.getArtifactId()
                    + " to " + result.getVersion() + " from repository " + result.getRepository() );
            }
        }
        else if ( logger.isDebugEnabled() )
        {
            logger.debug( "Resolved plugin version for " + request.getGroupId() + ":" + request.getArtifactId() + " to "
                + result.getVersion() + " from POM " + request.getPom() );
        }

        return result;
    }

    private PluginVersionResult resolveFromRepository( PluginVersionRequest request )
        throws PluginVersionResolutionException
    {
        RequestTrace trace = RequestTrace.newChild( null, request );

        DefaultPluginVersionResult result = new DefaultPluginVersionResult();

        org.eclipse.aether.metadata.Metadata metadata =
            new DefaultMetadata( request.getGroupId(), request.getArtifactId(), "maven-metadata.xml",
                                 DefaultMetadata.Nature.RELEASE_OR_SNAPSHOT );

        List<MetadataRequest> requests = new ArrayList<>();

        requests.add( new MetadataRequest( metadata, null, REPOSITORY_CONTEXT ).setTrace( trace ) );

        for ( RemoteRepository repository : request.getRepositories() )
        {
            requests.add( new MetadataRequest( metadata, repository, REPOSITORY_CONTEXT ).setTrace( trace ) );
        }

        List<MetadataResult> results = repositorySystem.resolveMetadata( request.getRepositorySession(), requests );

        Versions versions = new Versions();

        for ( MetadataResult res : results )
        {
            ArtifactRepository repository = res.getRequest().getRepository();
            if ( repository == null )
            {
                repository = request.getRepositorySession().getLocalRepository();
            }

            mergeMetadata( request.getRepositorySession(), trace, versions, res.getMetadata(), repository );
        }

        selectVersion( result, request, versions );

        return result;
    }

    private void selectVersion( DefaultPluginVersionResult result, PluginVersionRequest request, Versions versions )
        throws PluginVersionResolutionException
    {
        String version = null;
        ArtifactRepository repo = null;

        if ( StringUtils.isNotEmpty( versions.releaseVersion ) )
        {
            version = versions.releaseVersion;
            repo = versions.releaseRepository;
        }
        else if ( StringUtils.isNotEmpty( versions.latestVersion ) )
        {
            version = versions.latestVersion;
            repo = versions.latestRepository;
        }
        if ( version != null && !isCompatible( request, version ) )
        {
            versions.versions.remove( version );
            version = null;
        }

        if ( version == null )
        {
            VersionScheme versionScheme = new GenericVersionScheme();

            TreeSet<Version> releases = new TreeSet<>( Collections.reverseOrder() );
            TreeSet<Version> snapshots = new TreeSet<>( Collections.reverseOrder() );

            for ( String ver : versions.versions.keySet() )
            {
                try
                {
                    Version v = versionScheme.parseVersion( ver );

                    if ( ver.endsWith( "-SNAPSHOT" ) )
                    {
                        snapshots.add( v );
                    }
                    else
                    {
                        releases.add( v );
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    // ignore
                }
            }

            for ( Version v : releases )
            {
                String ver = v.toString();
                if ( isCompatible( request, ver ) )
                {
                    version = ver;
                    repo = versions.versions.get( version );
                    break;
                }
            }

            if ( version == null )
            {
                for ( Version v : snapshots )
                {
                    String ver = v.toString();
                    if ( isCompatible( request, ver ) )
                    {
                        version = ver;
                        repo = versions.versions.get( version );
                        break;
                    }
                }
            }
        }

        if ( version != null )
        {
            result.setVersion( version );
            result.setRepository( repo );
        }
        else
        {
            throw new PluginVersionResolutionException( request.getGroupId(), request.getArtifactId(),
                                                        request.getRepositorySession().getLocalRepository(),
                                                        request.getRepositories(),
                                                        "Plugin not found in any plugin repository" );
        }
    }

    private boolean isCompatible( PluginVersionRequest request, String version )
    {
        Plugin plugin = new Plugin();
        plugin.setGroupId( request.getGroupId() );
        plugin.setArtifactId( request.getArtifactId() );
        plugin.setVersion( version );

        PluginDescriptor pluginDescriptor;

        try
        {
            pluginDescriptor =
                pluginManager.getPluginDescriptor( plugin, request.getRepositories(), request.getRepositorySession() );
        }
        catch ( PluginResolutionException e )
        {
            logger.debug( "Ignoring unresolvable plugin version " + version, e );
            return false;
        }
        catch ( Exception e )
        {
            // ignore for now and delay failure to higher level processing
            return true;
        }

        try
        {
            pluginManager.checkRequiredMavenVersion( pluginDescriptor );
        }
        catch ( Exception e )
        {
            logger.debug( "Ignoring incompatible plugin version " + version + ": " + e.getMessage() );
            return false;
        }

        return true;
    }

    private void mergeMetadata( RepositorySystemSession session, RequestTrace trace, Versions versions,
                                org.eclipse.aether.metadata.Metadata metadata, ArtifactRepository repository )
    {
        if ( metadata != null && metadata.getFile() != null && metadata.getFile().isFile() )
        {
            try
            {
                Map<String, ?> options = Collections.singletonMap( MetadataReader.IS_STRICT, Boolean.FALSE );

                Metadata repoMetadata = metadataReader.read( metadata.getFile(), options );

                mergeMetadata( versions, repoMetadata, repository );
            }
            catch ( IOException e )
            {
                invalidMetadata( session, trace, metadata, repository, e );
            }
        }
    }

    private void invalidMetadata( RepositorySystemSession session, RequestTrace trace,
                                  org.eclipse.aether.metadata.Metadata metadata, ArtifactRepository repository,
                                  Exception exception )
    {
        RepositoryListener listener = session.getRepositoryListener();
        if ( listener != null )
        {
            RepositoryEvent.Builder event = new RepositoryEvent.Builder( session, EventType.METADATA_INVALID );
            event.setTrace( trace );
            event.setMetadata( metadata );
            event.setException( exception );
            event.setRepository( repository );
            listener.metadataInvalid( event.build() );
        }
    }

    private void mergeMetadata( Versions versions, Metadata source, ArtifactRepository repository )
    {
        Versioning versioning = source.getVersioning();
        if ( versioning != null )
        {
            String timestamp = StringUtils.clean( versioning.getLastUpdated() );

            if ( StringUtils.isNotEmpty( versioning.getRelease() )
                && timestamp.compareTo( versions.releaseTimestamp ) > 0 )
            {
                versions.releaseVersion = versioning.getRelease();
                versions.releaseTimestamp = timestamp;
                versions.releaseRepository = repository;
            }

            if ( StringUtils.isNotEmpty( versioning.getLatest() )
                && timestamp.compareTo( versions.latestTimestamp ) > 0 )
            {
                versions.latestVersion = versioning.getLatest();
                versions.latestTimestamp = timestamp;
                versions.latestRepository = repository;
            }

            for ( String version : versioning.getVersions() )
            {
                if ( !versions.versions.containsKey( version ) )
                {
                    versions.versions.put( version, repository );
                }
            }
        }
    }

    private PluginVersionResult resolveFromProject( PluginVersionRequest request )
    {
        PluginVersionResult result = null;

        if ( request.getPom() != null && request.getPom().getBuild() != null )
        {
            Build build = request.getPom().getBuild();

            result = resolveFromProject( request, build.getPlugins() );

            if ( result == null && build.getPluginManagement() != null )
            {
                result = resolveFromProject( request, build.getPluginManagement().getPlugins() );
            }
        }

        return result;
    }

    private PluginVersionResult resolveFromProject( PluginVersionRequest request, List<Plugin> plugins )
    {
        for ( Plugin plugin : plugins )
        {
            if ( request.getGroupId().equals( plugin.getGroupId() )
                && request.getArtifactId().equals( plugin.getArtifactId() ) )
            {
                if ( plugin.getVersion() != null )
                {
                    return new DefaultPluginVersionResult( plugin.getVersion() );
                }
                else
                {
                    return null;
                }
            }
        }
        return null;
    }

    static class Versions
    {

        String releaseVersion = "";

        String releaseTimestamp = "";

        ArtifactRepository releaseRepository;

        String latestVersion = "";

        String latestTimestamp = "";

        ArtifactRepository latestRepository;

        Map<String, ArtifactRepository> versions = new LinkedHashMap<>();

    }

}
