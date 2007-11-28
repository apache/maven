package org.apache.maven.errors;

import org.apache.maven.project.MavenProject;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.AggregatedBuildFailureException;
import org.apache.maven.ProjectBuildFailureException;
import org.apache.maven.Maven;

public aspect BuildFailureReporterAspect
    extends AbstractCoreReporterManagerAspect
{

    private pointcut le_execute( MavenSession session, ReactorManager reactorManager ):
        execution( void LifecycleExecutor+.execute( MavenSession, ReactorManager, .. ) )
        && args( session, reactorManager, .. );

    after( ReactorManager reactorManager, NoGoalsSpecifiedException err ):
        cflow( le_execute( MavenSession, reactorManager ) )
        && execution( NoGoalsSpecifiedException.new( .. ) )
        && this( err )
    {
        getReporter().reportNoGoalsSpecifiedException( reactorManager.getTopLevelProject(), err );
    }

    private pointcut aggregatedBuildFailureException_ctor( MojoBinding binding, MojoFailureException cause ):
        execution( AggregatedBuildFailureException.new( .., MojoBinding, MojoFailureException ) )
        && args( .., binding, cause );

    after( MavenSession session, MojoBinding binding, MojoFailureException cause ):
        cflow( le_execute( session, ReactorManager ) )
        && aggregatedBuildFailureException_ctor( binding, cause )
    {
        getReporter().reportAggregatedMojoFailureException( session, binding, cause );
    }

    private pointcut projectBuildFailureException_ctor( MojoBinding binding, MojoFailureException cause ):
        execution( ProjectBuildFailureException.new( .., MojoBinding, MojoFailureException ) )
        && args( .., binding, cause );

    after( MavenSession session, MojoBinding binding, MojoFailureException cause ):
        cflow( le_execute( session, ReactorManager ) )
        && projectBuildFailureException_ctor( binding, cause )
    {
        getReporter().reportProjectMojoFailureException( session, binding, cause );
    }

    private pointcut mvn_createReactorManager():
        execution( ReactorManager Maven+.createReactorManager( .. ) );

    after( ProjectCycleException err ):
        cflow( mvn_createReactorManager() )
        && execution( ProjectCycleException.new( .. ) )
        && this( err )
    {
        getReporter().reportProjectCycle( err );
    }

    private pointcut le_isTaskValid( MavenSession session, MavenProject rootProject ):
        execution( TaskValidationResult LifecycleExecutor+.isTaskValid( .., MavenSession, MavenProject ) )
        && args( .., session, rootProject );

    before( MavenSession session, MavenProject rootProject, PluginLoaderException cause, TaskValidationResult result ):
        cflow( le_isTaskValid( session, rootProject ) )
        && execution( TaskValidationResult.new( .., PluginLoaderException ) )
        && args( .., cause )
        && this( result )
    {
        getReporter().reportPluginErrorWhileValidatingTask( session, rootProject, cause, result );
    }

    before( MavenSession session, MavenProject rootProject, LifecycleSpecificationException cause, TaskValidationResult result ):
        cflow( le_isTaskValid( session, rootProject ) )
        && execution( TaskValidationResult.new( .., LifecycleSpecificationException ) )
        && args( .., cause )
        && this( result )
    {
        getReporter().reportLifecycleSpecErrorWhileValidatingTask( session, rootProject, cause, result );
    }

    before( MavenSession session, MavenProject rootProject, LifecycleLoaderException cause, TaskValidationResult result ):
        cflow( le_isTaskValid( session, rootProject ) )
        && execution( TaskValidationResult.new( .., LifecycleLoaderException ) )
        && args( .., cause )
        && this( result )
    {
        getReporter().reportLifecycleLoaderErrorWhileValidatingTask( session, rootProject, cause, result );
    }

}
