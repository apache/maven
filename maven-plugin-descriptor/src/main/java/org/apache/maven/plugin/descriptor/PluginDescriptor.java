package org.apache.maven.plugin.descriptor;

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

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginDescriptor
{
    private List mojos;

    private String groupId;

    private String artifactId;

    private List dependencies;

    private boolean isolatedRealm;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public List getMojos()
    {
        return mojos;
    }

    public void setMojos( List mojos )
    {
        this.mojos = new LinkedList( mojos );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    // ----------------------------------------------------------------------
    // Dependencies
    // ----------------------------------------------------------------------

    public List getDependencies()
    {
        return dependencies;
    }

    public void setDependencies( List dependencies )
    {
        this.dependencies = new LinkedList( dependencies );
    }

    public boolean isIsolatedRealm()
    {
        return isolatedRealm;
    }

    public static String constructPluginKey( String groupId, String artifactId )
    {
        return groupId + ":" + artifactId;
    }

    public String getId()
    {
        return constructPluginKey( groupId, artifactId );
    }

    /**
     * @todo remove - harcoding.
     */
    public static String getPluginIdFromGoal( String goalName )
    {
        String pluginId = goalName;

        if ( pluginId.indexOf( ":" ) > 0 )
        {
            pluginId = pluginId.substring( 0, pluginId.indexOf( ":" ) );
        }

        return getDefaultPluginArtifactId( pluginId );
    }

    /**
     * @todo remove - harcoding.
     */
    public static String getDefaultPluginArtifactId( String id )
    {
        return "maven-" + id + "-plugin";
    }

    /**
     * @todo remove - harcoding.
     */
    public static String getDefaultPluginGroupId()
    {
        return "org.apache.maven.plugins";
    }

    /**
     * Parse maven-...-plugin.
     *
     * @todo remove - harcoding. What about clashes?
     */
    public static String getPluginIdFromArtifactId( String artifactId )
    {
        int firstHyphen = artifactId.indexOf( "-" );

        int lastHyphen = artifactId.lastIndexOf( "-" );

        return artifactId.substring( firstHyphen + 1, lastHyphen );
    }
}
