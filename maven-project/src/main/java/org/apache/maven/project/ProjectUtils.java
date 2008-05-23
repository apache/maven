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
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.codehaus.plexus.PlexusContainer;

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
        throws InvalidRepositoryException
    {

        List repos = new ArrayList();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            Repository mavenRepo = (Repository) i.next();

            ArtifactRepository artifactRepo =
                buildArtifactRepository( mavenRepo, artifactRepositoryFactory, container );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }
        return repos;
    }

    public static ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo,
                                                                        ArtifactRepositoryFactory artifactRepositoryFactory,
                                                                        PlexusContainer container )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            return artifactRepositoryFactory.createDeploymentArtifactRepository( id, url, repo.getLayout(),
                                                                                 repo.isUniqueVersion() );
        }
        else
        {
            return null;
        }
    }

    public static ArtifactRepository buildArtifactRepository( Repository repo,
                                                              ArtifactRepositoryFactory artifactRepositoryFactory,
                                                              PlexusContainer container )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            if ( id == null || id.trim().length() < 1 )
            {
                throw new MissingRepositoryElementException( "Repository ID must not be empty (URL is: " + url + ")." );
            }

            if ( url == null || url.trim().length() < 1 )
            {
                throw new MissingRepositoryElementException( "Repository URL must not be empty (ID is: " + id + ").", id );
            }

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );
            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, repo.getLayout(), snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    private static ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy )
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

}
