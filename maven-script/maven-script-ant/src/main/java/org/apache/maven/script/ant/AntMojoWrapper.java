package org.apache.maven.script.ant;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.MapOrientedComponent;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.factory.ant.AntComponentExecutionException;
import org.codehaus.plexus.component.factory.ant.AntScriptInvoker;
import org.codehaus.plexus.component.repository.ComponentRequirement;

import java.util.Map;


public class AntMojoWrapper
    extends AbstractMojo
    implements ContextEnabled, MapOrientedComponent
{

    private Map pluginContext;
    private final AntScriptInvoker scriptInvoker;

    public AntMojoWrapper( AntScriptInvoker scriptInvoker )
    {
        this.scriptInvoker = scriptInvoker;
    }

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            scriptInvoker.invoke();
        }
        catch ( AntComponentExecutionException e )
        {
            throw new MojoExecutionException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    public void setPluginContext( Map pluginContext )
    {
        this.pluginContext = pluginContext;
    }

    public Map getPluginContext()
    {
        return pluginContext;
    }

    public void addComponentRequirement( ComponentRequirement requirementDescriptor, Object requirementValue )
        throws ComponentConfigurationException
    {
        scriptInvoker.addComponentRequirement( requirementDescriptor, requirementValue );
    }

    public void setComponentConfiguration( Map componentConfiguration )
        throws ComponentConfigurationException
    {
        scriptInvoker.setComponentConfiguration( componentConfiguration );
    }

}
