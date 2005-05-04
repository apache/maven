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

    private String goalPrefix;

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
    public static String getPrefixFromGoal( String goalName )
    {
        String prefix = goalName;

        if ( prefix.indexOf( ":" ) > 0 )
        {
            prefix = prefix.substring( 0, prefix.indexOf( ":" ) );
        }
        return prefix;
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
    public static String getGoalPrefixFromArtifactId( String artifactId )
    {
        return artifactId.replaceAll( "-?maven-?", "" ).replaceAll( "-?plugin-?", "" );
    }

    /**
     * @todo remove - harcoding. What about clashes?
     */
    public static String getDefaultPluginVersion()
    {
        return "1.0-SNAPSHOT";
    }

    public static String getGoalIdFromFullGoal( String goalName )
    {
        // TODO: much less of this magic is needed - make the mojoDescriptor just store the first and second part
        int index = goalName.indexOf( ':' );
        if ( index >= 0 )
        {
            return goalName.substring( index + 1 );
        }
        return null;
    }

    public String getGoalPrefix()
    {
        return goalPrefix;
    }

    public void setGoalPrefix( String goalPrefix )
    {
        this.goalPrefix = goalPrefix;
    }
}
