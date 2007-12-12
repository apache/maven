package org.apache.maven.errors;

import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.logging.Logger;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.DefaultLifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.util.List;

public privileged aspect LifecycleErrorReporterAspect
    extends AbstractCoreReporterAspect
{

    private pointcut le_executeGoalAndHandleFailures( MojoBinding binding ):
        execution( void DefaultLifecycleExecutor.executeGoalAndHandleFailures( MojoBinding, .. ) )
        && args( binding, .. );

    private pointcut pm_executeMojo( MavenProject project ):
        execution( void PluginManager+.executeMojo( MavenProject, .. ) )
        && args( project, .. );

    before( MojoBinding binding, MavenProject project, LifecycleExecutionException err ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && execution( LifecycleExecutionException.new( String, MavenProject ) )
        && args( .., project )
        && this( err )
    {
        getReporter().reportMissingPluginDescriptor( binding, project, err );
    }

    before( MojoBinding binding, MavenProject project, PluginLoaderException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && call( LifecycleExecutionException.new( String, MavenProject, PluginLoaderException ) )
        && args( *, project, cause )
    {
        getReporter().reportErrorLoadingPlugin( binding, project, cause );
    }

    before( MojoBinding binding, MavenProject project, MojoExecutionException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && handler( MojoExecutionException )
        && args( cause )
    {
        getReporter().reportMojoExecutionException( binding, project, cause );
    }

    before( MojoBinding binding, MavenProject project, PluginExecutionException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && !handler( MojoExecutionException )
        && execution( PluginExecutionException.new( .., String ) )
        && this( cause )
    {
        getReporter().reportInvalidPluginExecutionEnvironment( binding, project, cause );
    }

    before( MojoBinding binding, MavenProject project, ComponentLookupException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && withincode( Mojo DefaultPluginManager.getConfiguredMojo( .. ) )
        && handler( ComponentLookupException )
        && args( cause )
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

    private pointcut pm_executeMojoWithSessionAndExec( MavenProject project, MojoExecution exec, MavenSession session, DefaultPluginManager manager ):
        execution( void DefaultPluginManager.executeMojo( MavenProject, MojoExecution, MavenSession ) )
        && args( project, exec, session )
        && this( manager );

    after( MojoBinding binding, MavenProject project, MojoExecution exec, MavenSession session, DefaultPluginManager manager ) throwing( PluginConfigurationException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojoWithSessionAndExec( project, exec, session, manager ) )
        && pm_validatePomConfig()
    {
        PathTranslator translator = manager.pathTranslator;
        Logger logger = new ConsoleLogger( Logger.LEVEL_INFO, "error reporting" );
        getReporter().reportAttemptToOverrideUneditableMojoParameter( currentParameter, binding, project, session, exec, translator, logger, cause );
    }

    before( MojoBinding binding, MavenProject project, List invalidParameters, PluginParameterException err ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && cflow( pm_checkRequiredParameters() )
        && execution( PluginParameterException.new( .., List ) )
        && args( .., invalidParameters )
        && this( err )
    {
        getReporter().reportMissingRequiredMojoParameter( binding, project, invalidParameters, err );
    }

    private pointcut ppee_evaluate( String expression ):
        execution( Object PluginParameterExpressionEvaluator.evaluate( String ) )
        && args( expression );

    private pointcut within_ppee_evaluate( String expression ):
        withincode( Object PluginParameterExpressionEvaluator.evaluate( String ) )
        && args( expression );

    before( MojoBinding binding, MavenProject project, String expression, ExpressionEvaluationException err ):
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

    before( MojoBinding binding, MavenProject project, String expression, Exception cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && cflow( pm_checkRequiredParameters() )
        && cflow( ppee_evaluate( expression ) )
        && within( PluginParameterExpressionEvaluator )
        && handler( Exception )
        && args( cause )
    {
        getReporter().reportReflectionErrorWhileEvaluatingMojoParameter( currentParameter,
                                                      binding,
                                                      project,
                                                      expression,
                                                      cause );
    }

    after( MojoBinding binding, MavenProject project, PlexusConfiguration config ) throwing( PluginConfigurationException cause ):
        cflow( le_executeGoalAndHandleFailures( binding ) )
        && cflow( pm_executeMojo( project ) )
        && execution( void DefaultPluginManager.populatePluginFields( *, *, PlexusConfiguration, .. ) )
        && args( *, *, config, .. )
    {
        getReporter().reportErrorApplyingMojoConfiguration( binding, project, config, cause );
    }

    private pointcut pm_resolveTransitiveDependencies( MavenProject project, String scope ):
        execution( void DefaultPluginManager.resolveTransitiveDependencies( *, *, String, *, MavenProject ) )
        && args( *, *, scope, *, project );

    after( MavenProject project, String scope ) throwing( ArtifactNotFoundException cause ):
        pm_resolveTransitiveDependencies( project, scope )
    {
        getReporter().reportProjectDependenciesNotFound( project, scope, cause );
    }

    after( MavenProject project, String scope ) throwing( ArtifactResolutionException cause ):
        pm_resolveTransitiveDependencies( project, scope )
    {
        getReporter().reportProjectDependenciesUnresolvable( project, scope, cause );
    }

    private pointcut within_pm_downloadDependencies( MavenProject project ):
        withincode( void DefaultPluginManager.downloadDependencies( MavenProject, .. ) )
        && args( project, .. );

    private pointcut ar_resolve( Artifact artifact ):
        call( * ArtifactResolver+.resolve( Artifact, ..) )
        && args( artifact, .. );

    after( MavenProject project, Artifact artifact ) throwing( ArtifactNotFoundException cause ):
        within_pm_downloadDependencies( project )
        && ar_resolve( artifact )
    {
        getReporter().reportProjectDependencyArtifactNotFound( project, artifact, cause );
    }

    after( MavenProject project, Artifact artifact ) throwing( ArtifactResolutionException cause ):
        within_pm_downloadDependencies( project )
        && ar_resolve( artifact )
    {
        getReporter().reportProjectDependencyArtifactUnresolvable( project, artifact, cause );
    }

    private pointcut le_getLifecycleBindings( List tasks, MavenProject configuringProject, String targetDescription ):
        execution( List DefaultLifecycleExecutor.getLifecycleBindings( List, MavenProject, *, String ) )
        && args( tasks, configuringProject, *, targetDescription );

    before( List tasks, MavenProject configuringProject, String targetDescription, LifecycleException cause ):
        cflow( le_getLifecycleBindings( tasks, configuringProject, targetDescription ) )
        && call( LifecycleExecutionException.new( .., LifecycleException ) )
        && args( .., cause )
    {
        getReporter().reportErrorFormulatingBuildPlan( tasks, configuringProject, targetDescription, cause );
    }

}
