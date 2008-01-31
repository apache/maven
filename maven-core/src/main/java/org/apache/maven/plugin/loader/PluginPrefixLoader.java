package org.apache.maven.plugin.loader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public interface PluginPrefixLoader
{

    /**
     * Determine the appropriate {@link Plugin} instance for use with the specified plugin
     * prefix, using the following strategies (in order):
     * <br/>
     * <ol>
     *   <li>Search for a plugin that has already been loaded with the specified prefix</li>
     *   <li>Search for a plugin configured in the POM that has a matching prefix</li>
     *   <li>Search the pluginGroups specified in the settings.xml for a matching plugin</li>
     *   <li>Use groupId == org.apache.maven.plugins, and artifactId == maven-&lt;prefix&gt;-plugin,
     *         and try to resolve based on that.</li>
     * </ol>
     */
    Plugin findPluginForPrefix( String prefix, MavenProject project, MavenSession session )
        throws PluginLoaderException;

}
