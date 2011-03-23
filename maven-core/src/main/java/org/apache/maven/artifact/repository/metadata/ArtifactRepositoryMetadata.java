package org.apache.maven.artifact.repository.metadata;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Metadata for the artifact directory of the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ArtifactRepositoryMetadata
    extends AbstractRepositoryMetadata
{
    private Artifact artifact;

    public ArtifactRepositoryMetadata( Artifact artifact )
    {
        this( artifact, null );
    }

    public ArtifactRepositoryMetadata( Artifact artifact,
                                       Versioning versioning )
    {
        super( createMetadata( artifact, versioning ) );
        this.artifact = artifact;
    }

    public boolean storedInGroupDirectory()
    {
        return false;
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return false;
    }

    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    public String getBaseVersion()
    {
        // Don't want the artifact's version in here, as this is stored in the directory above that
        return null;
    }

    public Object getKey()
    {
        return "artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    public boolean isSnapshot()
    {
        // Don't consider the artifact's version in here, as this is stored in the directory above that
        return false;
    }

    public int getNature()
    {
        if ( artifact.getVersion() != null )
        {
            return artifact.isSnapshot() ? SNAPSHOT : RELEASE;
        }

        VersionRange range = artifact.getVersionRange();
        if ( range != null )
        {
            for ( Restriction restriction : range.getRestrictions() )
            {
                if ( isSnapshot( restriction.getLowerBound() ) || isSnapshot( restriction.getUpperBound() ) )
                {
                    return RELEASE_OR_SNAPSHOT;
                }
            }
        }

        return RELEASE;
    }

    private boolean isSnapshot( ArtifactVersion version )
    {
        return version != null && ArtifactUtils.isSnapshot( version.getQualifier() );
    }

    public ArtifactRepository getRepository()
    {
        return null;
    }

    public void setRepository( ArtifactRepository remoteRepository )
    {
        /*
         * NOTE: Metadata at the g:a level contains a collection of available versions. After merging, we can't tell
         * which repository provides which version so the metadata manager must not restrict the artifact resolution to
         * the repository with the most recent updates.
         */
    }

}
