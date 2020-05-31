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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Jason van Zyl
 */
public class PluginDescriptorBuilder
{
    public PluginDescriptor build( Reader reader )
        throws PlexusConfigurationException
    {
        return build( reader, null );
    }

    public PluginDescriptor build( Reader reader, String source )
        throws PlexusConfigurationException
    {
        return build( source, buildConfiguration( reader ) );
    }

    private PluginDescriptor build( String source, PlexusConfiguration c )
        throws PlexusConfigurationException
    {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setSource( source );
        pluginDescriptor.setGroupId( extractGroupId( c ) );
        pluginDescriptor.setArtifactId( extractArtifactId( c ) );
        pluginDescriptor.setVersion( extractVersion( c ) );
        pluginDescriptor.setGoalPrefix( extractGoalPrefix( c ) );

        pluginDescriptor.setName( extractName( c ) );
        pluginDescriptor.setDescription( extractDescription( c ) );

        pluginDescriptor.setIsolatedRealm( extractIsolatedRealm( c ) );
        pluginDescriptor.setInheritedByDefault( extractInheritedByDefault( c ) );

        pluginDescriptor.addMojos( extractMojos( c, pluginDescriptor ) );

        pluginDescriptor.setDependencies( extractComponentDependencies( c ) );

        return pluginDescriptor;
    }

    private String extractGroupId( PlexusConfiguration c )
    {
        return c.getChild( "groupId" ).getValue();
    }

    private String extractArtifactId( PlexusConfiguration c )
    {
        return c.getChild( "artifactId" ).getValue();
    }

    private String extractVersion( PlexusConfiguration c )
    {
        return c.getChild( "version" ).getValue();
    }

    private String extractGoalPrefix( PlexusConfiguration c )
    {
        return c.getChild( "goalPrefix" ).getValue();
    }

    private String extractName( PlexusConfiguration c )
    {
        return c.getChild( "name" ).getValue();
    }

    private String extractDescription( PlexusConfiguration c )
    {
        return c.getChild( "description" ).getValue();
    }

    private List<MojoDescriptor> extractMojos( PlexusConfiguration c, PluginDescriptor pluginDescriptor )
        throws PlexusConfigurationException
    {
        List<MojoDescriptor> mojos = new ArrayList<>();

        PlexusConfiguration[] mojoConfigurations = c.getChild( "mojos" ).getChildren( "mojo" );

        for ( PlexusConfiguration component : mojoConfigurations )
        {
            mojos.add( buildComponentDescriptor( component, pluginDescriptor ) );

        }
        return mojos;
    }

    private boolean extractInheritedByDefault( PlexusConfiguration c )
    {
        String inheritedByDefault = c.getChild( "inheritedByDefault" ).getValue();

        if ( inheritedByDefault != null )
        {
            return Boolean.parseBoolean( inheritedByDefault );
        }
        return false;
    }

    private boolean extractIsolatedRealm( PlexusConfiguration c )
    {
        String isolatedRealm = c.getChild( "isolatedRealm" ).getValue();

        if ( isolatedRealm != null )
        {
            return Boolean.parseBoolean( isolatedRealm );
        }
        return false;
    }

    private List<ComponentDependency> extractComponentDependencies( PlexusConfiguration c )
    {

        PlexusConfiguration[] dependencyConfigurations = c.getChild( "dependencies" ).getChildren( "dependency" );

        List<ComponentDependency> dependencies = new ArrayList<>();

        for ( PlexusConfiguration d : dependencyConfigurations )
        {
            dependencies.add( extractComponentDependency( d ) );
        }
        return dependencies;
    }

    private ComponentDependency extractComponentDependency( PlexusConfiguration d )
    {
        ComponentDependency cd = new ComponentDependency();

        cd.setArtifactId( extractArtifactId( d ) );

        cd.setGroupId( extractGroupId( d ) );

        cd.setType( d.getChild( "type" ).getValue() );

        cd.setVersion( extractVersion( d ) );
        return cd;
    }

