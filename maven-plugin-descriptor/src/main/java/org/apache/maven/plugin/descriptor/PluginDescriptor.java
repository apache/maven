package org.apache.maven.plugin.descriptor;

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
import org.apache.maven.plugin.lifecycle.Lifecycle;
import org.apache.maven.plugin.lifecycle.LifecycleConfiguration;
import org.apache.maven.plugin.lifecycle.io.xpp3.LifecycleMappingsXpp3Reader;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginDescriptor
    extends ComponentSetDescriptor
{

    private static final String LIFECYCLE_DESCRIPTOR = "META-INF/maven/lifecycle.xml";

    private String groupId;

    private String artifactId;

    private String version;

    private String goalPrefix;

    private String source;

    private boolean inheritedByDefault = true;
    
    private Artifact pluginArtifact;

    private List<Artifact> artifacts;

    private Map<String, Lifecycle> lifecycleMappings;

    private ClassRealm classRealm;

    // calculated on-demand.
    private Map<String, Artifact> artifactMap;

    private Set<Artifact> introducedDependencyArtifacts;

    private String name;

    private String description;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public List<MojoDescriptor> getMojos()
    {
        return getComponents();
    }

    public void addMojo( MojoDescriptor mojoDescriptor )
        throws DuplicateMojoDescriptorException
    {
        MojoDescriptor existing = null;
        // this relies heavily on the equals() and hashCode() for ComponentDescriptor,
        // which uses role:roleHint for identity...and roleHint == goalPrefix:goal.
        // role does not vary for Mojos.
        List<MojoDescriptor> mojos = getMojos();

        if ( mojos != null && mojos.contains( mojoDescriptor ) )
        {
            int indexOf = mojos.indexOf( mojoDescriptor );

            existing = (MojoDescriptor) mojos.get( indexOf );
        }

        if ( existing != null )
        {
            throw new DuplicateMojoDescriptorException( getGoalPrefix(), mojoDescriptor.getGoal(), existing
                .getImplementation(), mojoDescriptor.getImplementation() );
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
    
    private String lookupKey;

    public String getPluginLookupKey()
    {
        if ( lookupKey == null )
        {
            lookupKey = ( groupId + ":" + artifactId ).intern();
        }
        
        return lookupKey;
    }
    
    private String id;

    public String getId()
    {
        if ( id == null )
        {
            id = constructPluginKey( groupId, artifactId, version ).intern();
        }
        
        return id;
    }

    public static String getDefaultPluginArtifactId( String id )
    {
        return "maven-" + id + "-plugin";
    }

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

    public List<Artifact> getArtifacts()
    {
        return artifacts;
    }

    public void setArtifacts( List<Artifact> artifacts )
    {
        this.artifacts = artifacts;

        // clear the calculated artifactMap
        artifactMap = null;
    }

    public Map<String, Artifact> getArtifactMap()
    {
        if ( artifactMap == null )
        {
            artifactMap = ArtifactUtils.artifactMapByVersionlessId( getArtifacts() );
        }

        return artifactMap;
    }

    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        return getId().equals( ( (PluginDescriptor) object ).getId() );
    }

    public int hashCode()
    {
        return 10 + getId().hashCode();
    }

    public MojoDescriptor getMojo( String goal )
    {
        if ( getMojos() == null )
        {
            return null; // no mojo in this POM
        }

        // TODO: could we use a map? Maybe if the parent did that for components too, as this is too vulnerable to
        // changes above not being propagated to the map
        for ( MojoDescriptor desc : getMojos() )
        {
            if ( goal.equals( desc.getGoal() ) )
            {
                return desc;
            }
        }
        return null;
    }

    public Lifecycle getLifecycleMapping( String lifecycle )
        throws IOException, XmlPullParserException
    {
        if ( lifecycleMappings == null )
        {
            LifecycleConfiguration config;

            Reader reader = null;
            try
            {
                InputStream resourceAsStream = classRealm.getResourceAsStream( LIFECYCLE_DESCRIPTOR );
                if ( resourceAsStream == null )
                {
                    throw new FileNotFoundException( "Unable to find " + LIFECYCLE_DESCRIPTOR + " in the plugin" );
                }
                reader = ReaderFactory.newXmlReader( resourceAsStream );
                config = new LifecycleMappingsXpp3Reader().read( reader, true );
            }
            finally
            {
                IOUtil.close( reader );
            }

            lifecycleMappings = new HashMap<String, Lifecycle>();

            for ( Lifecycle l : config.getLifecycles() )
            {
                lifecycleMappings.put( l.getId(), l );
            }
        }
        return lifecycleMappings.get( lifecycle );
    }

    public void setClassRealm( ClassRealm classRealm )
    {
        this.classRealm = classRealm;
    }

    public ClassRealm getClassRealm()
    {
        return classRealm;
    }

    public void setIntroducedDependencyArtifacts( Set<Artifact> introducedDependencyArtifacts )
    {
        this.introducedDependencyArtifacts = introducedDependencyArtifacts;
    }

    public Set<Artifact> getIntroducedDependencyArtifacts()
    {
        return ( introducedDependencyArtifacts != null ) ? introducedDependencyArtifacts
                        : Collections.<Artifact> emptySet();
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public Artifact getPluginArtifact()
    {
        return pluginArtifact;
    }

    public void setPluginArtifact( Artifact pluginArtifact )
    {
        this.pluginArtifact = pluginArtifact;
    }
}
