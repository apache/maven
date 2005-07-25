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

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * @author jdcasey
 */
public class DefaultArtifactRepositoryFactory
    implements ArtifactRepositoryFactory
{
    // TODO: use settings?
    private String globalUpdatePolicy;

    private String globalChecksumPolicy;

    private boolean globalEnable = true;

    public ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout )
    {
        return new DefaultArtifactRepository( id, url, repositoryLayout );
    }

    public ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
    {
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

        // TODO: needed, or can offline cover it?
        if ( !globalEnable )
        {
            snapshots.setEnabled( false );
            releases.setEnabled( false );
        }

        return new DefaultArtifactRepository( id, url, repositoryLayout, snapshots, releases );
    }

    public void setGlobalUpdatePolicy( String updatePolicy )
    {
        this.globalUpdatePolicy = updatePolicy;
    }

    public void setGlobalChecksumPolicy( String checksumPolicy )
    {
        this.globalChecksumPolicy = checksumPolicy;
    }

    public void setGlobalEnable( boolean enable )
    {
        this.globalEnable = enable;
    }
}
