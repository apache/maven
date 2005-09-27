package org.apache.maven.plugin;

import java.util.Map;

public interface ContextEnabled
{
    
    String PLUGIN_CONTEXT_SESSION_KEY = "mavenPluginContext";
    
    void setPluginContext( Map pluginContext );
    
    Map getPluginContext();

}
