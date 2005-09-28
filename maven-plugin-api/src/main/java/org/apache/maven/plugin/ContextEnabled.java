package org.apache.maven.plugin;

import java.util.Map;

public interface ContextEnabled
{
    
    void setPluginContext( Map pluginContext );
    
    Map getPluginContext();

}
