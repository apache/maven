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

import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jason van Zyl
 * @version $Id$
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
        PlexusConfiguration c = buildConfiguration( reader );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setSource( source );
        pluginDescriptor.setGroupId( c.getChild( "groupId" ).getValue() );
        pluginDescriptor.setArtifactId( c.getChild( "artifactId" ).getValue() );
        pluginDescriptor.setVersion( c.getChild( "version" ).getValue() );
        pluginDescriptor.setGoalPrefix( c.getChild( "goalPrefix" ).getValue() );
        
        pluginDescriptor.setName( c.getChild( "name" ).getValue() );
        pluginDescriptor.setDescription( c.getChild( "description" ).getValue() );

        String isolatedRealm = c.getChild( "isolatedRealm" ).getValue();

        if ( isolatedRealm != null )
        {
            pluginDescriptor.setIsolatedRealm( Boolean.valueOf( isolatedRealm ).booleanValue() );
        }

        String inheritedByDefault = c.getChild( "inheritedByDefault" ).getValue();

        if ( inheritedByDefault != null )
        {
            pluginDescriptor.setInheritedByDefault( Boolean.valueOf( inheritedByDefault ).booleanValue() );
        }

        // ----------------------------------------------------------------------
        // Components
        // ----------------------------------------------------------------------

        PlexusConfiguration[] mojoConfigurations = c.getChild( "mojos" ).getChildren( "mojo" );

        for ( int i = 0; i < mojoConfigurations.length; i++ )
        {
            PlexusConfiguration component = mojoConfigurations[i];

            MojoDescriptor mojoDescriptor = buildComponentDescriptor( component, pluginDescriptor );

            pluginDescriptor.addMojo( mojoDescriptor );
        }

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        PlexusConfiguration[] dependencyConfigurations = c.getChild( "dependencies" ).getChildren( "dependency" );

        List dependencies = new ArrayList();

        for ( int i = 0; i < dependencyConfigurations.length; i++ )
        {
            PlexusConfiguration d = dependencyConfigurations[i];

            ComponentDependency cd = new ComponentDependency();

            cd.setArtifactId( d.getChild( "artifactId" ).getValue() );

            cd.setGroupId( d.getChild( "groupId" ).getValue() );

            cd.setType( d.getChild( "type" ).getValue() );

            cd.setVersion( d.getChild( "version" ).getValue() );

            dependencies.add( cd );
        }

        pluginDescriptor.setDependencies( dependencies );

        return pluginDescriptor;
    }

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

        mojo.setDescription( c.getChild( "description" ).getValue() );

        String dependencyResolution = c.getChild( "requiresDependencyResolution" ).getValue();

        if ( dependencyResolution != null )
        {
            mojo.setDependencyResolutionRequired( dependencyResolution );
        }

        String directInvocationOnly = c.getChild( "requiresDirectInvocation" ).getValue();

        if ( directInvocationOnly != null )
        {
            mojo.setDirectInvocationOnly( Boolean.valueOf( directInvocationOnly ).booleanValue() );
        }

        String requiresProject = c.getChild( "requiresProject" ).getValue();

        if ( requiresProject != null )
        {
            mojo.setProjectRequired( Boolean.valueOf( requiresProject ).booleanValue() );
        }

        String requiresReports = c.getChild( "requiresReports" ).getValue();

        if ( requiresReports != null )
        {
            mojo.setRequiresReports( Boolean.valueOf( requiresReports ).booleanValue() );
        }

        String aggregator = c.getChild( "aggregator" ).getValue();

        if ( aggregator != null )
        {
            mojo.setAggregator( Boolean.valueOf( aggregator ).booleanValue() );
        }

        String requiresOnline = c.getChild( "requiresOnline" ).getValue();

        if ( requiresOnline != null )
        {
            mojo.setOnlineRequired( Boolean.valueOf( requiresOnline ).booleanValue() );
        }

        String inheritedByDefault = c.getChild( "inheritedByDefault" ).getValue();

        if ( inheritedByDefault != null )
        {
            mojo.setInheritedByDefault( Boolean.valueOf( inheritedByDefault ).booleanValue() );
        }

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        PlexusConfiguration[] parameterConfigurations = c.getChild( "parameters" ).getChildren( "parameter" );

        List parameters = new ArrayList();

        for ( int i = 0; i < parameterConfigurations.length; i++ )
        {
            PlexusConfiguration d = parameterConfigurations[i];

            Parameter parameter = new Parameter();

            parameter.setName( d.getChild( "name" ).getValue() );

            parameter.setAlias( d.getChild( "alias" ).getValue() );

            parameter.setType( d.getChild( "type" ).getValue() );

            String required = d.getChild( "required" ).getValue();

            parameter.setRequired( Boolean.valueOf( required ).booleanValue() );

            PlexusConfiguration editableConfig = d.getChild( "editable" );

            // we need the null check for pre-build legacy plugins...
            if ( editableConfig != null )
            {
                String editable = d.getChild( "editable" ).getValue();

                parameter.setEditable( editable == null || Boolean.valueOf( editable ).booleanValue() );
            }

            parameter.setDescription( d.getChild( "description" ).getValue() );

            parameter.setDeprecated( d.getChild( "deprecated" ).getValue() );

            parameter.setImplementation( d.getChild( "implementation" ).getValue() );

            parameters.add( parameter );
        }

        mojo.setParameters( parameters );

        // TODO: this should not need to be handed off...

        // ----------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------

        mojo.setMojoConfiguration( c.getChild( "configuration" ) );

        // TODO: Go back to this when we get the container ready to configure mojos...
        //        mojo.setConfiguration( c.getChild( "configuration" ) );

        // ----------------------------------------------------------------------
        // Requirements
        // ----------------------------------------------------------------------

        PlexusConfiguration[] requirements = c.getChild( "requirements" ).getChildren( "requirement" );

        for ( int i = 0; i < requirements.length; i++ )
        {
            PlexusConfiguration requirement = requirements[i];

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
        catch ( IOException e )
        {
            throw new PlexusConfigurationException( "Error creating configuration", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new PlexusConfigurationException( "Error creating configuration", e );
        }
    }
}
