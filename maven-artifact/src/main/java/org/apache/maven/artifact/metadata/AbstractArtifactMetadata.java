package org.apache.maven.artifact.metadata;

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

/**
 * Common elements of artifact metadata.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractArtifactMetadata
    implements ArtifactMetadata
{
    protected final String filename;

    protected Artifact artifact;

    protected AbstractArtifactMetadata( Artifact artifact, String filename )
    {
        this.artifact = artifact;
        this.filename = filename;
    }

    public String getFilename()
    {
        return filename;
    }
    
    public boolean storedInArtifactDirectory()
    {
        return true;
    }

    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    public String getVersion()
    {
        return artifact.getVersion();
    }

    public String getBaseVersion()
    {
        return artifact.getBaseVersion();
    }

}
