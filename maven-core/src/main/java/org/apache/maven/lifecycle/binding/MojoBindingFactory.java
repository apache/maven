package org.apache.maven.lifecycle.binding;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.project.MavenProject;

/**
 * Responsible for constructing or parsing MojoBinding instances from one of several sources, potentially
 * using the {@link PluginLoader} to resolve any plugin prefixes first.
 *
 * @author jdcasey
 *
 */
public interface MojoBindingFactory
{

    String ROLE = MojoBindingFactory.class.getName();

    /**
     * Parse the specified mojo string into a MojoBinding, optionally allowing plugin-prefix references.
     * If a plugin-prefix is allowed and used, resolve the prefix and use the resulting PluginDescriptor
     * to set groupId and artifactId on the MojoBinding instance.
     */
    MojoBinding parseMojoBinding( String bindingSpec,
                                  MavenProject project,
                                  MavenSession session,
                                  boolean allowPrefixReference )
        throws LifecycleSpecificationException, LifecycleLoaderException;

    /**
     * Create a new MojoBinding instance with the specified information, and inject POM configurations
     * appropriate to that mojo before returning it.
     */
    MojoBinding createMojoBinding( String groupId,
                                   String artifactId,
                                   String version,
                                   String goal,
                                   MavenProject project );

    /**
     * Simplified version of {@link MojoBindingFactory#parseMojoBinding(String, MavenProject, MavenSession, boolean)}
     * which assumes the project is null and prefixes are not allowed. This method will <b>never</b>
     * result in the {@link PluginLoader} being used to resolve the PluginDescriptor.
     */
    MojoBinding parseMojoBinding( String bindingSpec )
        throws LifecycleSpecificationException;

    /**
     * Simplified version of {@link MojoBindingFactory#parseMojoBinding(String, MavenProject, MavenSession, boolean)}
     * which assumes that prefixes are not allowed. This method will <b>never</b>
     * result in the {@link PluginLoader} being used to resolve the PluginDescriptor.
     * @throws LifecycleSpecificationException
     */
    MojoBinding parseMojoBinding( String bindingSpec,
                                  MavenProject project )
        throws LifecycleSpecificationException;

}
