package org.apache.maven.plugin.loader;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

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
     * Load the {@link PluginDescriptor} instance for the specified plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental plugin information as necessary.
     */
    PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginLoaderException;

    /**
     * Load the {@link PluginDescriptor} instance for the plugin implied by the specified MojoBinding,
     * using the project for {@link ArtifactRepository} and other supplemental plugin information as
     * necessary.
     */
    PluginDescriptor loadPlugin( MojoBinding mojoBinding, MavenProject project, MavenSession session )
        throws PluginLoaderException;

    /**
     * Load the {@link PluginDescriptor} instance for the specified report plugin, using the project for
     * the {@link ArtifactRepository} and other supplemental report/plugin information as necessary.
     */
    PluginDescriptor loadReportPlugin( ReportPlugin reportPlugin, MavenProject project, MavenSession session )
        throws PluginLoaderException;

    /**
     * Load the {@link PluginDescriptor} instance for the report plugin implied by the specified MojoBinding,
     * using the project for {@link ArtifactRepository} and other supplemental report/plugin information as
     * necessary.
     */
    PluginDescriptor loadReportPlugin( MojoBinding mojoBinding, MavenProject project, MavenSession session )
        throws PluginLoaderException;

}
