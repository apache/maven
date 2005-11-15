package org.apache.maven.bootstrap.model;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Describes a dependency.
 *
 * @version $Id$
 */
public class Plugin
{
    private String version;

    private String artifactId;

    private String groupId;

    private Map configuration = new HashMap();

    public String getId()
    {
        return getGroupId() + ":" + getArtifactId();
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public String toString()
    {
        return getId() + ":" + getVersion();
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + groupId.hashCode();
        result = 37 * result + artifactId.hashCode();
        result = 37 * result + version.hashCode();
        return result;
    }

    public boolean equals( Object o )
    {
        if ( o == this )
        {
            return true;
        }

        if ( !( o instanceof Plugin ) )
        {
            return false;
        }

        Plugin d = (Plugin) o;

        if ( !d.getGroupId().equals( groupId ) )
        {
            return false;
        }
        else if ( !d.getArtifactId().equals( artifactId ) )
        {
            return false;
        }
        else if ( !d.getVersion().equals( version ) )
        {
            return false;
        }
        return true;
    }

    public Map getConfiguration()
    {
        return configuration;
    }

    public Dependency asDependency()
    {
        return new Dependency( groupId, artifactId, version, "maven-plugin", Collections.EMPTY_LIST );
    }

    public Dependency asDependencyPom()
    {
        return new Dependency( groupId, artifactId, version, "pom", Collections.EMPTY_LIST );
    }
}
