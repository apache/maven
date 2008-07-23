package org.apache.maven.lifecycle.binding;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Set;

/**
 * Responsible for the gross construction of LifecycleBindings, or mappings of MojoBinding instances to different parts
 * of the three lifecycles: clean, build, and site. Also, handles transcribing these LifecycleBindings instances into
 * lists of MojoBinding's, which can be consumed by the LifecycleExecutor.
 *
 * @author jdcasey
 *
 */
public interface LifecycleBindingManager
{

    String ROLE = LifecycleBindingManager.class.getName();

    /**
     * Construct the LifecycleBindings for the default lifecycle mappings, including injection of configuration from the
     * project into each MojoBinding, where appropriate.
     */
    LifecycleBindings getDefaultBindings( MavenProject project ) throws LifecycleSpecificationException;

    /**
     * Retrieve the LifecycleBindings given by the lifecycle mapping component/file for the project's packaging. Any
     * applicable mojo configuration will be injected into the LifecycleBindings from the POM.
     * @param session
     */
    LifecycleBindings getBindingsForPackaging( MavenProject project, MavenSession session )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Construct the LifecycleBindings that constitute the extra mojos bound to the lifecycle within the POM itself.
     */
    LifecycleBindings getProjectCustomBindings( MavenProject project, MavenSession session, Set unresolvableBindings )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Construct the LifecycleBindings that constitute the mojos mapped to the lifecycles by an overlay specified in a
     * plugin. Inject mojo configuration from the POM into all appropriate MojoBinding instances.
     */
    LifecycleBindings getPluginLifecycleOverlay( PluginDescriptor pluginDescriptor, String lifecycleId,
                                                 MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Retrieve the list of MojoBinding instances that correspond to the reports configured for the specified project.
     * Inject all appropriate configuration from the POM for each MojoBinding, using the following precedence rules:
     * <br/>
     * <ol>
     * <li>report-set-level configuration</li>
     * <li>reporting-level configuration</li>
     * <li>execution-level configuration</li>
     * <li>plugin-level configuration</li>
     * </ol>
     */
    List getReportBindings( MavenProject project, MavenSession session ) throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Go through the set of unbindable mojos provided (these are {@link MojoBinding} instancess coming
     * from the project as custom bindings for which we failed to determine a phase
     * to bind them during {@link LifecycleBindingManager#getProjectCustomBindings(MavenProject, MavenSession, Set)}).
     * For each {@link MojoBinding}, attempt to resolve it again, and if successful,
     * extract the default phase name from the {@link MojoDescriptor}.
     *
     * @throws LifecycleSpecificationException In case the plugin cannot be resolved, the plugin doesn't contain the specified mojo,
     *   or the mojo doesn't have a default phase.
     */
    void resolveUnbindableMojos( final Set unbindableMojos,
                                        final MavenProject project,
                                        final MavenSession session,
                                        final LifecycleBindings lifecycleBindings )
        throws LifecycleSpecificationException;

    List<org.apache.maven.lifecycle.binding.Lifecycle> getLifecycles();

}
