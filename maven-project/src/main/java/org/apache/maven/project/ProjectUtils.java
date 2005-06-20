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
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Repository;
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

            ArtifactRepository artifactRepo = buildArtifactRepository( mavenRepo, artifactRepositoryFactory, container );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }
        return repos;
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
            String snapshotPolicy = repo.getSnapshotPolicy();
            String checksumPolicy = repo.getChecksumPolicy();
            
            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo, container );
            
            return artifactRepositoryFactory.createArtifactRepository( id, url, layout, snapshotPolicy, checksumPolicy );
        }
        else
        {
            return null;
        }
    }

    private static ArtifactRepositoryLayout getRepositoryLayout( Repository mavenRepo, PlexusContainer container )
        throws ProjectBuildingException
    {
        String layout = mavenRepo.getLayout();

        ArtifactRepositoryLayout repositoryLayout = null;
        try
        {
            repositoryLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
        }
        catch ( ComponentLookupException e )
        {
            throw new ProjectBuildingException( "Cannot find layout implementation corresponding to: \'" + layout
                + "\' for remote repository with id: \'" + mavenRepo.getId() + "\'.", e );
        }
        return repositoryLayout;
    }

}
