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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** @author Jason van Zyl */
public class DefaultMavenTools
    implements MavenTools, Contextualizable
{
    private ArtifactRepositoryLayout repositoryLayout;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private PlexusContainer container;

    // ----------------------------------------------------------------------------
    // Code snagged from ProjectUtils: this will have to be moved somewhere else
    // but just trying to collect it all in one place right now.
    // ----------------------------------------------------------------------------

    public List buildArtifactRepositories( List repositories )
        throws InvalidRepositoryException
    {
        List repos = new ArrayList();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            Repository mavenRepo = (Repository) i.next();

            ArtifactRepository artifactRepo = buildArtifactRepository( mavenRepo );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }
        return repos;
    }

    public ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo );

            return artifactRepositoryFactory.createDeploymentArtifactRepository( id, url, layout,
                                                                                 repo.isUniqueVersion() );
        }
        else
        {
            return null;
        }
    }

    public ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo );

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );

            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, layout, snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    public ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy )
    {
        boolean enabled = true;

        String updatePolicy = null;

        String checksumPolicy = null;

        if ( policy != null )
        {
            enabled = policy.isEnabled();

            if ( policy.getUpdatePolicy() != null )
            {
                updatePolicy = policy.getUpdatePolicy();
            }
            if ( policy.getChecksumPolicy() != null )
            {
                checksumPolicy = policy.getChecksumPolicy();
            }
        }

        return new ArtifactRepositoryPolicy( enabled, updatePolicy, checksumPolicy );
    }

    private ArtifactRepositoryLayout getRepositoryLayout( RepositoryBase mavenRepo )
        throws InvalidRepositoryException
    {
        String layout = mavenRepo.getLayout();

        ArtifactRepositoryLayout repositoryLayout;
        try
        {
            repositoryLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
        }
        catch ( ComponentLookupException e )
        {
            throw new InvalidRepositoryException( "Cannot find layout implementation corresponding to: \'" + layout +
                "\' for remote repository with id: \'" + mavenRepo.getId() + "\'.", e );
        }
        return repositoryLayout;
    }

    // ----------------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
