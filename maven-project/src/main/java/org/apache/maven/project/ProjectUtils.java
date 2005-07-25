package org.apache.maven.project;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ProjectUtils
{
    private ProjectUtils()
    {
    }

    public static List buildArtifactRepositories( List repositories,
                                                  ArtifactRepositoryFactory artifactRepositoryFactory,
                                                  PlexusContainer container )
        throws ProjectBuildingException
    {

        List repos = new ArrayList();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            Repository mavenRepo = (Repository) i.next();

            ArtifactRepository artifactRepo = buildArtifactRepository( mavenRepo, artifactRepositoryFactory,
                                                                       container );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }
        return repos;
    }

    public static ArtifactRepository buildArtifactRepositoryBase( RepositoryBase repo,
                                                                  ArtifactRepositoryFactory artifactRepositoryFactory,
                                                                  PlexusContainer container )
        throws ProjectBuildingException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo, container );

            return artifactRepositoryFactory.createArtifactRepository( id, url, layout );
        }
        else
        {
            return null;
        }
    }

    public static ArtifactRepository buildArtifactRepository( Repository repo,
                                                              ArtifactRepositoryFactory artifactRepositoryFactory,
                                                              PlexusContainer container )
        throws ProjectBuildingException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo, container );

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots(),
                                                                                repo.getSnapshotPolicy(),
                                                                                repo.getChecksumPolicy() );
            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases(),
                                                                               repo.getSnapshotPolicy(),
                                                                               repo.getChecksumPolicy() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, layout, snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    private static ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy,
                                                                           String defaultUpdatePolicy,
                                                                           String defaultChecksumPolicy )
    {
        boolean enabled = true;
        String updatePolicy = defaultUpdatePolicy;
        String checksumPolicy = defaultChecksumPolicy;

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

    private static ArtifactRepositoryLayout getRepositoryLayout( RepositoryBase mavenRepo, PlexusContainer container )
        throws ProjectBuildingException
    {
        String layout = mavenRepo.getLayout();

        ArtifactRepositoryLayout repositoryLayout;
        try
        {
            repositoryLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
        }
        catch ( ComponentLookupException e )
        {
            throw new ProjectBuildingException( "Cannot find layout implementation corresponding to: \'" + layout +
                "\' for remote repository with id: \'" + mavenRepo.getId() + "\'.", e );
        }
        return repositoryLayout;
    }

}
