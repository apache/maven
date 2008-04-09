package org.apache.maven.errors;

import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.plan.BuildPlanner;
import org.apache.maven.lifecycle.plan.BuildPlan;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.statemgmt.StateManagementUtils;
import org.apache.maven.lifecycle.DefaultLifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.plan.LifecyclePlannerException;
import org.apache.maven.project.DuplicateArtifactAttachmentException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.PlexusContainer;

import java.util.List;

public privileged aspect LifecycleErrorReporterAspect
    extends AbstractCoreReporterAspect
{

    private pointcut le_executeGoalAndHandleFailures( MojoBinding binding ):
        execution( void DefaultLifecycleExecutor.executeGoalAndHandleFailures( MojoBinding, .. ) )
        && args( binding, .. );

    private pointcut le_executeGoalAndHandleFailures_withSession( MojoBinding binding,
                                                                  MavenSession session ):
        execution( void DefaultLifecycleExecutor.executeGoalAndHandleFailures( MojoBinding, MavenSession, .. ) )
        && args( binding, session, .. );

    private pointcut pm_executeMojo( MavenProject project ):
        execution( void PluginManager+.executeMojo( MavenProject, .. ) )
        && args( project, .. );

    private pointcut within_pm_executeMojo( MavenProject project ):
        withincode( void PluginManager+.executeMojo( MavenProject, .. ) )
        && args( project, .. );

    after( MojoBinding binding,
           MavenProject project) throwing ( PluginLoaderException cause ):
        ( cflow( le_executeGoalAndHandleFailures( MojoBinding ) )
          || cflow( execution( * LifecycleExecutor+.isTaskValid( .. ) ) ) )
        && execution( * PluginLoader+.loadPlugin( MojoBinding, MavenProject, .. ) )
        && args( binding, project, .. )
    {
        getReporter().reportErrorLoadingPlugin( binding, project, cause );
    }

    after( String task,
           MavenSession session,
           MavenProject project) throwing ( InvalidPluginException cause ):
        execution( private * DefaultLifecycleExecutor.getMojoDescriptorForDirectInvocation( String, MavenSession, MavenProject ) )
        && args( task, session, project )
    {
        getReporter().reportInvalidPluginForDirectInvocation( task, session, project, cause );
    }

    after( MojoBinding binding,
           MavenProject project) throwing ( DuplicateArtifactAttachmentException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && call( void Mojo+.execute() )
    {
        getReporter().reportDuplicateAttachmentException( binding, project, cause );
    }

    after( MojoBinding binding,
           MavenProject project) throwing ( MojoExecutionException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && call( void Mojo+.execute() )
    {
        // this will be covered by the reportErrorLoadingPlugin(..) method.
        if ( !StateManagementUtils.RESOLVE_LATE_BOUND_PLUGIN_GOAL.equals( binding.getGoal() ) )
        {
            getReporter().reportMojoExecutionException( binding, project, cause );
        }
    }

    PluginExecutionException around( MojoBinding binding,
                                     MavenProject project ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && call( PluginExecutionException.new( .., String ) )
    {
        PluginExecutionException cause = proceed( binding, project );
        getReporter().reportInvalidPluginExecutionEnvironment( binding, project, cause );

        return cause;
    }

    after( MojoBinding binding,
           MavenProject project) throwing ( ComponentLookupException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && withincode( Mojo DefaultPluginManager.getConfiguredMojo( .. ) )
        && call( Object PlexusContainer+.lookup( .. ) )
    {
        getReporter().reportMojoLookupError( binding, project, cause );
    }

    Parameter currentParameter;

    private pointcut paramGetName( Parameter parameter ):
        call( String Parameter.getName() )
        && target( parameter );

    private pointcut pm_validatePomConfig():
        execution( void DefaultPluginManager.validatePomConfiguration( .. ) );

    private pointcut within_pm_validatePomConfig():
        withincode( void DefaultPluginManager.validatePomConfiguration( .. ) );

    private pointcut pm_checkRequiredParameters():
        execution( void DefaultPluginManager.checkRequiredParameters( .. ) );

    private pointcut within_pm_checkRequiredParameters():
        withincode( void DefaultPluginManager.checkRequiredParameters( .. ) );

    before( Parameter parameter ):
        ( within_pm_validatePomConfig()
          || within_pm_checkRequiredParameters() )
        && paramGetName( parameter )
    {
        currentParameter = parameter;
    }

    after() returning:
        pm_validatePomConfig() ||
        pm_checkRequiredParameters()
    {
        currentParameter = null;
    }

    private pointcut pm_executeMojoWithSessionAndExec( MavenProject project,
                                                       MojoExecution exec,
                                                       MavenSession session,
                                                       DefaultPluginManager manager ):
        execution( void DefaultPluginManager.executeMojo( MavenProject, MojoExecution, MavenSession ) )
        && args( project, exec, session )
        && this( manager );

    after( MojoBinding binding,
           MavenProject project,
           MojoExecution exec,
           MavenSession session,
           DefaultPluginManager manager) throwing( PluginConfigurationException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojoWithSessionAndExec( project, exec, session, manager ) )
        && pm_validatePomConfig()
    {
        PathTranslator translator = manager.pathTranslator;
        Logger logger = new ConsoleLogger( Logger.LEVEL_INFO, "error reporting" );
        getReporter().reportAttemptToOverrideUneditableMojoParameter( currentParameter,
                                                                      binding,
                                                                      project,
                                                                      session,
                                                                      exec,
                                                                      translator,
                                                                      logger,
                                                                      cause );
    }

    PluginParameterException around( MojoBinding binding,
                                     MavenProject project,
                                     List invalidParameters ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && cflow( pm_checkRequiredParameters() )
        && call( PluginParameterException.new( .., List ) )
        && args( .., invalidParameters )
    {
        PluginParameterException err = proceed( binding, project, invalidParameters );

        getReporter().reportMissingRequiredMojoParameter( binding, project, invalidParameters, err );

        return err;
    }

    private pointcut ppee_evaluate( String expression ):
        execution( Object PluginParameterExpressionEvaluator.evaluate( String ) )
        && args( expression );

    private pointcut within_ppee_evaluate( String expression ):
        withincode( Object PluginParameterExpressionEvaluator.evaluate( String ) )
        && args( expression );

    before( MojoBinding binding,
            MavenProject project,
            String expression,
            ExpressionEvaluationException err ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && cflow( pm_checkRequiredParameters() )
        && cflow( ppee_evaluate( expression ) )
        && execution( ExpressionEvaluationException.new( String ) )
        && this( err )
    {
        getReporter().reportUseOfBannedMojoParameter( currentParameter,
                                                      binding,
                                                      project,
                                                      expression,
                                                      (String) PluginParameterExpressionEvaluator.BANNED_EXPRESSIONS.get( expression ),
                                                      err );
    }

    after( MojoBinding binding,
           MavenProject project,
           String expression) throwing ( Exception cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && cflow( pm_checkRequiredParameters() )
        && cflow( ppee_evaluate( expression ) )
        && within( PluginParameterExpressionEvaluator )
        && call( Object ReflectionValueExtractor.evaluate( String, Object ) )
    {
        getReporter().reportReflectionErrorWhileEvaluatingMojoParameter( currentParameter,
                                                                         binding,
                                                                         project,
                                                                         expression,
                                                                         cause );
    }

    after( MojoBinding binding,
           MavenProject project,
           PlexusConfiguration config) throwing( PluginConfigurationException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && execution( void DefaultPluginManager.populatePluginFields( *, *, PlexusConfiguration, .. ) )
        && args( *, *, config, .. )
    {
        getReporter().reportErrorApplyingMojoConfiguration( binding, project, config, cause );
    }

    private pointcut pm_resolveTransitiveDependencies( MavenProject project,
                                                       String scope ):
        execution( void DefaultPluginManager.resolveTransitiveDependencies( *, *, String, *, MavenProject, * ) )
        && args( *, *, scope, *, project, * );

    after( MavenProject project,
           String scope) throwing( ArtifactNotFoundException cause ):
        pm_resolveTransitiveDependencies( project, scope )
    {
        getReporter().reportProjectDependenciesNotFound( project, scope, cause );
    }

    after( MavenProject project,
           String scope) throwing( ArtifactResolutionException cause ):
        pm_resolveTransitiveDependencies( project, scope )
    {
        if ( cause instanceof MultipleArtifactsNotFoundException )
        {
            getReporter().reportProjectDependenciesNotFound( project,
                                                             scope,
                                                             (MultipleArtifactsNotFoundException) cause );
        }
        else
        {
            getReporter().reportProjectDependenciesUnresolvable( project, scope, cause );
        }
    }

    private pointcut le_getLifecycleBindings( List tasks,
                                              MavenProject configuringProject,
                                              String targetDescription ):
        execution( List DefaultLifecycleExecutor.getLifecycleBindings( List, MavenProject, *, String ) )
        && args( tasks, configuringProject, *, targetDescription );

    BuildPlan around( List tasks,
                      MavenProject project,
                      MavenSession session )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException:
            cflow( execution( * DefaultLifecycleExecutor.*( .. ) ) )
            && execution( BuildPlan BuildPlanner+.constructBuildPlan( List, MavenProject, MavenSession, * ) )
            && args( tasks, project, session, * )
    {
        try
        {
            return proceed( tasks, project, session );
        }
        catch ( LifecycleLoaderException cause )
        {
            getReporter().reportErrorFormulatingBuildPlan( tasks, project, session, cause );
            throw cause;
        }
        catch ( LifecyclePlannerException cause )
        {
            getReporter().reportErrorFormulatingBuildPlan( tasks, project, session, cause );
            throw cause;
        }
        catch ( LifecycleSpecificationException cause )
        {
            getReporter().reportErrorFormulatingBuildPlan( tasks, project, session, cause );
            throw cause;
        }
    }

}
