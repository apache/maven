package org.apache.maven.plugin.descriptor;

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
        PlexusConfiguration c = buildConfiguration( reader );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        String id = c.getChild( "id" ).getValue();
        if ( id != null )
        {
            // TODO: remove. This is old style mojos (alpha-1)
            pluginDescriptor.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
            pluginDescriptor.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( id ) );
        }
        else
        {
            pluginDescriptor.setGroupId( c.getChild( "groupId" ).getValue() );
            pluginDescriptor.setArtifactId( c.getChild( "artifactId" ).getValue() );
        }

        // ----------------------------------------------------------------------
        // Components
        // ----------------------------------------------------------------------

        PlexusConfiguration[] mojoConfigurations = c.getChild( "mojos" ).getChildren( "mojo" );

        List mojos = new ArrayList();

        for ( int i = 0; i < mojoConfigurations.length; i++ )
        {
            PlexusConfiguration component = mojoConfigurations[i];

            mojos.add( buildComponentDescriptor( component ) );
        }

        pluginDescriptor.setMojos( mojos );

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        PlexusConfiguration[] dependencyConfigurations = c.getChild( "dependencies" ).getChildren( "dependency" );

        List dependencies = new ArrayList();

        for ( int i = 0; i < dependencyConfigurations.length; i++ )
        {
            PlexusConfiguration d = dependencyConfigurations[i];

            Dependency cd = new Dependency();

            cd.setArtifactId( d.getChild( "artifactId" ).getValue() );

            cd.setGroupId( d.getChild( "groupId" ).getValue() );

            cd.setType( d.getChild( "type" ).getValue() );

            cd.setVersion( d.getChild( "version" ).getValue() );

            dependencies.add( cd );
        }

        pluginDescriptor.setDependencies( dependencies );

        return pluginDescriptor;
    }

    public MojoDescriptor buildComponentDescriptor( PlexusConfiguration c )
        throws PlexusConfigurationException
    {
        MojoDescriptor mojo = new MojoDescriptor();

        mojo.setId( c.getChild( "id" ).getValue() );

        mojo.setGoal( c.getChild( "goal" ).getValue() );

        mojo.setImplementation( c.getChild( "implementation" ).getValue() );

        PlexusConfiguration langConfig = c.getChild( "language" );

        if ( langConfig != null )
        {
            mojo.setLanguage( langConfig.getValue() );
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
            mojo.setRequiresDependencyResolution( dependencyResolution );
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

            parameter.setRequired( "true".equals( required ) );
            
            PlexusConfiguration editableConfig = d.getChild("editable");
            
            // we need the null check for pre-build legacy plugins...
            if(editableConfig != null)
            {
                String editable = d.getChild("editable").getValue();
                
                parameter.setEditable( editable == null || "true".equals( editable ) );
            }
            
            parameter.setValidator( d.getChild( "validator" ).getValue() );

            parameter.setDescription( d.getChild( "description" ).getValue() );

            // TODO: remove
            parameter.setExpression( d.getChild( "expression" ).getValue() );

            // TODO: remove
            parameter.setDefaultValue( d.getChild( "default" ).getValue() );

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
