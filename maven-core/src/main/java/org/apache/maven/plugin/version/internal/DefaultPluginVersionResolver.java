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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.transfer.MetadataNotFoundException;
import org.sonatype.aether.util.metadata.DefaultMetadata;

/**
 * Resolves a version for a plugin.
 * 
 * @since 3.0-alpha-3
 * @author Benjamin Bentmann
 */
@Component( role = PluginVersionResolver.class )
public class DefaultPluginVersionResolver
    implements PluginVersionResolver
{

    private static final String REPOSITORY_CONTEXT = "plugin";

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private MetadataReader metadataReader;

    public PluginVersionResult resolve( PluginVersionRequest request )
        throws PluginVersionResolutionException
    {
        logger.debug( "Resolving plugin version for " + request.getGroupId() + ":" + request.getArtifactId() );

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
            logger.debug( "Resolved plugin version for " + request.getGroupId() + ":" + request.getArtifactId()
                + " to " + result.getVersion() + " from POM " + request.getPom() );
        }

        return result;
    }

    private PluginVersionResult resolveFromRepository( PluginVersionRequest request )
        throws PluginVersionResolutionException
    {
        DefaultPluginVersionResult result = new DefaultPluginVersionResult();

        org.sonatype.aether.metadata.Metadata metadata =
            new DefaultMetadata( request.getGroupId(), request.getArtifactId(), "maven-metadata.xml",
                                 DefaultMetadata.Nature.RELEASE_OR_SNAPSHOT );

        List<MetadataRequest> requests = new ArrayList<MetadataRequest>();

        requests.add( new MetadataRequest( metadata, null, REPOSITORY_CONTEXT ) );

        for ( RemoteRepository repository : request.getRepositories() )
        {
            requests.add( new MetadataRequest( metadata, repository, REPOSITORY_CONTEXT ) );
        }

        List<MetadataResult> results = repositorySystem.resolveMetadata( request.getRepositorySession(), requests );

        LocalRepository localRepo = request.getRepositorySession().getLocalRepository();

        Versions versions = new Versions();

        for ( MetadataResult res : results )
        {
            if ( res.getException() != null )
            {
                if ( res.getException() instanceof MetadataNotFoundException )
                {
                    logger.debug( "Could not find " + res.getRequest().getMetadata() + " in "
                        + res.getRequest().getRepository() );
                }
                else if ( logger.isDebugEnabled() )
                {
                    logger.warn( "Could not retrieve " + res.getRequest().getMetadata() + " from "
                        + res.getRequest().getRepository() + ": " + res.getException().getMessage(), res.getException() );
                }
                else
                {
                    logger.warn( "Could not retrieve " + res.getRequest().getMetadata() + " from "
                        + res.getRequest().getRepository() + ": " + res.getException().getMessage() );
                }
            }

            if ( res.getMetadata() != null )
            {
                mergeMetadata( versions, res.getMetadata().getFile(), res.getRequest().getRepository() );
            }
        }

        if ( StringUtils.isNotEmpty( versions.releaseVersion ) )
        {
            result.setVersion( versions.releaseVersion );
            result.setRepository( ( versions.releaseRepository == null ) ? localRepo : versions.releaseRepository );
        }
        else if ( StringUtils.isNotEmpty( versions.latestVersion ) )
        {
            result.setVersion( versions.latestVersion );
            result.setRepository( ( versions.latestRepository == null ) ? localRepo : versions.latestRepository );
        }
        else
        {
            throw new PluginVersionResolutionException( request.getGroupId(), request.getArtifactId(), localRepo,
                                                        request.getRepositories(),
                                                        "Plugin not found in any plugin repository" );
        }

        return result;
    }

    private void mergeMetadata( Versions versions, File metadataFile, ArtifactRepository repository )
    {
        if ( metadataFile != null && metadataFile.isFile() )
        {
            try
            {
                Map<String, ?> options = Collections.singletonMap( MetadataReader.IS_STRICT, Boolean.FALSE );

                Metadata repoMetadata = metadataReader.read( metadataFile, options );

                mergeMetadata( versions, repoMetadata, repository );
            }
            catch ( IOException e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.warn( "Failed to read metadata " + metadataFile + ": " + e.getMessage(), e );
                }
                else
                {
                    logger.warn( "Failed to read metadata " + metadataFile + ": " + e.getMessage() );
                }
            }
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

    }

}
