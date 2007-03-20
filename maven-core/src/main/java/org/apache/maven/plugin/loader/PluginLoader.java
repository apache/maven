package org.apache.maven.plugin.loader;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Responsible for loading plugins, reports, and any components contained therein. Will resolve
 * plugin versions and plugin prefixes as necessary for plugin resolution.
 * 
 * @author jdcasey
 *
 */
public interface PluginLoader
{

    /**
     * Lookup a component with the specified role + roleHint in the plugin's {@link ClassRealm}.
     * Load the plugin first.
     */
    Object loadPluginComponent( String role, String roleHint, Plugin plugin, MavenProject project )
        throws ComponentLookupException, PluginLoaderException;

    /**
     * Load the {@link PluginDescriptor} instance for the specified plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental plugin information as necessary.
     */
    PluginDescriptor loadPlugin( Plugin plugin, MavenProject project )
        throws PluginLoaderException;

    /**
     * Load the {@link PluginDescriptor} instance for the plugin implied by the specified MojoBinding, 
     * using the project for {@link ArtifactRepository} and other supplemental plugin information as 
     * necessary.
     */
    PluginDescriptor loadPlugin( MojoBinding mojoBinding, MavenProject project )
        throws PluginLoaderException;

    /**
     * Load the {@link PluginDescriptor} instance for the specified report plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental report/plugin information as necessary.
     */
    PluginDescriptor loadReportPlugin( ReportPlugin reportPlugin, MavenProject project )
        throws PluginLoaderException;

    /**
     * Load the {@link PluginDescriptor} instance for the report plugin implied by the specified MojoBinding, 
     * using the project for {@link ArtifactRepository} and other supplemental report/plugin information as 
     * necessary.
     */
    PluginDescriptor loadReportPlugin( MojoBinding mojoBinding, MavenProject project )
        throws PluginLoaderException;

    /**
     * Determine the appropriate {@link PluginDescriptor} instance for use with the specified plugin
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
    PluginDescriptor findPluginForPrefix( String prefix, MavenProject project )
        throws PluginLoaderException;
    
}