    @SuppressWarnings( "checkstyle:methodlength" )
    public MojoDescriptor buildComponentDescriptor( PlexusConfiguration c, PluginDescriptor pluginDescriptor )
        throws PlexusConfigurationException
    {
        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setPluginDescriptor( pluginDescriptor );

        mojo.setGoal( c.getChild( "goal" ).getValue() );

        mojo.setImplementation( c.getChild( "implementation" ).getValue() );

        PlexusConfiguration langConfig = c.getChild( "language" );

        if ( langConfig != null )
        {
            mojo.setLanguage( langConfig.getValue() );
        }

        PlexusConfiguration configuratorConfig = c.getChild( "configurator" );

        if ( configuratorConfig != null )
        {
            mojo.setComponentConfigurator( configuratorConfig.getValue() );
        }

        PlexusConfiguration composerConfig = c.getChild( "composer" );

        if ( composerConfig != null )
        {
            mojo.setComponentComposer( composerConfig.getValue() );
        }

        String since = c.getChild( "since" ).getValue();

        if ( since != null )
        {
            mojo.setSince( since );
        }

        PlexusConfiguration deprecated = c.getChild( "deprecated", false );

        if ( deprecated != null )
        {
            mojo.setDeprecated( deprecated.getValue() );
        }

        String phase = c.getChild( "phase" ).getValue();

        if ( phase != null )
        {
            mojo.setPhase( phase );
        }

        String executePhase = c.getChild( "executePhase" ).getValue();

        if ( executePhase != null )
        {
            mojo.setExecutePhase( executePhase );
        }

        String executeMojo = c.getChild( "executeGoal" ).getValue();

        if ( executeMojo != null )
        {
            mojo.setExecuteGoal( executeMojo );
        }

        String executeLifecycle = c.getChild( "executeLifecycle" ).getValue();

        if ( executeLifecycle != null )
        {
            mojo.setExecuteLifecycle( executeLifecycle );
        }

        mojo.setInstantiationStrategy( c.getChild( "instantiationStrategy" ).getValue() );

        mojo.setDescription( extractDescription( c ) );

        PlexusConfiguration dependencyResolution = c.getChild( "requiresDependencyResolution", false );

        if ( dependencyResolution != null )
        {
            mojo.setDependencyResolutionRequired( dependencyResolution.getValue() );
        }

        PlexusConfiguration dependencyCollection = c.getChild( "requiresDependencyCollection", false );

        if ( dependencyCollection != null )
        {
            mojo.setDependencyCollectionRequired( dependencyCollection.getValue() );
        }

        String directInvocationOnly = c.getChild( "requiresDirectInvocation" ).getValue();

        if ( directInvocationOnly != null )
        {
            mojo.setDirectInvocationOnly( Boolean.parseBoolean( directInvocationOnly ) );
        }

        String requiresProject = c.getChild( "requiresProject" ).getValue();

        if ( requiresProject != null )
        {
            mojo.setProjectRequired( Boolean.parseBoolean( requiresProject ) );
        }

        String requiresReports = c.getChild( "requiresReports" ).getValue();

        if ( requiresReports != null )
        {
            mojo.setRequiresReports( Boolean.parseBoolean( requiresReports ) );
        }

        String aggregator = c.getChild( "aggregator" ).getValue();

        if ( aggregator != null )
        {
            mojo.setAggregator( Boolean.parseBoolean( aggregator ) );
        }

        String requiresOnline = c.getChild( "requiresOnline" ).getValue();

        if ( requiresOnline != null )
        {
            mojo.setOnlineRequired( Boolean.parseBoolean( requiresOnline ) );
        }

        String inheritedByDefault = c.getChild( "inheritedByDefault" ).getValue();

        if ( inheritedByDefault != null )
        {
            mojo.setInheritedByDefault( Boolean.parseBoolean( inheritedByDefault ) );
        }

        String threadSafe = c.getChild( "threadSafe" ).getValue();

        if ( threadSafe != null )
        {
            mojo.setThreadSafe( Boolean.parseBoolean( threadSafe ) );
        }

        // ----------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------

        PlexusConfiguration mojoConfig = c.getChild( "configuration" );
        mojo.setMojoConfiguration( mojoConfig );

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        PlexusConfiguration[] parameterConfigurations = c.getChild( "parameters" ).getChildren( "parameter" );

        List<Parameter> parameters = new ArrayList<>();

        for ( PlexusConfiguration d : parameterConfigurations )
        {
            Parameter parameter = new Parameter();

            parameter.setName( extractName( d ) );

            parameter.setAlias( d.getChild( "alias" ).getValue() );

            parameter.setType( d.getChild( "type" ).getValue() );

            String required = d.getChild( "required" ).getValue();

            parameter.setRequired( Boolean.parseBoolean( required ) );

            PlexusConfiguration editableConfig = d.getChild( "editable" );

            // we need the null check for pre-build legacy plugins...
            if ( editableConfig != null )
            {
                String editable = d.getChild( "editable" ).getValue();

                parameter.setEditable( editable == null || Boolean.parseBoolean( editable ) );
            }

            parameter.setDescription( extractDescription( d ) );

            parameter.setDeprecated( d.getChild( "deprecated" ).getValue() );

            parameter.setImplementation( d.getChild( "implementation" ).getValue() );

            parameter.setSince( d.getChild( "since" ).getValue() );

            PlexusConfiguration paramConfig = mojoConfig.getChild( parameter.getName(), false );
            if ( paramConfig != null )
            {
                parameter.setExpression( paramConfig.getValue( null ) );
                parameter.setDefaultValue( paramConfig.getAttribute( "default-value" ) );
            }

            parameters.add( parameter );
        }

        mojo.setParameters( parameters );

        // TODO this should not need to be handed off...

        // ----------------------------------------------------------------------
        // Requirements
        // ----------------------------------------------------------------------

        PlexusConfiguration[] requirements = c.getChild( "requirements" ).getChildren( "requirement" );

        for ( PlexusConfiguration requirement : requirements )
        {
            ComponentRequirement cr = new ComponentRequirement();

            cr.setRole( requirement.getChild( "role" ).getValue() );

            cr.setRoleHint( requirement.getChild( "role-hint" ).getValue() );

            cr.setFieldName( requirement.getChild( "field-name" ).getValue() );

            mojo.addRequirement( cr );
        }

        return mojo;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public PlexusConfiguration buildConfiguration( Reader configuration )
        throws PlexusConfigurationException
    {
        try
        {
            return new XmlPlexusConfiguration( Xpp3DomBuilder.build( configuration ) );
        }
        catch ( IOException | XmlPullParserException e )
        {
            throw new PlexusConfigurationException( e.getMessage(), e );
        }
    }
}
