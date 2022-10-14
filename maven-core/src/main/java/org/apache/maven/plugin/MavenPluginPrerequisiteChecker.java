package org.apache.maven.plugin;

import org.apache.maven.plugin.descriptor.PluginDescriptor;

/**
 * Service responsible for checking if plugin's prerequisites are met.
 */
@FunctionalInterface
public interface MavenPluginPrerequisiteChecker
{
    void accept( PluginDescriptor pluginDescriptor ) throws PluginIncompatibleException;
}
