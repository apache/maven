package model;

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
 * Describes a dependency.
 *
 * @version $Id$
 */
public class Dependency
{
    private String id;

    private String version;

    private String url;

    private String jar;

    private String artifactId;

    private String groupId;

    private String type = "jar";

    private String scope = "compile";

    public Dependency()
    {
    }

    public Dependency( String groupId, String artifactId, String version )
    {
        this.version = version;
        this.artifactId = artifactId;
        this.groupId = groupId;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getId()
    {
        if ( isValid( getGroupId() ) && isValid( getArtifactId() ) )
        {
            return getGroupId() + ":" + getArtifactId();
        }

        return id;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactDirectory()
    {
        if ( isValid( getGroupId() ) )
        {
            return getGroupId();
        }

        return getId();
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getArtifact()
    {
        // If the jar name has been explicty set then use that. This
        // is when the <jar/> element is explicity used in the POM.
        if ( jar != null )
        {
            return jar;
        }

        String artifact;

        if ( isValid( getArtifactId() ) )
        {
            artifact = getArtifactId() + "-" + getVersion() + ".";
        }
        else
        {
            artifact = getId() + "-" + getVersion() + ".";
        }

        if ( "jar".equals( getType() ) || "maven-plugin".equals( getType() ) )
        {
            artifact += "jar";
        }
        else
        {
            artifact += getType();
        }
        return artifact;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public void setJar( String jar )
    {
        // This is a check we need because of the jelly interpolation
        // process. If we don't check an empty string will be set and
        // screw up getArtifact() above.
        if ( jar.trim().length() == 0 )
        {
            return;
        }

        this.jar = jar;
    }

    public String getJar()
    {
        return jar;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    private boolean isValid( String value )
    {
        if ( value != null && value.trim().equals( "" ) == false )
        {
            return true;
        }

        return false;
    }

    public String getRepositoryPath()
    {
        return getArtifactDirectory() + "/" + getType() + "s/" + getArtifact();
    }

    public String toString()
    {
        return getRepositoryPath();
    }
}
