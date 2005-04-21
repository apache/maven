package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.Artifact;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArtifactResolutionException
    extends Exception
{
    private String groupId;

    private String artifactId;

    private String version;

    private String type;

    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type, Throwable t )
    {
        super( "Unable to resolve artifact " + groupId + ":" + artifactId + ":" + version + ":" + type + "\n" + message, t );

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
    }

    public ArtifactResolutionException( String message, Artifact artifact, Throwable t )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), t );
    }

    public ArtifactResolutionException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getType()
    {
        return type;
    }
}
