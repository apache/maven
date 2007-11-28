package org.apache.maven.errors;

import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;

public interface CoreErrorReporter
{

    String NEWLINE = "\n";

    void clearErrors();

    String getFormattedMessage( Throwable error );

    Throwable getRealCause( Throwable error );

    Throwable findReportedException( Throwable error );

    void reportNoGoalsSpecifiedException( MavenProject rootProject, NoGoalsSpecifiedException error );

    void reportAggregatedMojoFailureException( MavenSession session, MojoBinding binding, MojoFailureException cause );

    void reportProjectMojoFailureException( MavenSession session, MojoBinding binding, MojoFailureException cause );

    void reportProjectCycle( ProjectCycleException error );

    void reportPluginErrorWhileValidatingTask( MavenSession session, MavenProject rootProject, PluginLoaderException cause, TaskValidationResult result );

    void reportLifecycleSpecErrorWhileValidatingTask( MavenSession session, MavenProject rootProject, LifecycleSpecificationException cause, TaskValidationResult result );

    void reportLifecycleLoaderErrorWhileValidatingTask( MavenSession session, MavenProject rootProject, LifecycleLoaderException cause, TaskValidationResult result );

}
