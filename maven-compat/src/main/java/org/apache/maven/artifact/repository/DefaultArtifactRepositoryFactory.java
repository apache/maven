package org.apache.maven.artifact.repository;

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
import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jdcasey
 * @plexus.component
 */
public class DefaultArtifactRepositoryFactory
    implements ArtifactRepositoryFactory
{
    // TODO: use settings?
    private String globalUpdatePolicy;

    private String globalChecksumPolicy;

    // FIXME: This is a non-ThreadLocal cache!!
    private final Map<String,ArtifactRepository> artifactRepositories = new HashMap<String,ArtifactRepository>();

    /** @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout" */
    private Map<String,ArtifactRepositoryLayout> repositoryLayouts;

    public ArtifactRepositoryLayout getLayout( String layoutId )
        throws UnknownRepositoryLayoutException
    {
        return repositoryLayouts.get( layoutId );
    }

    public ArtifactRepository createDeploymentArtifactRepository( String id,
                                                                  String url,
                                                                  String layoutId,
                                                                  boolean uniqueVersion )
        throws UnknownRepositoryLayoutException
    {
        ArtifactRepositoryLayout layout = repositoryLayouts.get( layoutId );

        checkLayout( id, layoutId, layout );

        return createDeploymentArtifactRepository( id, url, layout, uniqueVersion );
    }

    private void checkLayout( String repositoryId,
                              String layoutId,
                              ArtifactRepositoryLayout layout )
        throws UnknownRepositoryLayoutException
    {
        if ( layout == null )
        {
            throw new UnknownRepositoryLayoutException( repositoryId, layoutId );
        }
    }

    public ArtifactRepository createDeploymentArtifactRepository( String id,
                                                                  String url,
                                                                  ArtifactRepositoryLayout repositoryLayout,
                                                                  boolean uniqueVersion )
    {
        return new DefaultArtifactRepository( id, url, repositoryLayout, uniqueVersion );
    }

    public ArtifactRepository createArtifactRepository( String id,
                                                        String url,
                                                        String layoutId,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
        throws UnknownRepositoryLayoutException
    {
        ArtifactRepositoryLayout layout = repositoryLayouts.get( layoutId );

        checkLayout( id, layoutId, layout );

        return createArtifactRepository( id, url, layout, snapshots, releases );
    }

    public ArtifactRepository createArtifactRepository( String id,
                                                        String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
    {
        boolean blacklisted = false;
        if ( artifactRepositories.containsKey( id ) )
        {
            ArtifactRepository repository = artifactRepositories.get( id );
            // TODO: this should be an if there are duplicates?
            if ( repository.getUrl().equals( url ) )
            {
                blacklisted = repository.isBlacklisted();
            }
        }

        if ( snapshots == null )
        {
            snapshots = new ArtifactRepositoryPolicy();
        }

        if ( releases == null )
        {
            releases = new ArtifactRepositoryPolicy();
        }

        if ( globalUpdatePolicy != null )
        {
            snapshots.setUpdatePolicy( globalUpdatePolicy );
            releases.setUpdatePolicy( globalUpdatePolicy );
        }

        if ( globalChecksumPolicy != null )
        {
            snapshots.setChecksumPolicy( globalChecksumPolicy );
            releases.setChecksumPolicy( globalChecksumPolicy );
        }

        DefaultArtifactRepository repository = new DefaultArtifactRepository( id, url, repositoryLayout, snapshots, releases );
        repository.setBlacklisted( blacklisted );

        artifactRepositories.put( id, repository );

        return repository;
    }

    public void setGlobalUpdatePolicy( String updatePolicy )
    {
        globalUpdatePolicy = updatePolicy;
    }

    public void setGlobalChecksumPolicy( String checksumPolicy )
    {
        globalChecksumPolicy = checksumPolicy;
    }

    public ArtifactRepository createLocalRepository( File localRepositoryDirectory )
        throws InvalidRepositoryException
    {
        ArtifactRepositoryLayout layout = getLayout( DEFAULT_LAYOUT_ID );
        DefaultArtifactRepository repo;
        try
        {
            repo = new DefaultArtifactRepository( LOCAL_REPOSITORY_ID,
                localRepositoryDirectory.toURI()
                    .toURL()
                    .toExternalForm(),
                layout, new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy() );
        }
        catch ( MalformedURLException e )
        {
            throw new InvalidRepositoryException( "Invalid local repository directory: "
                + localRepositoryDirectory
                + ". Cannot render URL.", LOCAL_REPOSITORY_ID, e );
        }

        repo.setBasedir( localRepositoryDirectory.getAbsolutePath() );

        return repo;
    }
}
