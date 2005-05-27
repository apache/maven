package org.apache.maven.plugin.descriptor;

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
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
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

        mojo.setInstantiationStrategy( c.getChild( "instantiationStrategy" ).getValue() );

        mojo.setDescription( c.getChild( "description" ).getValue() );

        String dependencyResolution = c.getChild( "requiresDependencyResolution" ).getValue();

        if ( dependencyResolution != null )
        {
            mojo.setDependencyResolutionRequired( dependencyResolution );
        }

        String requiresProject = c.getChild( "requiresProject" ).getValue();

        if ( requiresProject != null )
        {
            mojo.setProjectRequired( Boolean.valueOf( requiresProject ).booleanValue() );
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

            parameter.setExpression( d.getChild( "expression" ).getValue() );

            parameter.setDeprecated( d.getChild( "deprecated" ).getValue() );

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
