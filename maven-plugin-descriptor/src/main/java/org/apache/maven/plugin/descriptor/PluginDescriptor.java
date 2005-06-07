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

import org.codehaus.plexus.component.repository.ComponentSetDescriptor;

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginDescriptor
    extends ComponentSetDescriptor
{
    private String groupId;

    private String artifactId;

    private String version;

    private List dependencies;

    private String goalPrefix;

    private String source;

    private boolean inheritedByDefault = true;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public List getMojos()
    {
        return getComponents();
    }

    public void addMojo( MojoDescriptor mojoDescriptor )
        throws DuplicateMojoDescriptorException
    {
        // this relies heavily on the equals() and hashCode() for ComponentDescriptor, 
        // which uses role:roleHint for identity...and roleHint == goalPrefix:goal.
        // role does not vary for Mojos.
        List mojos = getComponents();

        if ( mojos != null && mojos.contains( mojoDescriptor ) )
        {
            int indexOf = mojos.indexOf( mojoDescriptor );

            MojoDescriptor existing = (MojoDescriptor) mojos.get( indexOf );

            throw new DuplicateMojoDescriptorException( getGoalPrefix(), mojoDescriptor.getGoal(), existing.getImplementation(), mojoDescriptor.getImplementation() );
        }
        else
        {
            addComponentDescriptor( mojoDescriptor );
        }
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

    public static String constructPluginKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }
    
    public String getPluginLookupKey()
    {
        return groupId + ":" + artifactId;
    }
    
    public String getId()
    {
        String id = constructPluginKey( groupId, artifactId, version );
        if ( groupId == null || artifactId == null || version == null )
        {
            throw new IllegalStateException( "Plugin descriptor ID incomplete: " + id + " in " + getSource() );
        }
        return id;
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
     * @todo move to plugin-tools-api as a default only
     */
    public static String getGoalPrefixFromArtifactId( String artifactId )
    {
        if ( "maven-plugin-plugin".equals( artifactId ) )
        {
            return "plugin";
        }
        else
        {
            return artifactId.replaceAll( "-?maven-?", "" ).replaceAll( "-?plugin-?", "" );
        }
    }

    public String getGoalPrefix()
    {
        return goalPrefix;
    }

    public void setGoalPrefix( String goalPrefix )
    {
        this.goalPrefix = goalPrefix;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public void setSource( String source )
    {
        this.source = source;
    }

    public String getSource()
    {
        return source;
    }

    public boolean isInheritedByDefault()
    {
        return inheritedByDefault;
    }

    public void setInheritedByDefault( boolean inheritedByDefault )
    {
        this.inheritedByDefault = inheritedByDefault;
    }
}
