package org.apache.maven.plugin;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.configuration.PlexusConfigurationException;


public interface PluginRepository {

    Plugin findPluginById(String pluginId, String mojoId) throws Exception;
}
