package org.apache.maven.tools.repoclean.artifact.metadata;

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
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Attach a POM to an artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ProjectMetadata
    implements ArtifactMetadata
{
    private final Artifact artifact;

    public ProjectMetadata( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public String getFilename()
    {
        return getArtifact().getArtifactId() + "-" + getArtifact().getVersion() + ".pom";
    }

    public void storeInLocalRepository( ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        // not used in repoclean.
    }

    public void retrieveFromRemoteRepository( ArtifactRepository remoteRepository, WagonManager wagonManager )
    {
        // not used - TODO: again indicates bad design?
    }

    public void setArtifact( Artifact artifact )
    {
        // this should be immutable...
    }

    public boolean exists()
    {
        return false;
    }

    public String getGroupId()
    {
        return null;
    }

    public String getArtifactId()
    {
        return null;
    }

    public String getVersion()
    {
        return null;
    }

    public String getBaseVersion()
    {
        return null;
    }
}
