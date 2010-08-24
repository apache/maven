package org.apache.maven.project;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.impl.RemoteRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * A model resolver to assist building of projects. This resolver gives priority to those repositories that have been
 * declared in the POM.
 * 
 * @author Benjamin Bentmann
 */
class ReactorModelResolver
    implements ModelResolver
{

    private final RepositorySystemSession session;

    private final String context = "project";

    private List<RemoteRepository> repositories;

    private List<RemoteRepository> pomRepositories;

    private final List<RemoteRepository> defaultRepositories;

    private final ArtifactResolver resolver;

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final Set<String> repositoryIds;

    private final ReactorModelPool modelPool;

    public ReactorModelResolver( RepositorySystemSession session, ArtifactResolver resolver,
                                 RemoteRepositoryManager remoteRepositoryManager, List<RemoteRepository> repositories,
                                 ReactorModelPool modelPool )
    {
        this.session = session;
        this.resolver = resolver;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.pomRepositories = new ArrayList<RemoteRepository>();
        this.defaultRepositories = repositories;
        this.repositories = repositories;
        this.repositoryIds = new HashSet<String>();
        this.modelPool = modelPool;
    }

    private ReactorModelResolver( ReactorModelResolver original )
    {
        this.session = original.session;
        this.resolver = original.resolver;
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.pomRepositories = original.pomRepositories;
        this.defaultRepositories = original.defaultRepositories;
        this.repositories = original.repositories;
        this.repositoryIds = new HashSet<String>( original.repositoryIds );
        this.modelPool = original.modelPool;
    }

    public void addRepository( Repository repository )
        throws InvalidRepositoryException
    {
        if ( !repositoryIds.add( repository.getId() ) )
        {
            return;
        }

        List<RemoteRepository> newRepositories = Collections.singletonList( convert( repository ) );

        pomRepositories =
            remoteRepositoryManager.aggregateRepositories( session, pomRepositories, newRepositories, true );
        repositories =
            remoteRepositoryManager.aggregateRepositories( session, pomRepositories, defaultRepositories, false );
    }

    private static RemoteRepository convert( Repository repository )
    {
        RemoteRepository result =
            new RemoteRepository( repository.getId(), repository.getLayout(), repository.getUrl() );
        result.setPolicy( true, convert( repository.getSnapshots() ) );
        result.setPolicy( false, convert( repository.getReleases() ) );
        return result;
    }

    private static RepositoryPolicy convert( org.apache.maven.model.RepositoryPolicy policy )
    {
        boolean enabled = true;
        String checksums = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        String updates = RepositoryPolicy.UPDATE_POLICY_DAILY;

        if ( policy != null )
        {
            enabled = policy.isEnabled();
            if ( policy.getUpdatePolicy() != null )
            {
                updates = policy.getUpdatePolicy();
            }
            if ( policy.getChecksumPolicy() != null )
            {
                checksums = policy.getChecksumPolicy();
            }
        }

        return new RepositoryPolicy( enabled, updates, checksums );
    }

    public ModelResolver newCopy()
    {
        return new ReactorModelResolver( this );
    }

    public ModelSource resolveModel( String groupId, String artifactId, String version )
        throws UnresolvableModelException
    {
        File pomFile = null;

        if ( modelPool != null )
        {
            pomFile = modelPool.get( groupId, artifactId, version );
        }

        if ( pomFile == null )
        {
            Artifact pomArtifact = new DefaultArtifact( groupId, artifactId, "", "pom", version );

            try
            {
                ArtifactRequest request = new ArtifactRequest( pomArtifact, repositories, context );
                pomArtifact = resolver.resolveArtifact( session, request ).getArtifact();
            }
            catch ( ArtifactResolutionException e )
            {
                throw new UnresolvableModelException( "Failed to resolve POM for " + groupId + ":" + artifactId + ":"
                    + version + " due to " + e.getMessage(), groupId, artifactId, version, e );
            }

            pomFile = pomArtifact.getFile();
        }

        return new FileModelSource( pomFile );
    }

}
