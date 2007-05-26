package org.apache.maven.lifecycle.plan;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.binding.MojoBindingFactory;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Responsible for creating a plan of execution for a given project and list of tasks. This build plan consists of
 * MojoBinding instances that carry all the information necessary to execute a mojo, including configuration from the
 * POM and other sources. NOTE: the build plan may be constructed of a main lifecycle binding-set, plus any number of
 * lifecycle modifiers and direct-invocation modifiers, to handle cases of forked execution.
 * 
 * @author jdcasey
 * 
 */
public class DefaultBuildPlanner
    implements BuildPlanner, LogEnabled
{

    private Logger logger;

    private PluginLoader pluginLoader;

    private LifecycleBindingManager lifecycleBindingManager;

    private MojoBindingFactory mojoBindingFactory;

    /**
     * Orchestrates construction of the build plan which will be used by the user of LifecycleExecutor.
     */
    public BuildPlan constructBuildPlan( final List tasks, final MavenProject project )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException
    {
        LifecycleBindings defaultBindings = lifecycleBindingManager.getDefaultBindings( project );
        LifecycleBindings packagingBindings = lifecycleBindingManager.getBindingsForPackaging( project );
        LifecycleBindings projectBindings = lifecycleBindingManager.getProjectCustomBindings( project );

        BuildPlan plan = new BuildPlan( packagingBindings, projectBindings, defaultBindings, tasks );

        initializeDirectInvocations( plan, project );

        // Inject forked lifecycles as plan modifiers for each mojo that has @execute in it.
        addForkedLifecycleModifiers( plan, project );
        addReportingLifecycleModifiers( plan, project );

        // TODO: Inject relative-ordered project/plugin executions as plan modifiers.

        return plan;
    }

    private void initializeDirectInvocations( final BuildPlan plan, final MavenProject project )
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        List tasks = plan.getTasks();
        for ( Iterator it = tasks.iterator(); it.hasNext(); )
        {
            String task = (String) it.next();

            if ( !LifecycleUtils.isValidPhaseName( task ) )
            {
                MojoBinding binding = mojoBindingFactory.parseMojoBinding( task, project, true );
                plan.addDirectInvocationBinding( task, binding );
            }
        }
    }

    public void enableLogging( final Logger logger )
    {
        this.logger = logger;
    }

    /**
     * Traverses all MojoBinding instances discovered from the POM and its packaging-mappings, and orchestrates the
     * process of injecting any modifiers that are necessary to accommodate forked execution.
     */
    private void addForkedLifecycleModifiers( final BuildPlan plan, final MavenProject project )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        List planBindings = plan.renderExecutionPlan( new Stack() );
        plan.resetExecutionProgress();

        for ( Iterator it = planBindings.iterator(); it.hasNext(); )
        {
            MojoBinding mojoBinding = (MojoBinding) it.next();

            findForkModifiers( mojoBinding, plan, project );
        }
    }

    private void findForkModifiers( final MojoBinding mojoBinding, final BuildPlan plan, final MavenProject project )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        PluginDescriptor pluginDescriptor = null;
        try
        {
            pluginDescriptor = pluginLoader.loadPlugin( mojoBinding, project );
        }
        catch ( PluginLoaderException e )
        {
            String message =
                "Failed to load plugin: " + MojoBindingUtils.createPluginKey( mojoBinding )
                                + ". Adding to late-bound plugins list.\nReason: " + e.getMessage();

            if ( logger.isDebugEnabled() )
            {
                logger.debug( message, e );
            }
            else
            {
                logger.warn( message );
            }

            plan.addLateBoundMojo( mojoBinding );
        }

        if ( pluginDescriptor == null )
        {
            return;
        }

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoBinding.getGoal() );
        if ( mojoDescriptor == null )
        {
            throw new LifecyclePlannerException( "Mojo: " + mojoBinding.getGoal() + " does not exist in plugin: "
                                                 + pluginDescriptor.getId() + "." );
        }

        findForkModifiers( mojoBinding, pluginDescriptor, plan, project );
    }

    /**
     * Traverses all MojoBinding instances discovered from the POM and its packaging-mappings, and orchestrates the
     * process of injecting any modifiers that are necessary to accommodate mojos that require access to the project's
     * configured reports.
     */
    private void addReportingLifecycleModifiers( final BuildPlan plan, final MavenProject project )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        List planBindings = plan.renderExecutionPlan( new Stack() );
        plan.resetExecutionProgress();

        for ( Iterator it = planBindings.iterator(); it.hasNext(); )
        {
            MojoBinding mojoBinding = (MojoBinding) it.next();

            PluginDescriptor pluginDescriptor = null;
            try
            {
                pluginDescriptor = pluginLoader.loadPlugin( mojoBinding, project );
            }
            catch ( PluginLoaderException e )
            {
                String message =
                    "Failed to load plugin: " + MojoBindingUtils.createPluginKey( mojoBinding )
                                    + ". Adding to late-bound plugins list.\nReason: " + e.getMessage();

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( message, e );
                }
                else
                {
                    logger.warn( message );
                }

                plan.addLateBoundMojo( mojoBinding );
            }

            if ( pluginDescriptor == null )
            {
                continue;
            }

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoBinding.getGoal() );
            if ( mojoDescriptor == null )
            {
                throw new LifecyclePlannerException( "Mojo: " + mojoBinding.getGoal() + " does not exist in plugin: "
                                                     + pluginDescriptor.getId() + "." );
            }

            if ( mojoDescriptor.isRequiresReports() )
            {
                List reportBindings = lifecycleBindingManager.getReportBindings( project );

                for ( Iterator reportBindingIt = reportBindings.iterator(); reportBindingIt.hasNext(); )
                {
                    MojoBinding reportBinding = (MojoBinding) reportBindingIt.next();

                    try
                    {
                        pluginLoader.loadReportPlugin( mojoBinding, project );
                    }
                    catch ( PluginLoaderException e )
                    {
                        throw new LifecyclePlannerException( "Failed to load report-plugin descriptor for: "
                                                             + MojoBindingUtils.toString( reportBinding )
                                                             + ". Reason: " + e.getMessage(), e );
                    }
                }

                plan.addForkedExecution( mojoBinding, reportBindings );

                // NOTE: the first sighting of a mojo requiring reports should satisfy this condition.
                // therefore, we can break out as soon as we find one.
                break;
            }
        }
    }

    /**
     * Explores a single MojoBinding, and injects any necessary plan modifiers to accommodate any of the three types of
     * forked execution, along with any new mojos/lifecycles that entails.
     */
    private void findForkModifiers( final MojoBinding mojoBinding, final PluginDescriptor pluginDescriptor,
                                    final BuildPlan plan, final MavenProject project )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        if ( mojoDescriptor.getExecuteGoal() != null )
        {
            recurseSingleMojoFork( mojoBinding, pluginDescriptor, plan, project );
        }
        else if ( mojoDescriptor.getExecutePhase() != null )
        {
            recursePhaseMojoFork( mojoBinding, pluginDescriptor, plan, project );
        }
    }

    /**
     * Constructs the lifecycle bindings used to execute a particular fork, given the forking mojo binding. If the mojo
     * binding specifies a lifecycle overlay, this method will add that into the forked lifecycle, and calculate the
     * bindings to inject based on the phase in that new lifecycle which should be executed.
     * 
     * Hands off to the
     * {@link DefaultBuildPlanner#modifyBuildPlanForForkedLifecycle(MojoBinding, PluginDescriptor, ModifiablePlanElement, LifecycleBindings, MavenProject, LinkedList, List)}
     * method to handle the actual plan modification.
     */
    private void recursePhaseMojoFork( final MojoBinding mojoBinding, final PluginDescriptor pluginDescriptor,
                                       final BuildPlan plan, final MavenProject project )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        String phase = mojoDescriptor.getExecutePhase();

        if ( phase == null )
        {
            return;
        }

        if ( !LifecycleUtils.isValidPhaseName( phase ) )
        {
            throw new LifecyclePlannerException( "Cannot find lifecycle for phase: " + phase );
        }

        BuildPlan clonedPlan = plan.copy( Collections.singletonList( phase ) );

        String executeLifecycle = mojoDescriptor.getExecuteLifecycle();
        if ( executeLifecycle != null )
        {
            LifecycleBindings overlayBindings;
            try
            {
                overlayBindings =
                    lifecycleBindingManager.getPluginLifecycleOverlay( pluginDescriptor, executeLifecycle, project );
            }
            catch ( LifecycleLoaderException e )
            {
                throw new LifecyclePlannerException( "Failed to load overlay lifecycle: " + executeLifecycle
                                                     + ". Reason: " + e.getMessage(), e );
            }

            clonedPlan.addLifecycleOverlay( overlayBindings );
        }

        plan.addForkedExecution( mojoBinding, clonedPlan );

        addForkedLifecycleModifiers( clonedPlan, project );
    }

    /**
     * Retrieves the information necessary to create a new MojoBinding for a single-mojo forked execution, then hands
     * off to the
     * {@link DefaultBuildPlanner#modifyBuildPlanForForkedDirectInvocation(MojoBinding, MojoBinding, PluginDescriptor, ModifiablePlanElement, LifecycleBindings, MavenProject, LinkedList, List)}
     * method to actually inject the modification.
     */
    private void recurseSingleMojoFork( final MojoBinding mojoBinding, final PluginDescriptor pluginDescriptor,
                                        final BuildPlan plan, final MavenProject project )
        throws LifecyclePlannerException, LifecycleSpecificationException, LifecycleLoaderException
    {
        String referencingGoal = mojoBinding.getGoal();

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( referencingGoal );

        String executeGoal = mojoDescriptor.getExecuteGoal();

        if ( executeGoal == null )
        {
            return;
        }

        MojoDescriptor otherDescriptor = pluginDescriptor.getMojo( executeGoal );
        if ( otherDescriptor == null )
        {
            throw new LifecyclePlannerException( "Mojo: " + executeGoal + " (referenced by: " + referencingGoal
                                                 + ") does not exist in plugin: " + pluginDescriptor.getId() + "." );
        }

        MojoBinding binding =
            mojoBindingFactory.createMojoBinding( pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(),
                                                  pluginDescriptor.getVersion(), executeGoal, project );

        binding.setOrigin( "Forked from " + referencingGoal );

        plan.addForkedExecution( mojoBinding, Collections.singletonList( binding ) );
    }

}
