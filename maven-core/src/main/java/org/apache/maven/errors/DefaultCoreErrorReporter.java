package org.apache.maven.errors;

import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.dag.CycleDetectedException;

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
}
