package org.apache.maven.errors;

import org.apache.maven.project.MavenProject;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.DefaultLifecycleExecutor;
import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.AggregatedBuildFailureException;
import org.apache.maven.ProjectBuildFailureException;
import org.apache.maven.Maven;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.lifecycle.binding.MojoBindingFactory;

public privileged aspect BuildFailureReporterAspect
    extends AbstractCoreReporterAspect
{

    private pointcut within_le_execute( MavenSession session, ReactorManager reactorManager ):
        withincode( void LifecycleExecutor+.execute( MavenSession, ReactorManager, .. ) )
        && args( session, reactorManager, .. );

    private pointcut le_execute( MavenSession session, ReactorManager reactorManager ):
        execution( void LifecycleExecutor+.execute( MavenSession, ReactorManager, .. ) )
        && args( session, reactorManager, .. );

    /**
     * Call stack is:
     * <br/>
     * <pre>
     * <code>
     * DefaultMaven.execute(MavenExecutionRequest)
     * --&gt; DefaultLifecycleExecutor.execute(MavenSession, ReactorManager, EventDispatcher)
     * &lt;-- NoGoalsSpecifiedException
     * </code>
     * </pre>
     */
    NoGoalsSpecifiedException around( ReactorManager reactorManager ):
        cflow( le_execute( MavenSession, reactorManager ) )
        && call( NoGoalsSpecifiedException.new( .. ) )
    {
        NoGoalsSpecifiedException err = proceed( reactorManager );

        getReporter().reportNoGoalsSpecifiedException( reactorManager.getTopLevelProject(), err );

        return err;
    }

    private pointcut aggregatedBuildFailureException_ctor( MojoBinding binding, MojoFailureException cause ):
        call( AggregatedBuildFailureException.new( .., MojoBinding, MojoFailureException ) )
        && args( .., binding, cause );

    /**
     * Call stack is:
     * <br/>
     * <pre>
     * <code>
     * DefaultMaven.execute(MavenExecutionRequest)
     * --&gt; DefaultLifecycleExecutor.execute(MavenSession, ReactorManager, EventDispatcher)
     *        --&gt; DefaultLifecycleExecutor.executeTaskSegments(List, ReactorManager, MavenSession, MavenProject, EventDispatcher)
     *               --&gt; (@aggregator plugin execution)
     * &lt;---------------- AggregatedBuildFailureException
     * </code>
     * </pre>
     */
    after( MavenSession session, MojoBinding binding, MojoFailureException cause ):
        cflow( le_execute( session, ReactorManager ) )
        && aggregatedBuildFailureException_ctor( binding, cause )
    {
        getReporter().reportAggregatedMojoFailureException( session, binding, cause );
    }

    private pointcut projectBuildFailureException_ctor( MojoBinding binding, MojoFailureException cause ):
        call( ProjectBuildFailureException.new( .., MojoBinding, MojoFailureException ) )
        && args( .., binding, cause );

    /**
     * Call stack is:
     * <br/>
     * <pre>
     * <code>
     * DefaultMaven.execute(MavenExecutionRequest)
     * --&gt; DefaultLifecycleExecutor.execute(MavenSession, ReactorManager, EventDispatcher)
     *        --&gt; DefaultLifecycleExecutor.executeTaskSegments(List, ReactorManager, MavenSession, MavenProject, EventDispatcher)
     *               --&gt; (normal plugin execution)
     * &lt;---------------- ProjectBuildFailureException
     * </code>
     * </pre>
     */
    after( MavenSession session, MojoBinding binding, MojoFailureException cause ):
        cflow( le_execute( session, ReactorManager ) )
        && projectBuildFailureException_ctor( binding, cause )
    {
        getReporter().reportProjectMojoFailureException( session, binding, cause );
    }

    private pointcut mvn_createReactorManager():
        execution( ReactorManager Maven+.createReactorManager( .. ) );

    /**
     * Call stack is:
     * <br/>
     * <pre>
     * <code>
     * DefaultMaven.execute(MavenExecutionRequest)
     * --&gt; DefaultMaven.createReactorManager(MavenExecutionRequest, MavenExecutionResult)
     * &lt;-- ProjectCycleException
     * </code>
     * </pre>
     */
    after( ProjectCycleException err ):
        cflow( mvn_createReactorManager() )
        && execution( ProjectCycleException.new( .. ) )
        && this( err )
    {
        getReporter().reportProjectCycle( err );
    }

    private pointcut within_le_getMojoDescriptorForDirectInvocation():
        withincode( * DefaultLifecycleExecutor.getMojoDescriptorForDirectInvocation( String, MavenSession, MavenProject ) );

    /**
     * Call stack is:
     * <br/>
     * <pre>
     * <code>
     * DefaultMaven.execute(MavenExecutionRequest)
     * --&gt; DefaultLifecycleExecutor.isTaskValid(String, MavenSession, MavenProject)
     *        --&gt; catch( LifecycleSpecificationException )
     * &lt;-- TaskValidationResult
     * </code>
     * </pre>
     */
    after( String task, MavenSession session, MavenProject rootProject ) throwing ( LifecycleSpecificationException cause ):
        within_le_getMojoDescriptorForDirectInvocation()
        && call( * MojoBindingFactory+.parseMojoBinding( String, MavenProject, MavenSession, .. ) )
        && args( task, rootProject, session, .. )
    {
        getReporter().reportLifecycleSpecErrorWhileValidatingTask( task, session, rootProject, cause );
    }

    /**
     * Call stack is:
     * <br/>
     * <pre>
     * <code>
     * DefaultMaven.execute(MavenExecutionRequest)
     * --&gt; DefaultLifecycleExecutor.isTaskValid(String, MavenSession, MavenProject)
     *        --&gt; catch( LifecycleLoaderException )
     * &lt;-- TaskValidationResult
     * </code>
     * </pre>
     */
    after( String task, MavenSession session, MavenProject rootProject ) throwing ( LifecycleLoaderException cause ):
        within_le_getMojoDescriptorForDirectInvocation()
        && call( * MojoBindingFactory+.parseMojoBinding( String, MavenProject, MavenSession, .. ) )
        && args( task, rootProject, session, .. )
    {
        getReporter().reportLifecycleLoaderErrorWhileValidatingTask( task, session, rootProject, cause );
    }

}
