package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.plan.LifecyclePlannerException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;

/**
 * Responsible for the gross construction of LifecycleBindings, or mappings of MojoBinding instances
 * to different parts of the three lifecycles: clean, build, and site. Also, handles transcribing
 * these LifecycleBindings instances into lists of MojoBinding's, which can be consumed by the
 * LifecycleExecutor.
 * 
 * @author jdcasey
 *
 */
public interface LifecycleBindingManager
{

    String ROLE = LifecycleBindingManager.class.getName();

    /**
     * Construct the LifecycleBindings for the default lifecycle mappings, including injection of 
     * configuration from the project into each MojoBinding, where appropriate.
     */
    LifecycleBindings getDefaultBindings( MavenProject project )
        throws LifecycleSpecificationException;

    /**
     * Retrieve the LifecycleBindings given by the lifecycle mapping component/file for the project's
     * packaging. Any applicable mojo configuration will be injected into the LifecycleBindings from
     * the POM.
     */
    LifecycleBindings getBindingsForPackaging( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Construct the LifecycleBindings that constitute the extra mojos bound to the lifecycle within
     * the POM itself.
     */
    LifecycleBindings getProjectCustomBindings( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Construct the LifecycleBindings that constitute the mojos mapped to the lifecycles by an overlay
     * specified in a plugin. Inject mojo configuration from the POM into all appropriate MojoBinding
     * instances.
     */
    LifecycleBindings getPluginLifecycleOverlay( PluginDescriptor pluginDescriptor, String lifecycleId, MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Retrieve the list of MojoBinding instances that correspond  to the reports configured for the
     * specified project. Inject all appropriate configuration from the POM for each MojoBinding, using
     * the following precedence rules:
     * <br/>
     * <ol>
     *   <li>report-set-level configuration</li>
     *   <li>reporting-level configuration</li>
     *   <li>execution-level configuration</li>
     *   <li>plugin-level configuration</li>
     * </ol>
     */
    List getReportBindings( MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException;

    /**
     * Traverse the specified LifecycleBindings instance for all of the specified tasks. If the task
     * is found to be a phase name, construct the list of all MojoBindings that lead up to that phase
     * in that lifecycle, and add them to the master MojoBinding list. If the task is not a phase name,
     * treat it as a direct mojo invocation, parse it into a MojoBinding (resolving the plugin prefix
     * first if necessary), and add it to the master MojoBinding list.
     * 
     * Then, iterate through all MojoBindings in the master list, and for each one that maps to an 
     * entry in directInvocationModifiers, substitute the resultant MojoBinding list from that 
     * modifier in place of the original MojoBinding.
     * 
     * Finally, return the modified master list.
     */
    List assembleMojoBindingList( List tasks, LifecycleBindings bindings, Map directInvocationModifiers, MavenProject project )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException;

    /**
     * Traverse the specified LifecycleBindings instance for all of the specified tasks. If the task
     * is found to be a phase name, construct the list of all MojoBindings that lead up to that phase
     * in that lifecycle, and add them to the master MojoBinding list. If the task is not a phase name,
     * treat it as a direct mojo invocation, parse it into a MojoBinding (resolving the plugin prefix
     * first if necessary), and add it to the master MojoBinding list. Finally, return the master list.
     */
    List assembleMojoBindingList( List tasks, LifecycleBindings lifecycleBindings, MavenProject project )
        throws LifecycleSpecificationException, LifecyclePlannerException, LifecycleLoaderException;

}
