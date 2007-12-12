package org.apache.maven.errors;

import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultCoreErrorReporter
    implements CoreErrorReporter
{

    private Map formattedMessages = new HashMap();

    private Map realCauses = new HashMap();

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#clearErrors()
     */
    public void clearErrors()
    {
        formattedMessages.clear();
        realCauses.clear();
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#hasInformationFor(java.lang.Throwable)
     */
    public Throwable findReportedException( Throwable error )
    {
        if ( formattedMessages.containsKey( error ) )
        {
            return error;
        }
        else if ( error.getCause() != null )
        {
            return findReportedException( error.getCause() );
        }

        return null;
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#getFormattedMessage(java.lang.Throwable)
     */
    public String getFormattedMessage( Throwable error )
    {
        return (String) formattedMessages.get( error );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#getRealCause(java.lang.Throwable)
     */
    public Throwable getRealCause( Throwable error )
    {
        return (Throwable) realCauses.get( error );
    }

    private void registerBuildError( Throwable error,
                                     String formattedMessage,
                                     Throwable realCause )
    {
        formattedMessages.put( error, formattedMessage );
        if ( realCause != null )
        {
            realCauses.put( error, realCause );
        }
    }

    private void registerBuildError( Throwable error,
                                     String formattedMessage )
    {
        formattedMessages.put( error, formattedMessage );
    }

    public void reportNoGoalsSpecifiedException( MavenProject rootProject, NoGoalsSpecifiedException error )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "You have not specified any goals or lifecycle phases for Maven to execute." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Either specify a goal or lifecycle phase on the command line" );
        writer.write( NEWLINE );
        writer.write( "(you may want to try \'package\' to get started), or configure the " );
        writer.write( NEWLINE );
        writer.write( "<defaultGoal/> element in the build section of your project POM." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "NOTE: You can also chain multiple goals/phases together, as in the following example:" );
        writer.write( NEWLINE );
        writer.write( "mvn clean package" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        addTips( CoreErrorTips.getNoGoalsTips(), writer );

        registerBuildError( error, writer.toString() );
    }

    private void addTips( List tips,
                          StringWriter writer )
    {
        if ( ( tips != null ) && !tips.isEmpty() )
        {
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Some tips:" );
            for ( Iterator it = tips.iterator(); it.hasNext(); )
            {
                String tip = (String) it.next();

                writer.write( NEWLINE );
                writer.write( "\t- " );
                writer.write( tip );
            }
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

    public void reportLifecycleLoaderErrorWhileValidatingTask( MavenSession session,
                                                               MavenProject rootProject,
                                                               LifecycleLoaderException cause,
                                                               TaskValidationResult result )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Invalid mojo or lifecycle phase: " );
        writer.write( result.getInvalidTask() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Original error message was: " );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getTaskValidationTips( result, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportLifecycleSpecErrorWhileValidatingTask( MavenSession session,
                                                             MavenProject rootProject,
                                                             LifecycleSpecificationException cause,
                                                             TaskValidationResult result )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Invalid mojo or lifecycle phase: " );
        writer.write( result.getInvalidTask() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Original error message was: " );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getTaskValidationTips( result, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportPluginErrorWhileValidatingTask( MavenSession session,
                                                      MavenProject rootProject,
                                                      PluginLoaderException cause,
                                                      TaskValidationResult result )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Invalid mojo or lifecycle phase: " );
        writer.write( result.getInvalidTask() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Failed to load plugin: " );
        writer.write( cause.getPluginKey() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Original error message was: " );
        writer.write( cause.getMessage() );

        addTips( CoreErrorTips.getTaskValidationTips( result, cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    public void reportMissingPluginDescriptor( MojoBinding binding,
                                               MavenProject project,
                                               LifecycleExecutionException err )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven cannot find a plugin required by your build:" );

        writeMojoBinding( binding, writer );
        writer.write( "Referenced from project:" );
        writeProjectCoordinate( project, writer );

        writer.write( "NOTE: If the above information seems incorrect, check that " +
        		"the corresponding <plugin/> section in your POM is correct." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "If you specified this plugin directly using something like " +
        		"'javadoc:javadoc', check that the <pluginGroups/> section in your " +
        		"$HOME/.m2/settings.xml contains the proper groupId for the plugin " +
        		"you are trying to use (each groupId goes in a separate <pluginGroup/> " +
        		"element within the <pluginGroups/> section." );

        addTips( CoreErrorTips.getMissingPluginDescriptorTips( binding, project ), writer );

        registerBuildError( err, writer.toString() );
    }

    public void reportInvalidPluginExecutionEnvironment( MojoBinding binding,
                                                         MavenProject project,
                                                         PluginExecutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The following plugin cannot function in the current build environment:" );

        writeMojoBinding( binding, writer );
        writer.write( "Referenced from project:" );
        writeProjectCoordinate( project, writer );

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

    public void reportProjectDependenciesUnresolvable( MavenProject project,
                                                       String scope,
                                                       ArtifactResolutionException cause )
    {
        reportTransitiveResolutionError( project, scope, cause );
    }

    public void reportProjectDependencyArtifactNotFound( MavenProject project,
                                                         Artifact artifact,
                                                         ArtifactNotFoundException cause )
    {
        reportArtifactError( project, artifact, cause );
    }

    public void reportProjectDependencyArtifactUnresolvable( MavenProject project,
                                                             Artifact artifact,
                                                             ArtifactResolutionException cause )
    {
        reportArtifactError( project, artifact, cause );
    }

    private void reportTransitiveResolutionError( MavenProject project,
                                      String scope,
                                      AbstractArtifactResolutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Transitive dependency resolution for scope: " );
        writer.write( scope );
        writer.write( "has failed for your project." );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        writeProjectCoordinate( project, writer );
        addTips( CoreErrorTips.getDependencyArtifactResolutionTips( project, scope, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    private void reportArtifactError( MavenProject project,
                                      Artifact depArtifact,
                                      AbstractArtifactResolutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Maven could not resolve one of your project dependencies from the repository:" );

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

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        writeProjectCoordinate( project, writer );
        addTips( CoreErrorTips.getDependencyArtifactResolutionTips( project, depArtifact, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
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
                                                 MavenProject configuringProject,
                                                 String targetDescription,
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
        writeProjectCoordinate( configuringProject, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Build execution sub-segment:" );
        writer.write( targetDescription );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( "Root error message: " );
        writer.write( getRootCause( cause ).getMessage() );

        addTips( CoreErrorTips.getBuildPlanningErrorTips( tasks, configuringProject, cause ), writer );

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
        writer.write( String.valueOf( pomFile ) );
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

    public void handleProjectBuildingError( MavenExecutionRequest request,
                                            File pomFile,
                                            ProjectBuildingException exception )
    {
        // TODO Auto-generated method stub

    }

    public void reportInvalidMavenVersion( MavenProject project,
                                           ArtifactVersion mavenVersion,
                                           MavenExecutionException err )
    {
        // TODO Auto-generated method stub

    }

    public void reportPomFileScanningError( File basedir,
                                            String includes,
                                            String excludes,
                                            IOException cause )
    {
        // TODO Auto-generated method stub

    }

    public void reportPomFileCanonicalizationError( File pomFile,
                                                    IOException cause )
    {
        // TODO Auto-generated method stub

    }

    public void handleSuperPomBuildingError( MavenExecutionRequest request,
                                             ArtifactRepository localRepository,
                                             ProfileManager globalProfileManager,
                                             ProjectBuildingException exception )
    {
        // TODO Auto-generated method stub

    }

    public void handleSuperPomBuildingError( ProfileManager globalProfileManager,
                                             ProjectBuildingException exception )
    {
        // TODO Auto-generated method stub

    }

    public void handleSuperPomBuildingError( ProjectBuildingException exception )
    {
        // TODO Auto-generated method stub

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


}
