package org.apache.maven.artifact.repository;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.wagon.repository.Repository;

/**
 * This class is an abstraction of the location from/to resources can be
 * transfered.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public class DefaultArtifactRepository
    extends Repository
    implements ArtifactRepository
{
    private final ArtifactRepositoryLayout layout;

    private ArtifactRepositoryPolicy snapshots;

    private ArtifactRepositoryPolicy releases;

    /**
     * Create a local repository or a deployment repository.
     *
     * @param id the unique identifier of the repository
     * @param url the URL of the repository
     * @param layout the layout of the repository
     */
    public DefaultArtifactRepository( String id, String url, ArtifactRepositoryLayout layout )
    {
        this( id, url, layout, null, null );
    }

    /**
     * Create a remote download repository.
     *
     * @param id the unique identifier of the repository
     * @param url the URL of the repository
     * @param layout the layout of the repository
     * @param snapshots the policies to use for snapshots
     * @param releases the policies to use for releases
     */
    public DefaultArtifactRepository( String id, String url, ArtifactRepositoryLayout layout,
                                      ArtifactRepositoryPolicy snapshots, ArtifactRepositoryPolicy releases )
    {
        super( id, url );

        this.layout = layout;

        this.snapshots = snapshots;

        this.releases = releases;
    }

    public String pathOf( Artifact artifact )
    {
        return layout.pathOf( artifact );
    }

    public String pathOfMetadata( ArtifactMetadata artifactMetadata )
    {
        return layout.pathOfMetadata( artifactMetadata );
    }

    public String formatAsDirectory( String directory )
    {
        return layout.formatAsDirectory( directory );
    }

    public String formatAsFile( String file )
    {
        return layout.formatAsFile( file );
    }

    public ArtifactRepositoryLayout getLayout()
    {
        return layout;
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        return snapshots;
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        return releases;
    }
}
