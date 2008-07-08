package org.apache.maven.errors;

import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.CyclicDependencyException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.extension.ExtensionManagerException;
import org.apache.maven.lifecycle.LifecycleException;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.DuplicateArtifactAttachmentException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.error.DefaultProjectErrorReporter;
import org.apache.maven.project.error.ProjectErrorReporter;
import org.apache.maven.project.error.ProjectReporterManager;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.reactor.MissingModuleException;
import org.apache.maven.realm.RealmManagementException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultCoreErrorReporter
    extends DefaultProjectErrorReporter
    implements CoreErrorReporter
{

    private static final String NEWLINE = "\n";

    public DefaultCoreErrorReporter( Map formattedMessageStore, Map realCauseStore, Map stackTraceRecommendationStore )
    {
        super( formattedMessageStore, realCauseStore, stackTraceRecommendationStore );
    }

    public DefaultCoreErrorReporter()
    {

    }

    public void reportNoGoalsSpecifiedException( MavenProject rootProject, NoGoalsSpecifiedException error )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "You must specify at least one goal or lifecycle phase to perform build steps." );
        writer.write( NEWLINE );
        writer.write( "The following list illustrates some commonly used build commands:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "  mvn clean" );
        writer.write( NEWLINE );
        writer.write( "    Deletes any build output (e.g. class files or JARs)." );
        writer.write( NEWLINE );
        writer.write( "  mvn test" );
        writer.write( NEWLINE );
        writer.write( "    Runs the unit tests for the project." );
        writer.write( NEWLINE );
        writer.write( "  mvn install" );
        writer.write( NEWLINE );
        writer.write( "    Copies the project artifacts into your local repository." );
        writer.write( NEWLINE );
        writer.write( "  mvn deploy" );
        writer.write( NEWLINE );
        writer.write( "    Copies the project artifacts into the remote repository." );
        writer.write( NEWLINE );
        writer.write( "  mvn site" );
        writer.write( NEWLINE );
        writer.write( "    Creates project documentation (e.g. reports or Javadoc)." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        addTips( CoreErrorTips.getNoGoalsTips(), writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Use \"mvn -?\" to show general usage information about Maven's command line." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        registerBuildError( error, writer.toString() );
    }

    private void addTips( List tips,
                          StringWriter writer )
    {
        if ( ( tips != null ) && !tips.isEmpty() )
        {
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Please see:" );
            writer.write( NEWLINE );
            for ( Iterator it = tips.iterator(); it.hasNext(); )
            {
                String tip = (String) it.next();

                writer.write( NEWLINE );
                writer.write( "\t- " );
                writer.write( tip );
            }
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "for more information." );
        }
    }

    public void reportAggregatedMojoFailureException( MavenSession session,
                                                      MojoBinding binding,
                                                      MojoFailureException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Mojo (aggregator): " );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "    " );
        writer.write( MojoBindingUtils.toString( binding ) );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "FAILED while executing in directory:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "    " );
        writer.write( session.getExecutionRootDirectory() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Reason:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        handleMojoFailureException( cause, writer );

        addTips( CoreErrorTips.getMojoFailureTips( binding ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportProjectMojoFailureException( MavenSession session,
                                                   MojoBinding binding,
                                                   MojoFailureException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Mojo: " );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "    " );
        writer.write( MojoBindingUtils.toString( binding ) );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "FAILED for project: " );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "    " );
        writer.write( session.getCurrentProject().getId() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Reason:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        handleMojoFailureException( cause, writer );

        addTips( CoreErrorTips.getMojoFailureTips( binding ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    private void handleMojoFailureException( MojoFailureException error,
                                             StringWriter writer )
    {
        String message = error.getLongMessage();
        if ( message == null )
        {
            message = error.getMessage();
        }

        writer.write( message );
        writer.write( NEWLINE );
    }

    public void reportProjectCycle( ProjectCycleException error )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven has detected a cyclic relationship among a set of projects in the current build." );
        writer.write( NEWLINE );
        writer.write( "The projects involved are:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        List projects = error.getProjects();
        Map projectsByVersionlessId = new HashMap();
        for ( Iterator it = projects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();
            projectsByVersionlessId.put( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ), project );
        }

        CycleDetectedException cause = (CycleDetectedException) error.getCause();
        List cycle = cause.getCycle();
        for ( Iterator it = cycle.iterator(); it.hasNext(); )
        {
            String id = (String) it.next();
            MavenProject project = (MavenProject) projectsByVersionlessId.get( id );

            writer.write( "- " );
            writer.write( project.getId() );
            writer.write( " (path: " );
            writer.write( project.getFile().getPath() );
            writer.write( ")" );
            writer.write( NEWLINE );
        }

        writer.write( NEWLINE );
        writer.write( "NOTE: This cycle usually indicates two projects listing one another as dependencies, but" );
        writer.write( NEWLINE );
        writer.write( "may also indicate one project using another as a parent, plugin, or extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        addTips( CoreErrorTips.getProjectCycleTips( error ), writer );

        registerBuildError( error, writer.toString(), cause );
    }

    public void reportLifecycleLoaderErrorWhileValidatingTask( String task,
                                                               MavenSession session,
                                                               MavenProject rootProject,
                                                               LifecycleLoaderException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Invalid mojo or lifecycle phase: " );
        writer.write( task );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Original error message was: " );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getTaskValidationTips( task, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportLifecycleSpecErrorWhileValidatingTask( String task,
                                                             MavenSession session,
                                                             MavenProject rootProject,
                                                             LifecycleSpecificationException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Invalid mojo or lifecycle phase: " );
        writer.write( task );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Original error message was: " );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getTaskValidationTips( task, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportInvalidPluginExecutionEnvironment( MojoBinding binding,
                                                         MavenProject project,
                                                         PluginExecutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The following plugin cannot function in the current build environment:" );

        writer.write( NEWLINE );
        writeMojoBinding( binding, writer );

        writer.write( "While building project:" );
        writer.write( NEWLINE );
        writeProjectCoordinate( project, writer );

        writer.write( NEWLINE );
        writer.write( "Reason: " );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getInvalidExecutionEnvironmentTips( binding, project, cause ), writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportMojoExecutionException( MojoBinding binding,
                                              MavenProject project,
                                              MojoExecutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The following mojo encountered an error while executing:" );

        writeMojoBinding( binding, writer );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        writer.write( "Reason: " );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getMojoExecutionExceptionTips( binding, project, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportMojoLookupError( MojoBinding binding,
                                       MavenProject project,
                                       ComponentLookupException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while looking up the following Mojo:" );

        writeMojoBinding( binding, writer );
        writer.write( "Referenced from project:" );
        writeProjectCoordinate( project, writer );

        writer.write( "Reason: " );
        writer.write( cause.getMessage() );

        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Root cause: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getMojoLookupErrorTips( binding, project, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    private Throwable getRootCause( Throwable cause )
    {
        Throwable nested = cause.getCause();
        if ( nested != null )
        {
            return getRootCause( nested );
        }
        else
        {
            return cause;
        }
    }

    public void reportAttemptToOverrideUneditableMojoParameter( Parameter currentParameter,
                                                                MojoBinding binding,
                                                                MavenProject project,
                                                                MavenSession session,
                                                                MojoExecution exec,
                                                                PathTranslator translator,
                                                                Logger logger,
                                                                PluginConfigurationException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The following mojo parameter cannot be configured:" );

        writeParameter( currentParameter, writer );
        writer.write( "in mojo:" );
        writeMojoBinding( binding, writer );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        PluginParameterExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(
                                                                                               session,
                                                                                               exec,
                                                                                               translator,
                                                                                               logger,
                                                                                               session.getExecutionProperties() );

        Object fromDefaultValue = null;
        Object fromExpression = null;
        try
        {
            if ( currentParameter.getDefaultValue() != null )
            {
                fromDefaultValue = evaluator.evaluate( currentParameter.getDefaultValue() );
            }

            if ( currentParameter.getExpression() != null )
            {
                fromExpression = evaluator.evaluate( currentParameter.getExpression() );
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            // ignored.
        }

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Instead of configuring this parameter directly, try configuring your POM or settings.xml file." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Using the default-value and expression annotations built into the mojo itself, these values were found in your build:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Value: " );
        writer.write( String.valueOf( fromDefaultValue ) );
        writer.write( NEWLINE );
        writer.write( "Using the expression:" );
        writer.write( currentParameter.getDefaultValue() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Value: " );
        writer.write( String.valueOf( fromExpression ) );
        writer.write( NEWLINE );
        writer.write( "Using the expression:" );
        writer.write( currentParameter.getExpression() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "If one of the above expressions rendered a valid value, it " +
        		"may give some indication of which part of the POM or settings.xml " +
        		"you can modify in order to change this parameter's value." );

        addTips( CoreErrorTips.getUneditableMojoParameterTips( currentParameter, binding, project, cause ), writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportUseOfBannedMojoParameter( Parameter currentParameter,
                                                MojoBinding binding,
                                                MavenProject project,
                                                String expression,
                                                String altExpression,
                                                ExpressionEvaluationException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The following mojo-parameter expression is banned for use in POM configurations:" );
        writer.write( NEWLINE );
        writer.write( expression );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Please use the following expression instead:" );
        writer.write( NEWLINE );
        writer.write( altExpression );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writeParameter( currentParameter, writer );
        writer.write( "in mojo:" );
        writeMojoBinding( binding, writer );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        addTips( CoreErrorTips.getBannedParameterExpressionTips( currentParameter, binding, project ), writer );

        registerBuildError( err, writer.toString() );
    }

    public void reportReflectionErrorWhileEvaluatingMojoParameter( Parameter currentParameter,
                                                                   MojoBinding binding,
                                                                   MavenProject project,
                                                                   String expression,
                                                                   Exception cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The following mojo-parameter expression could not be resolved, due to an erroroneous or empty reference in the object graph:" );
        writer.write( NEWLINE );
        writer.write( expression );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writeParameter( currentParameter, writer );
        writer.write( "in mojo:" );
        writeMojoBinding( binding, writer );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        addTips( CoreErrorTips.getReflectionErrorInParameterExpressionTips( expression, currentParameter, binding, project ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportMissingRequiredMojoParameter( MojoBinding binding,
                                                    MavenProject project,
                                                    List invalidParameters,
                                                    PluginParameterException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "One or more required mojo parameters have not been configured." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Mojo:" );
        writeMojoBinding( binding, writer );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Missing parameters include:" );
        for ( Iterator it = invalidParameters.iterator(); it.hasNext(); )
        {
            Parameter parameter = (Parameter) it.next();
            writer.write( NEWLINE );
            writer.write( parameter.getName() );

            if ( parameter.getAlias() != null )
            {
                writer.write( " (aliased as: " );
                writer.write( parameter.getAlias() );
            }
        }

        addTips( CoreErrorTips.getMissingRequiredParameterTips( invalidParameters, binding, project ), writer );

        registerBuildError( err, writer.toString() );
    }

    public void reportErrorApplyingMojoConfiguration( MojoBinding binding,
                                                      MavenProject project,
                                                      PlexusConfiguration config,
                                                      PluginConfigurationException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while configuring one of the mojos for your build." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Mojo:" );
        writeMojoBinding( binding, writer );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Here is the configuration it attempted to apply to the mojo:" );
        writeConfiguration( config, writer, 0 );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message:" );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getMojoConfigurationErrorTips( binding, project, config, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportProjectDependenciesNotFound( MavenProject project,
                                                   String scope,
                                                   ArtifactNotFoundException cause )
    {
        reportTransitiveResolutionError( project, scope, cause );
    }

    public void reportProjectDependenciesNotFound( MavenProject project,
                                                   String scope,
                                                   MultipleArtifactsNotFoundException cause )
    {
        reportTransitiveResolutionError( project, scope, cause );
    }

    public void reportProjectDependenciesUnresolvable( MavenProject project,
                                                       String scope,
                                                       ArtifactResolutionException cause )
    {
        reportTransitiveResolutionError( project, scope, cause );
    }

    private void reportTransitiveResolutionError( MavenProject project,
                                      String scope,
                                      AbstractArtifactResolutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Transitive dependency resolution for scope: " );
        writer.write( scope );
        writer.write( " has failed for your project." );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );

        Throwable root = getRootCause( cause );
        if ( root != cause )
        {
            writer.write( NEWLINE );
            writer.write( "Root error message: " );
            writer.write( root.getMessage() );
        }

        writeProjectCoordinate( project, writer );
        addTips( CoreErrorTips.getDependencyArtifactResolutionTips( project, scope, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    private void writeArtifactError( AbstractArtifactResolutionException cause,
                                    StringWriter writer )
    {
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( cause.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( cause.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( cause.getVersion() );
        writer.write( NEWLINE );
        writer.write( "Type: " );
        writer.write( cause.getType() );
        writer.write( NEWLINE );

        if ( cause.getClassifier() != null )
        {
            writer.write( NEWLINE );
            writer.write( "Classifier: " );
            writer.write( cause.getClassifier() );
        }

        if ( cause != null )
        {
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Error message: " );
            writer.write( cause.getMessage() );
            writer.write( NEWLINE );
            writer.write( "Root error message: " );
            writer.write( getRootCause( cause ).getMessage() );
        }
    }

    private void writeArtifactInfo( Artifact depArtifact,
                                    StringWriter writer,
                                    boolean includeScope )
    {
        writeArtifactInfo( depArtifact, null, writer, includeScope );
    }

    private void writeArtifactInfo( Artifact depArtifact,
                                    AbstractArtifactResolutionException cause,
                                    StringWriter writer,
                                    boolean includeScope )
    {
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( depArtifact.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( depArtifact.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( depArtifact.getVersion() );
        writer.write( NEWLINE );
        writer.write( "Type: " );
        writer.write( depArtifact.getType() );
        writer.write( NEWLINE );
        writer.write( "Scope: " );
        writer.write( depArtifact.getScope() );

        if ( depArtifact.getClassifier() != null )
        {
            writer.write( NEWLINE );
            writer.write( "Classifier: " );
            writer.write( depArtifact.getClassifier() );
        }

        if ( cause != null )
        {
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Error message: " );
            writer.write( cause.getMessage() );
            writer.write( NEWLINE );
            writer.write( "Root error message: " );
            writer.write( getRootCause( cause ).getMessage() );
        }
    }

    public void reportErrorLoadingPlugin( MojoBinding binding,
                                          MavenProject project,
                                          PluginLoaderException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while loading a plugin for use in your build." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Plugin:" );
        writePluginInformation( binding, writer );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message:" );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( "Root error message:" );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getErrorLoadingPluginTips( binding, project, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportErrorFormulatingBuildPlan( List tasks,
                                                 MavenProject project,
                                                 MavenSession session,
                                                 LifecycleException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven cannot calculate your build plan, given the following information:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Tasks:" );
        for ( Iterator it = tasks.iterator(); it.hasNext(); )
        {
            String task = (String) it.next();
            writer.write( NEWLINE );
            writer.write( "- " );
            writer.write( task );
        }
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Current project:" );
        writeProjectCoordinate( project, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getBuildPlanningErrorTips( tasks, project, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportInvalidMavenVersion( MavenProject project,
                                           ArtifactVersion mavenVersion,
                                           MavenExecutionException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The version of Maven currently in use is incompatible with your project's <maven/> prerequisite:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Current Maven Version: " );
        writer.write( mavenVersion.toString() );
        writer.write( NEWLINE );
        writer.write( "Version required:" );
        writer.write( project.getPrerequisites().getMaven() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project Information:" );
        writer.write( NEWLINE );
        writeProjectCoordinate( project, writer );

        addTips( CoreErrorTips.getIncompatibleProjectMavenVersionPrereqTips( project, mavenVersion ), writer );

        registerBuildError( err, writer.toString() );
    }

    public void handleSuperPomBuildingError( ProjectBuildingException exception )
    {
        ProjectErrorReporter projectReporter = ProjectReporterManager.getReporter();
        Throwable reportedException = projectReporter.findReportedException( exception );
        String formattedMessage = projectReporter.getFormattedMessage( reportedException );

        registerBuildError( exception, formattedMessage, reportedException );
    }

    public void reportErrorInterpolatingModel( Model model,
                                               Map inheritedValues,
                                               File pomFile,
                                               MavenExecutionRequest request,
                                               ModelInterpolationException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "You have an invalid expression in your POM (interpolation failed):" );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );

        writeProjectCoordinate( model, pomFile, writer );
        addTips( CoreErrorTips.getTipsForModelInterpolationError( model, pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportErrorResolvingExtensionDirectDependencies( Artifact extensionArtifact,
                                                                 Artifact projectArtifact,
                                                                 List remoteRepos,
                                                                 MavenExecutionRequest request,
                                                                 ArtifactMetadataRetrievalException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while trying to resolve an the direct dependencies for a build extension used in your project." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeArtifactInfo( projectArtifact, writer, false );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Extension:" );
        writeArtifactInfo( extensionArtifact, writer, false );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Failed Artifact:" );
        writeArtifactInfo( cause.getArtifact(), writer, false );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getErrorResolvingExtensionDirectDepsTips( extensionArtifact, projectArtifact, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportErrorResolvingExtensionDependencies( Artifact extensionArtifact,
                                                           Artifact projectArtifact,
                                                           List remoteRepos,
                                                           MavenExecutionRequest request,
                                                           ArtifactResolutionResult resolutionResult,
                                                           ExtensionManagerException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while trying to resolve the artifacts for a build extension used in your project." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeArtifactInfo( projectArtifact, writer, false );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Extension:" );
        writeArtifactInfo( extensionArtifact, writer, false );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        List missingArtifacts = resolutionResult.getMissingArtifacts();
        if ( ( missingArtifacts != null ) && !missingArtifacts.isEmpty() )
        {
            writer.write( "The following artifacts were not found." );
            writer.write( NEWLINE );
            writer.write( "(Format is: groupId:artifactId:version:type[:classifier])" );
            writer.write( NEWLINE );

            for ( Iterator it = missingArtifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();
                writer.write( NEWLINE );
                writeCompactArtifactCoordinate( "- ", artifact, writer );
            }
            writer.write( NEWLINE );
            writer.write( NEWLINE );
        }

        List circularDependencyExceptions = resolutionResult.getCircularDependencyExceptions();
        if ( ( circularDependencyExceptions != null ) && !circularDependencyExceptions.isEmpty() )
        {
            writer.write( "The following dependency cycles were found." );
            writer.write( NEWLINE );
            writer.write( "(Format is: groupId:artifactId:version:type[:classifier]), followed by the dependency trail that included the offending artifact.)" );
            writer.write( NEWLINE );

            int i = 1;
            for ( Iterator it = circularDependencyExceptions.iterator(); it.hasNext(); )
            {
                CyclicDependencyException cde = (CyclicDependencyException) it.next();
                Artifact artifact = cde.getArtifact();
                writer.write( NEWLINE );
                writeCompactArtifactCoordinate( i + ". ", artifact, writer );

                List trail = artifact.getDependencyTrail();
                for ( Iterator trailIt = trail.iterator(); trailIt.hasNext(); )
                {
                    String id = (String) trailIt.next();
                    writer.write( NEWLINE );
                    writer.write( "  - " );
                    writer.write( id );
                }

                writer.write( NEWLINE );
                i++;
            }

            writer.write( NEWLINE );
        }

        Map mapOfLists = new LinkedHashMap();

        List metadataExceptions = resolutionResult.getMetadataResolutionExceptions();
        if ( ( metadataExceptions != null ) && !metadataExceptions.isEmpty() )
        {
            mapOfLists.put( "The following metadata-resolution errors were found.", metadataExceptions );
        }

        List errorArtifactExceptions = resolutionResult.getErrorArtifactExceptions();
        if ( ( errorArtifactExceptions != null ) && !errorArtifactExceptions.isEmpty() )
        {
            mapOfLists.put( "The following artifact-resolution errors were found.", errorArtifactExceptions );
        }

        List versionRangeViolations = resolutionResult.getVersionRangeViolations();
        if ( ( versionRangeViolations != null ) && !versionRangeViolations.isEmpty() )
        {
            mapOfLists.put( "The following artifact version-range violations were found.", versionRangeViolations );
        }

        for ( Iterator entryIt = mapOfLists.entrySet().iterator(); entryIt.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) entryIt.next();
            String key = (String) entry.getKey();
            List exceptions = (List) entry.getValue();

            writer.write( key );
            writer.write( NEWLINE );

            int i = 1;
            for ( Iterator it = exceptions.iterator(); it.hasNext(); )
            {
                Exception e = (Exception) it.next();
                writer.write( NEWLINE );
                writer.write( i );
                writer.write( ". " );
                writer.write( e.getMessage() );

                Throwable t = getRootCause( e );
                if ( ( t != null ) && ( t != e ) )
                {
                    writer.write( NEWLINE );
                    writer.write( NEWLINE );
                    writer.write( "Root error: " );
                    writer.write( NEWLINE );
                    writer.write( NEWLINE );
                    writer.write( t.getMessage() );
                }

                writer.write( NEWLINE );
                i++;
            }

            writer.write( NEWLINE );
        }

        addTips( CoreErrorTips.getErrorResolvingExtensionArtifactsTips( extensionArtifact, projectArtifact, resolutionResult ),
                 writer );

        registerBuildError( err, writer.toString() );
    }

    private void writeCompactArtifactCoordinate( String linePrefix,
                                                 Artifact artifact,
                                                 StringWriter writer )
    {
        writer.write( linePrefix );
        writer.write( artifact.getGroupId() );
        writer.write( ":" );
        writer.write( artifact.getArtifactId() );
        writer.write( ":" );
        writer.write( artifact.getVersion() );
        writer.write( ":" );
        writer.write( artifact.getType() );
        if ( artifact.getClassifier() != null )
        {
            writer.write( ":" );
            writer.write( artifact.getClassifier() );
        }
    }

    public void reportErrorManagingRealmForExtension( Artifact extensionArtifact,
                                                      Artifact projectArtifact,
                                                      List remoteRepos,
                                                      MavenExecutionRequest request,
                                                      RealmManagementException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while trying to construct the classloader for a build extension used in your project." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeArtifactInfo( projectArtifact, writer, false );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Extension:" );
        writeArtifactInfo( extensionArtifact, writer, false );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getErrorManagingExtensionRealmTips( extensionArtifact, projectArtifact, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportErrorConfiguringExtensionPluginRealm( Plugin plugin,
                                                            Model originModel,
                                                            List remoteRepos,
                                                            MavenExecutionRequest request,
                                                            RealmManagementException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while trying to construct the classloader for a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getErrorManagingExtensionPluginRealmTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    private void writePluginInfo( Plugin plugin,
                                  StringWriter writer )
    {
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( plugin.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( plugin.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( plugin.getVersion() );
        writer.write( NEWLINE );
    }

    public void reportUnresolvableArtifactWhileAddingExtensionPlugin( Plugin plugin,
                                                                      Model originModel,
                                                                      List remoteRepos,
                                                                      MavenExecutionRequest request,
                                                                      ArtifactResolutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven failed to resolve one or more dependency artifacts for a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writeArtifactError( cause, writer );

        addTips( CoreErrorTips.getErrorResolvingExtensionPluginArtifactsTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportExtensionPluginArtifactNotFound( Plugin plugin,
                                                       Model originModel,
                                                       List remoteRepos,
                                                       MavenExecutionRequest request,
                                                       AbstractArtifactResolutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The artifact for a plugin used by your project as a build extension was not found." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writeArtifactError( cause, writer );

        addTips( CoreErrorTips.getErrorResolvingExtensionPluginArtifactsTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportUnresolvableExtensionPluginVersion( Plugin plugin,
                                                          Model originModel,
                                                          List remoteRepos,
                                                          MavenExecutionRequest request,
                                                          PluginVersionResolutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven could not resolve a valid version for a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getErrorResolvingExtensionPluginVersionTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportInvalidDependencyVersionInExtensionPluginPOM( Plugin plugin,
                                                             Model originModel,
                                                             List remoteRepos,
                                                             MavenExecutionRequest request,
                                                             InvalidDependencyVersionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an invalid version among the dependencies of a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Dependency:" );
        Dependency dep = cause.getDependency();
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( dep.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( dep.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( dep.getVersion() );
        writer.write( NEWLINE );
        writer.write( "Type: " );
        writer.write( dep.getType() );
        writer.write( NEWLINE );
        writer.write( "Scope: " );
        writer.write( dep.getScope() );
        if ( dep.getClassifier() != null )
        {
            writer.write( NEWLINE );
            writer.write( "Classifier: " );
            writer.write( dep.getClassifier() );
        }

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getInvalidDependencyVersionForExtensionPluginTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportErrorSearchingforCompatibleExtensionPluginVersion( Plugin plugin,
                                                                         Model originModel,
                                                                         List remoteRepos,
                                                                         MavenExecutionRequest request,
                                                                         String requiredMavenVersion,
                                                                         String currentMavenVersion,
                                                                         InvalidVersionSpecificationException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an incompatible version of a plugin used by your project as a build extension." );
        writer.write( " In attempting to search for an older version of this plugin, Maven failed to construct a valid version range for the search." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Current Maven version: " );
        writer.write( currentMavenVersion );
        writer.write( NEWLINE );
        writer.write( "Plugin requires Maven version: " );
        writer.write( requiredMavenVersion );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getInvalidPluginVersionRangeForExtensionPluginTips( plugin, originModel, requiredMavenVersion, currentMavenVersion, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportErrorSearchingforCompatibleExtensionPluginVersion( Plugin plugin,
                                                                         Model originModel,
                                                                         List remoteRepos,
                                                                         MavenExecutionRequest request,
                                                                         String requiredMavenVersion,
                                                                         String currentMavenVersion,
                                                                         ArtifactMetadataRetrievalException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an incompatible version of a plugin used by your project as a build extension." );
        writer.write( " In attempting to search for an older version of this plugin, Maven failed to retrieve the list of available plugin versions." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Current Maven version: " );
        writer.write( currentMavenVersion );
        writer.write( NEWLINE );
        writer.write( "Plugin requires Maven version: " );
        writer.write( requiredMavenVersion );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getInvalidPluginVersionRangeForExtensionPluginTips( plugin, originModel, requiredMavenVersion, currentMavenVersion, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportIncompatibleMavenVersionForExtensionPlugin( Plugin plugin,
                                                                  Model originModel,
                                                                  List remoteRepos,
                                                                  MavenExecutionRequest request,
                                                                  String requiredMavenVersion,
                                                                  String currentMavenVersion,
                                                                  PluginVersionResolutionException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an incompatible version of a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Current Maven version: " );
        writer.write( currentMavenVersion );
        writer.write( NEWLINE );
        writer.write( "Plugin requires Maven version: " );
        writer.write( requiredMavenVersion );

        addTips( CoreErrorTips.getInvalidPluginVersionRangeForExtensionPluginTips( plugin, originModel, requiredMavenVersion, currentMavenVersion ),
                 writer );

        registerBuildError( err, writer.toString() );
    }

    public void reportUnresolvableExtensionPluginPOM( Plugin plugin,
                                                      Model originModel,
                                                      List remoteRepos,
                                                      MavenExecutionRequest request,
                                                      ArtifactMetadataRetrievalException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven failed to resolve the POM of a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( "Without the POM, it is impossible to discover or resolve the plugin's dependencies." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getUnresolvableExtensionPluginPOMTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportErrorConfiguringExtensionPluginRealm( Plugin plugin,
                                                            Model originModel,
                                                            List remoteRepos,
                                                            MavenExecutionRequest request,
                                                            PluginManagerException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven failed to construct the classloader for a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getErrorManagingExtensionPluginRealmTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportExtensionPluginVersionNotFound( Plugin plugin,
                                                      Model originModel,
                                                      List remoteRepos,
                                                      MavenExecutionRequest request,
                                                      PluginVersionNotFoundException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven failed to resolve a valid version for a plugin used by your project as a build extension." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project:" );
        writeProjectCoordinate( originModel, null, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Plugin (used as an extension):" );
        writePluginInfo( plugin, writer );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getExtensionPluginVersionNotFoundTips( plugin, originModel, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    private void writeParameter( Parameter currentParameter,
                                 StringWriter writer )
    {
        writer.write( NEWLINE );
        writer.write( "Uneditable parameter:" );
        writer.write( NEWLINE );
        writer.write( "Name: " );
        writer.write( currentParameter.getName() );
        writer.write( NEWLINE );
        writer.write( "Alias: " );
        writer.write( currentParameter.getAlias() );
        writer.write( NEWLINE );
    }

    private void writeProjectCoordinate( MavenProject project,
                                         StringWriter writer )
    {
        writer.write( NEWLINE );
        if ( project == null )
        {
            writer.write( "No project is in use." );
        }
        else
        {
            writer.write( "Group-Id: " );
            writer.write( project.getGroupId() );
            writer.write( NEWLINE );
            writer.write( "Artifact-Id: " );
            writer.write( project.getArtifactId() );
            writer.write( NEWLINE );
            writer.write( "Version: " );
            writer.write( project.getVersion() );
            writer.write( NEWLINE );
            writer.write( "From file: " );
            writer.write( String.valueOf( project.getFile() ) );
        }
        writer.write( NEWLINE );
    }

    private void writeProjectCoordinate( Model model,
                                         File pomFile,
                                         StringWriter writer )
    {
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( model.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( model.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( model.getVersion() );
        writer.write( NEWLINE );
        writer.write( "From file: " );
        writer.write( pomFile == null ? "Not captured for this error report." : pomFile.getAbsolutePath() );
        writer.write( NEWLINE );
    }

    private void writePluginInformation( MojoBinding binding,
                                         StringWriter writer )
    {
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( binding.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( binding.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( binding.getVersion() );
        writer.write( NEWLINE );
        writer.write( "Referenced mojo: " );
        writer.write( binding.getGoal() );
        writer.write( NEWLINE );
        writer.write( "brought in via: " );
        writer.write( binding.getOrigin() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
    }

    private void writeMojoBinding( MojoBinding binding, StringWriter writer )
    {
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( binding.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( binding.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( binding.getVersion() );
        writer.write( NEWLINE );
        writer.write( "Mojo: " );
        writer.write( binding.getGoal() );
        writer.write( NEWLINE );
        writer.write( "brought in via: " );
        writer.write( binding.getOrigin() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
    }

    private void writeConfiguration( PlexusConfiguration config,
                                     StringWriter writer, int indent )
    {
        indent( writer, indent );

        writer.write( "<" );
        writer.write( config.getName() );
        writer.write( ">" );

        try
        {
            if ( config.getValue() != null )
            {
                writer.write( config.getValue() );
            }
        }
        catch ( PlexusConfigurationException e )
        {
            // skip it.
        }

        PlexusConfiguration[] children = config.getChildren();
        if ( ( children != null ) && ( children.length > 0 ) )
        {
            for ( int i = 0; i < children.length; i++ )
            {
                writer.write( NEWLINE );
                writeConfiguration( children[i], writer, indent + 1 );
            }

            indent( writer, indent );
        }

        writer.write( "</" );
        writer.write( config.getName() );
        writer.write( ">" );
        writer.write( NEWLINE );
    }

    private void indent( StringWriter writer,
                         int indent )
    {
        for ( int i = 0; i < indent; i++ )
        {
            writer.write( "  " );
        }
    }

    public void reportMissingModulePom( MissingModuleException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The module: " );
        writer.write( err.getModuleName() );
        writer.write( " cannot be found." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Module's expected path: " );
        writer.write( NEWLINE );
        writer.write( err.getModuleFile().getAbsolutePath() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Referenced by POM: " );
        writer.write( NEWLINE );
        writer.write( err.getPomFile().getAbsolutePath() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        addTips( CoreErrorTips.getMissingModuleTips( err.getPomFile(), err.getModuleFile(), err.getModuleName() ), writer );

        registerBuildError( err, writer.toString() );
    }

    public void reportInvalidPluginForDirectInvocation( String task,
                                                        MavenSession session,
                                                        MavenProject project,
                                                        InvalidPluginException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven encountered an error while loading a plugin for use in your build." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Original task invocation:" );
        writer.write( task );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "While building project:" );
        writeProjectCoordinate( project, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message:" );
        writer.write( NEWLINE );
        writer.write( err.getMessage() );

        addTips( CoreErrorTips.getInvalidPluginForDirectInvocationTips( task, session, project, err ), writer );

        registerBuildError( err, writer.toString() );
    }

    public void reportDuplicateAttachmentException( MojoBinding binding,
                                                    MavenProject project,
                                                    DuplicateArtifactAttachmentException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Your build attempted to attach multiple artifacts with the same classifier to the main project." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Mojo responsible for second attachment attempt:" );
        writer.write( MojoBindingUtils.toString( binding ) );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Reported for project:" );
        // Note: Using cause.getProject(), since an aggregator mojo (or, really, any sort)
        // could try to attach to any of the projects in the reactor, and in the case of the aggregator,
        // the project passed into the mojo execution and passed on here would just be the root project.
        writeProjectCoordinate( cause.getProject(), writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Artifact attachment:" );
        writeArtifactInfo( cause.getArtifact(), writer, false );

        addTips( CoreErrorTips.getDuplicateAttachmentTips( binding, project, cause ), writer );

        registerBuildError( cause, writer.toString() );
    }

}
