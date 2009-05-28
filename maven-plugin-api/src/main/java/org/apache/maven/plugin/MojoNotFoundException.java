package org.apache.maven.plugin;

import org.apache.maven.plugin.descriptor.PluginDescriptor;

public class MojoNotFoundException
    extends Exception
{
    private String goal;

    private PluginDescriptor pluginDescriptor;
        
    public MojoNotFoundException( String goal, PluginDescriptor pluginDescriptor )
    {
        this.goal = goal;
        this.pluginDescriptor = pluginDescriptor;        
    }

    public String getGoal()
    {
        return goal;
    }

    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }        
}
