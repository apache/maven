package org.apache.maven.artifact;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultArtifact
    implements Artifact
{
    // ----------------------------------------------------------------------
    // These are the only things i need to specify
    // ----------------------------------------------------------------------

    private String groupId;

    private String artifactId;

    private String version;

    private String type;



    private String extension;

    private String path;

    public DefaultArtifact( String groupId, String artifactId, String version, String type, String extension )
    {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.version = version;

        this.type = type;

        this.extension = extension;
    }

    public DefaultArtifact( String groupId, String artifactId, String version, String type )
    {
        this( groupId, artifactId, version, type, type );
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

    public String getExtension()
    {
        if ( extension != null )
        {
            return extension;
        }

        return type;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public boolean exists()
    {
        return getFile().exists();
    }

    public File getFile()
    {
        return new File( getPath() );
    }

    public File getChecksumFile()
    {
        return new File( getFile().getAbsolutePath() + ".md5" );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String toString()
    {
        return getId();
    }

    public String getId()
    {
        return getGroupId() + ":" +
            getArtifactId() + ":" +
            getType() + ":" +
            getVersion();
    }

    public String getConflictId()
    {
        return getGroupId() + ":" +
            getArtifactId() + ":" +
            getType();
    }
}
