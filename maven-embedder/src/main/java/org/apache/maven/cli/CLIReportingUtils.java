package org.apache.maven.cli;

import org.apache.maven.AggregatedBuildFailureException;
import org.apache.maven.BuildFailureException;
import org.apache.maven.InvalidTaskException;
import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectBuildFailureException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Utility class used to report errors, statistics, application version info, etc.
 *
 * @author jdcasey
 *
 */
public final class CLIReportingUtils
{

    public static final long MB = 1024 * 1024;

    public static final int MS_PER_SEC = 1000;

    public static final int SEC_PER_MIN = 60;

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String OS_ARCH = System.getProperty( "os.arch" ).toLowerCase( Locale.US );

    public static final String OS_VERSION = System.getProperty( "os.version" )
                                                  .toLowerCase( Locale.US );

    private static final String NEWLINE = System.getProperty( "line.separator" );

    private CLIReportingUtils()
    {
    }

    static void showVersion()
    {
        InputStream resourceAsStream;
        try
        {
            Properties properties = new Properties();
            resourceAsStream = MavenCli.class.getClassLoader()
                                             .getResourceAsStream( "META-INF/maven/org.apache.maven/maven-core/pom.properties" );
            properties.load( resourceAsStream );

            if ( properties.getProperty( "builtOn" ) != null )
            {
                System.out.println( "Maven version: "
                                    + properties.getProperty( "version", "unknown" ) + " built on "
                                    + properties.getProperty( "builtOn" ) );
            }
            else
            {
                System.out.println( "Maven version: "
                                    + properties.getProperty( "version", "unknown" ) );
            }

            System.out.println( "Java version: "
                                + System.getProperty( "java.version", "<unknown java version>" ) );

            //TODO: when plexus can return the family type, add that here because it will make it easier to know what profile activation settings to use.
            System.out.println( "OS name: \"" + OS_NAME + "\" version: \"" + OS_VERSION
                                + "\" arch: \"" + OS_ARCH + "\"" );
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
    }

    /**
     * Logs result of the executed build.
     * @param request - build parameters
     * @param result - result of build
     * @param logger - the logger to use
     */
    public static void logResult( MavenExecutionRequest request,
                           MavenExecutionResult result,
                           MavenEmbedderLogger logger )
    {
        ReactorManager reactorManager = result.getReactorManager();

        logReactorSummary( reactorManager, logger );

        if ( ( reactorManager != null ) && reactorManager.hasBuildFailures() )
        {
            logErrors( reactorManager, request.isShowErrors(), logger );

            if ( !ReactorManager.FAIL_NEVER.equals( reactorManager.getFailureBehavior() ) )
            {
                logger.info( "BUILD FAILED" );

                line( logger );

                stats( request.getStartTime(), logger );

                line( logger );
            }
            else
            {
                logger.info( " + Ignoring build failures" );
            }
        }

        if ( result.hasExceptions() )
        {
            for ( Iterator i = result.getExceptions().iterator(); i.hasNext(); )
            {
                Exception e = (Exception) i.next();

                showError( e, request.isShowErrors(), logger );
            }
        }
        else
        {
            line( logger );

            logger.info( "BUILD SUCCESSFUL" );

            line( logger );

            stats( request.getStartTime(), logger );

            line( logger );
        }

        logger.close();
    }

    private static void logErrors( ReactorManager rm,
                                   boolean showErrors,
                                   MavenEmbedderLogger logger )
    {
        for ( Iterator it = rm.getSortedProjects().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            if ( rm.hasBuildFailure( project ) )
            {
                BuildFailure buildFailure = rm.getBuildFailure( project );

                logger.info( "Error for project: " + project.getName() + " (during "
                             + buildFailure.getTask() + ")" );

                line( logger );
            }
        }

        if ( !showErrors )
        {
            logger.info( "For more information, run Maven with the -e switch" );

            line( logger );
        }
    }

    static void showError( String message,
                           Exception e,
                           boolean showErrors )
    {
        showError( message, e, showErrors, new MavenEmbedderConsoleLogger() );
    }

    static void showError( Exception e,
                           boolean show,
                           MavenEmbedderLogger logger )
    {
        showError( null, e, show, logger );
    }

    /**
     * Format the exception and output it through the logger.
     * @param message - error message
     * @param e - exception that was thrown
     * @param showStackTraces
     * @param logger
     */
    public static void showError( String message,
                           Exception e,
                           boolean showStackTraces,
                           MavenEmbedderLogger logger )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );

        if ( message != null )
        {
            writer.write( message );
            writer.write( NEWLINE );
        }

        buildErrorMessage( e, showStackTraces, writer );

        writer.write( NEWLINE );

        if ( showStackTraces )
        {
            writer.write( "Error stacktrace:" );
            writer.write( NEWLINE );
            e.printStackTrace( new PrintWriter( writer ) );

        }
        else
        {
            writer.write( "For more information, run with the -e flag" );
        }

        logger.error( writer.toString() );
    }

    public static void buildErrorMessage( Exception e,
                                           boolean showStackTraces,
                                           StringWriter writer )
    {
        boolean handled = false;

        if ( e instanceof BuildFailureException )
        {
            handled = handleBuildFailureException( (BuildFailureException) e,
                                                   showStackTraces,
                                                   writer );
        }
        else if ( e instanceof ProjectBuildingException )
        {
            handled = handleProjectBuildingException( (ProjectBuildingException) e,
                                                      showStackTraces,
                                                      writer );
        }
        else if ( e instanceof LifecycleExecutionException )
        {
            handled = handleLifecycleExecutionException( (LifecycleExecutionException) e,
                                                         showStackTraces,
                                                         writer );
        }
        else if ( e instanceof DuplicateProjectException )
        {
            handled = handleDuplicateProjectException( (DuplicateProjectException) e,
                                                       showStackTraces,
                                                       writer );
        }
        else if ( e instanceof MavenExecutionException )
        {
            handled = handleMavenExecutionException( (MavenExecutionException) e,
                                                     showStackTraces,
                                                     writer );
        }

        if ( !handled )
        {
            handleGenericException( e, showStackTraces, writer );
        }
    }

    private static boolean handleMavenExecutionException( MavenExecutionException e,
                                                          boolean showStackTraces,
                                                          StringWriter writer )
    {

        // =====================================================================
        // Cases covered:
        // =====================================================================
        //
        // MavenExecutionException(String, File, ProjectBuildingException)
        // MavenExecutionException(String, ExtensionScanningException)
        // MavenExecutionException(String, IOException)
        //
        // =====================================================================
        // Cases left to cover:
        // =====================================================================
        //
        // MavenExecutionException(String, ArtifactResolutionException)
        // MavenExecutionException(String, File)

        Throwable cause = e.getCause();
        if ( cause != null )
        {
            if ( cause instanceof IOException )
            {
                writer.write( e.getMessage() );
                writer.write( NEWLINE );

                handleGenericException( cause, showStackTraces, writer );

                return true;
            }
            else if ( cause instanceof ExtensionScanningException )
            {
                writer.write( "While scanning for build extensions:" );
                writer.write( NEWLINE );
                writer.write( NEWLINE );

                Throwable nestedCause = cause.getCause();
                if ( ( nestedCause != null ) && ( nestedCause instanceof ProjectBuildingException ) )
                {
                    return handleProjectBuildingException( (ProjectBuildingException) nestedCause,
                                                           showStackTraces,
                                                           writer );
                }
                else
                {
                    handleGenericException( cause, showStackTraces, writer );

                    return true;
                }
            }
            else if ( cause instanceof ProjectBuildingException )
            {
                return handleProjectBuildingException( (ProjectBuildingException) cause,
                                                       showStackTraces,
                                                       writer );
            }
            else if ( cause instanceof ArtifactResolutionException )
            {

            }
        }
        else
        {

        }

        if ( e.getPomFile() != null )
        {
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "POM File: " );
            writer.write( e.getPomFile().getAbsolutePath() );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
        }

        return true;
    }

    private static boolean handleDuplicateProjectException( DuplicateProjectException e,
                                                            boolean showStackTraces,
                                                            StringWriter writer )
    {
        // =====================================================================
        // Cases covered:
        // =====================================================================
        //
        // DuplicateProjectException(String, File, File, String)

        File existing = e.getExistingProjectFile();
        File conflicting = e.getConflictingProjectFile();
        String projectId = e.getProjectId();

        writer.write( "Duplicated project detected." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project: " + projectId );
        writer.write( NEWLINE );
        writer.write( "File: " );
        writer.write( existing.getAbsolutePath() );
        writer.write( NEWLINE );
        writer.write( "File: " );
        writer.write( conflicting.getAbsolutePath() );

        return true;
    }

    private static void handleGenericException( Throwable exception,
                                                boolean showStackTraces,
                                                StringWriter writer )
    {
        writer.write( exception.getMessage() );
        writer.write( NEWLINE );
    }

    private static boolean handleLifecycleExecutionException( LifecycleExecutionException e,
                                                              boolean showStackTraces,
                                                              StringWriter writer )
    {

        // =====================================================================
        // Cases covered:
        // =====================================================================
        //
        // LifecycleExecutionException(String, MavenProject, PluginNotFoundException)
        // LifecycleExecutionException(String, MavenProject, InvalidDependencyVersionException)
        // LifecycleExecutionException(String, MavenProject, InvalidVersionSpecificationException)
        // LifecycleExecutionException(String, MavenProject, ArtifactNotFoundException)
        // LifecycleExecutionException(String, MavenProject, ArtifactResolutionException)
        // LifecycleExecutionException(String, MavenProject, PluginVersionNotFoundException)
        // LifecycleExecutionException(String, MavenProject, PluginVersionResolutionException)
        //
        // =====================================================================
        // Cases left to cover:
        // =====================================================================
        //
        // LifecycleExecutionException(String, MavenProject, InvalidPluginException)
        // LifecycleExecutionException(String, MavenProject, LifecycleException)
        // LifecycleExecutionException(String, MavenProject, PluginConfigurationException)
        // LifecycleExecutionException(String, MavenProject, PluginLoaderException)
        // LifecycleExecutionException(String, MavenProject, PluginManagerException)
        //    ...this includes PluginExecutionException, which wraps MojoExecutionException
        //    with MojoExecution and MavenProject context info.

        boolean result = false;

        Throwable cause = e.getCause();
        if ( cause != null )
        {
            if ( cause instanceof PluginNotFoundException )
            {
                Plugin plugin = ( (PluginNotFoundException) cause ).getPlugin();
                ArtifactNotFoundException artifactException = (ArtifactNotFoundException) ( (PluginNotFoundException) cause ).getCause();

                writer.write( NEWLINE );
                writer.write( "Maven cannot find a plugin required by your build:" );
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
                writer.write( NEWLINE );

                handleGenericException( artifactException, showStackTraces, writer );

                writer.write( NEWLINE );
                writer.write( NEWLINE );
                writer.write( "NOTE: If the above Group-Id or Artifact-Id are incorrect," );
                writer.write( NEWLINE );
                writer.write( "check that the corresponding <plugin/> section in your POM is correct." );
                writer.write( NEWLINE );
                writer.write( "If you specified this plugin directly using something like 'javadoc:javadoc'," );
                writer.write( NEWLINE );
                writer.write( "check that the <pluginGroups/> section in your $HOME/.m2/settings.xml contains the" );
                writer.write( NEWLINE );
                writer.write( "proper groupId for the plugin you are trying to use (each groupId goes in a separate" );
                writer.write( NEWLINE );
                writer.write( "<pluginGroup/> element within the <pluginGroups/> section." );
                writer.write( NEWLINE );

                result = true;
            }
            else if ( cause instanceof ProjectBuildingException )
            {
                result = handleProjectBuildingException( (ProjectBuildingException) cause, showStackTraces, writer );
            }
            else if ( cause instanceof ArtifactNotFoundException )
            {
                writer.write( NEWLINE );
                writer.write( "One or more project dependency artifacts are missing." );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
                writer.write( "Reason: " );
                writer.write( cause.getMessage() );
                writer.write( NEWLINE );

                result = true;
            }
            else if ( cause instanceof ArtifactNotFoundException )
            {
                writer.write( NEWLINE );
                writer.write( "Maven encountered an error while resolving one or more project dependency artifacts." );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
                writer.write( "Reason: " );
                writer.write( cause.getMessage() );
                writer.write( NEWLINE );

                result = true;
            }
            else if ( cause instanceof PluginVersionNotFoundException )
            {
                writer.write( NEWLINE );
                writer.write( "Cannot find a valid version for plugin: " );
                writer.write( NEWLINE );
                writer.write( "Group-Id: " );
                writer.write( ((PluginVersionNotFoundException)cause).getGroupId() );
                writer.write( NEWLINE );
                writer.write( "Artifact-Id: " );
                writer.write( ((PluginVersionNotFoundException)cause).getArtifactId() );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
                writer.write( "Reason: " );
                writer.write( cause.getMessage() );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
                writer.write( "Please ensure that your proxy information is specified correctly in $HOME/.m2/settings.xml." );
                writer.write( NEWLINE );

                result = true;
            }
            else if ( cause instanceof PluginVersionResolutionException )
            {
                writer.write( NEWLINE );
                writer.write( "Maven encountered an error while trying to resolve a valid version for plugin: " );
                writer.write( NEWLINE );
                writer.write( "Group-Id: " );
                writer.write( ((PluginVersionNotFoundException)cause).getGroupId() );
                writer.write( NEWLINE );
                writer.write( "Artifact-Id: " );
                writer.write( ((PluginVersionNotFoundException)cause).getArtifactId() );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
                writer.write( "Reason: " );
                writer.write( cause.getMessage() );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
                writer.write( "Please ensure that your proxy information is specified correctly in $HOME/.m2/settings.xml." );
                writer.write( NEWLINE );

                result = true;
            }
        }

        MavenProject project = e.getProject();

        writer.write( NEWLINE );
        writer.write( "While building project with id: " );
        writer.write( project.getId() );
        writer.write( NEWLINE );
        if ( project.getFile() != null )
        {
            writer.write( "Project File: " );
            writer.write( project.getFile().getAbsolutePath() );
        }
        writer.write( NEWLINE );

        return result;
    }

    private static boolean handleProjectBuildingException( ProjectBuildingException e,
                                                           boolean showStackTraces,
                                                           StringWriter writer )
    {
        // =====================================================================
        // Cases covered:
        // =====================================================================
        //
        // ProjectBuildingException(String, String, File, XmlPullParserException)
        //
        // ProjectBuildingException(String, String, IOException)
        // ProjectBuildingException(String, String, File, IOException)
        //
        // ProjectBuildingException(String, String, ArtifactNotFoundException)
        // ProjectBuildingException(String, String, File, ArtifactNotFoundException)
        //
        // ProjectBuildingException(String, String, ArtifactResolutionException)
        // ProjectBuildingException(String, String, File, ArtifactResolutionException)
        //
        // ProjectBuildingException(String, String, File, ProfileActivationException)
        //
        // ProjectBuildingException(String, String, InvalidRepositoryException)
        // ProjectBuildingException(String, String, File, InvalidRepositoryException)
        //
        // =====================================================================
        // Subclass cases:
        // =====================================================================
        //
        // InvalidProjectModelException(all)
        // InvalidProjectVersionException(all)
        // InvalidDependencyVersionException(all)

        boolean result = false;

        Throwable cause = e.getCause();

        // Start by looking at whether we can handle the PBE as a specific sub-class of ProjectBuildingException...
//        if ( e instanceof InvalidProjectModelException )
//        {
//            InvalidProjectModelException error = (InvalidProjectModelException) e;
//
//            writer.write( error.getMessage() );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//            writer.write( "The following POM validation errors were detected:" );
//            writer.write( NEWLINE );
//
//            for ( Iterator it = error.getValidationResult().getMessages().iterator(); it.hasNext(); )
//            {
//                String message = (String) it.next();
//                writer.write( NEWLINE );
//                writer.write( " - " );
//                writer.write( message );
//            }
//
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//
//            result = true;
//        }
        if ( false )
        {
        }
//        else if ( e instanceof InvalidDependencyVersionException )
//        {
//            writer.write( NEWLINE );
//            writer.write( "Your project declares a dependency with an invalid version." );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//
//            Dependency dep = ((InvalidDependencyVersionException)e).getDependency();
//            writer.write( "Dependency:" );
//            writer.write( NEWLINE );
//            writer.write( "Group-Id: " );
//            writer.write( dep.getGroupId() );
//            writer.write( NEWLINE );
//            writer.write( "Artifact-Id: " );
//            writer.write( dep.getArtifactId() );
//            writer.write( NEWLINE );
//            writer.write( "Version: " );
//            writer.write( dep.getVersion() );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//
//            writer.write( "Reason: " );
//            writer.write( cause.getMessage() );
//            writer.write( NEWLINE );
//
//            result = true;
//        }
        // InvalidDependencyVersionException extends from InvalidProjectVersionException, so it comes first.
//        else if ( e instanceof InvalidProjectVersionException )
//        {
//            writer.write( NEWLINE );
//            writer.write( "You have an invalid version in your POM:" );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//            writer.write( "Location: " );
//            writer.write( ((InvalidProjectVersionException)e).getLocationInPom() );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//            writer.write( "Reason: " );
//            writer.write( cause.getMessage() );
//            writer.write( NEWLINE );
//
//            result = true;
//        }
        // now that we've sorted through all the sub-classes of ProjectBuildingException,
        // let's look at causes of a basic PBE instance.
        else if ( ( cause instanceof ArtifactNotFoundException )
                  || ( cause instanceof ArtifactResolutionException ) )
        {
            writer.write( NEWLINE );
            writer.write( e.getMessage() );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Reason: " );
            writer.write( cause.getMessage() );
            writer.write( NEWLINE );

            result = true;
        }
        // handled by aspect binding to ProjectErrorReporter now.
//        else if ( cause instanceof ProfileActivationException )
//        {
//            writer.write( NEWLINE );
//            writer.write( "Profile activation failed. One or more named profile activators may be missing." );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//            writer.write( "Reason: " );
//            writer.write( cause.getMessage() );
//            writer.write( NEWLINE );
//
//            result = true;
//        }
        else if ( cause instanceof IOException )
        {
            writer.write( NEWLINE );
            if ( e.getPomFile() == null )
            {
                writer.write( "Error reading built-in super POM!" );
            }
            else
            {
                writer.write( "Error reading POM." );
            }
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( cause.getMessage() );
            writer.write( NEWLINE );
        }
        else if ( cause instanceof XmlPullParserException )
        {
            writer.write( NEWLINE );
            if ( e.getPomFile() == null )
            {
                writer.write( "Error parsing built-in super POM!" );
            }
            else
            {
                writer.write( "Error parsing POM." );
            }
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( cause.getMessage() );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Line: " );
            writer.write( "" + ( (XmlPullParserException) cause ).getLineNumber() );
            writer.write( NEWLINE );
            writer.write( "Column: " );
            writer.write( "" + ( (XmlPullParserException) cause ).getColumnNumber() );
            writer.write( NEWLINE );

            result = true;
        }
//        else if ( cause instanceof InvalidRepositoryException )
//        {
//            writer.write( NEWLINE );
//            writer.write( "You have an invalid repository/pluginRepository declaration in your POM:" );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//            writer.write( "Repository-Id: " );
//            writer.write( ((InvalidRepositoryException)cause).getRepositoryId() );
//            writer.write( NEWLINE );
//            writer.write( NEWLINE );
//            writer.write( "Reason: " );
//            writer.write( cause.getMessage() );
//            writer.write( NEWLINE );
//
//            result = true;
//        }

        writer.write( NEWLINE );
        writer.write( "Failing project's id: " );
        writer.write( e.getProjectId() );
        writer.write( NEWLINE );
        if ( e.getPomFile() == null )
        {
            writer.write( "Source: Super POM (implied root ancestor of all Maven POMs)" );
        }
        else
        {
            writer.write( "Project File: " );
            writer.write( e.getPomFile().getAbsolutePath() );
        }
        writer.write( NEWLINE );

        return result;
    }

    private static boolean handleBuildFailureException( BuildFailureException e,
                                                        boolean showStackTraces,
                                                        StringWriter writer )
    {
        // =====================================================================
        // Cases covered (listed exceptions extend BuildFailureException):
        // =====================================================================
        //
        // AggregatedBuildFailureException(String, MojoBinding, MojoFailureException)
        //
        // InvalidTaskException(TaskValidationResult, LifecycleLoaderException)
        // InvalidTaskException(TaskValidationResult, LifecycleSpecificationException)
        // InvalidTaskException(TaskValidationResult, PluginLoaderException)
        //
        // NoGoalsSpecifiedException(String)
        //
        // ProjectBuildFailureException(String, MojoBinding, MojoFailureException)
        //
        // ProjectCycleException(List, String, CycleDetectedException)

        if ( e instanceof NoGoalsSpecifiedException )
        {
            writer.write( NEWLINE );
            writer.write( "You have not specified any goals or lifecycle phases for Maven to execute." );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Please, either specify a goal or lifecycle phase on the command line" );
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
            writer.write( "For more information about which goals and phases are available, see the following:" );
            writer.write( NEWLINE );
            writer.write( "- Maven in 5 Minutes guide (http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)" );
            writer.write( NEWLINE );
            writer.write( "- Maven User's documentation (http://maven.apache.org/users/)" );
            writer.write( NEWLINE );
            writer.write( "- Maven Plugins page (http://maven.apache.org/plugins/)" );
            writer.write( NEWLINE );
            writer.write( "- CodeHaus Mojos Project page (http://mojo.codehaus.org/plugins.html)" );
            writer.write( NEWLINE );
            writer.write( NEWLINE );

            return true;
        }
        else if ( e instanceof AggregatedBuildFailureException )
        {
            writer.write( NEWLINE );
            writer.write( "Mojo (aggregator): " );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "    " );
            writer.write( MojoBindingUtils.toString( ( (AggregatedBuildFailureException) e ).getBinding() ) );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "FAILED while executing in directory:" );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "    " );
            writer.write( ( (AggregatedBuildFailureException) e ).getExecutionRootDirectory() );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Reason:" );
            writer.write( NEWLINE );
            writer.write( NEWLINE );

            handleMojoFailureException( ( (AggregatedBuildFailureException) e ).getMojoFailureException(),
                                        writer );

            return true;
        }
        else if ( e instanceof ProjectBuildFailureException )
        {
            writer.write( NEWLINE );
            writer.write( "Mojo: " );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "    " );
            writer.write( MojoBindingUtils.toString( ( (ProjectBuildFailureException) e ).getBinding() ) );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "FAILED for project: " );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "    " );
            writer.write( ( (ProjectBuildFailureException) e ).getProjectId() );
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Reason:" );
            writer.write( NEWLINE );
            writer.write( NEWLINE );

            handleMojoFailureException( ( (ProjectBuildFailureException) e ).getMojoFailureException(),
                                        writer );

            return true;
        }
        else if ( e instanceof InvalidTaskException )
        {
            String task = ( (InvalidTaskException) e ).getTask();
            Throwable cause = e.getCause();

            writer.write( NEWLINE );
            writer.write( "Invalid mojo or lifecycle phase: " );
            writer.write( task );
            writer.write( NEWLINE );
            writer.write( NEWLINE );

            if ( cause instanceof PluginLoaderException )
            {
                writer.write( "Failed to load plugin: " );
                writer.write( ( (PluginLoaderException) cause ).getPluginKey() );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
            }
            else
            {
                writer.write( "Error message was: " );
                writer.write( e.getMessage() );
                writer.write( NEWLINE );
                writer.write( NEWLINE );

            }

            if ( showStackTraces )
            {
                writer.write( "Original error:" );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
                handleGenericException( cause, showStackTraces, writer );
            }
            else
            {
                writer.write( "Original error message was: " );
                writer.write( cause.getMessage() );
                writer.write( NEWLINE );
                writer.write( NEWLINE );
            }

            return true;
        }
        else if ( e instanceof ProjectCycleException )
        {
            writer.write( NEWLINE );
            writer.write( "Maven has detected a cyclic relationship among a set of projects in the current build." );
            writer.write( NEWLINE );
            writer.write( "The projects involved are:" );
            writer.write( NEWLINE );
            writer.write( NEWLINE );

            List projects = ( (ProjectCycleException) e ).getProjects();
            for ( Iterator it = projects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();
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

            if ( showStackTraces )
            {
                writer.write( "Original error:" );
                writer.write( NEWLINE );
                writer.write( NEWLINE );

                handleGenericException( ( (ProjectCycleException) e ).getCause(),
                                        showStackTraces,
                                        writer );

                writer.write( NEWLINE );
                writer.write( NEWLINE );
            }

            return true;
        }

        return false;
    }

    private static boolean handleMojoFailureException( MojoFailureException error,
                                                       StringWriter writer )
    {
        String message = error.getLongMessage();
        if ( message == null )
        {
            message = error.getMessage();
        }

        writer.write( message );
        writer.write( NEWLINE );

        return true;
    }

    private static void logReactorSummary( ReactorManager rm,
                                           MavenEmbedderLogger logger )
    {
        if ( ( rm != null ) && rm.hasMultipleProjects() && rm.executedMultipleProjects() )
        {
            logger.info( "" );
            logger.info( "" );

            // -------------------------
            // Reactor Summary:
            // -------------------------
            // o project-name...........FAILED
            // o project2-name..........SKIPPED (dependency build failed or was skipped)
            // o project-3-name.........SUCCESS

            line( logger );
            logger.info( "Reactor Summary:" );
            line( logger );

            for ( Iterator it = rm.getSortedProjects().iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                if ( rm.hasBuildFailure( project ) )
                {
                    logReactorSummaryLine( project.getName(),
                                           "FAILED",
                                           rm.getBuildFailure( project ).getTime(),
                                           logger );
                }
                else if ( rm.isBlackListed( project ) )
                {
                    logReactorSummaryLine( project.getName(),
                                           "SKIPPED (dependency build failed or was skipped)",
                                           logger );
                }
                else if ( rm.hasBuildSuccess( project ) )
                {
                    logReactorSummaryLine( project.getName(),
                                           "SUCCESS",
                                           rm.getBuildSuccess( project ).getTime(),
                                           logger );
                }
                else
                {
                    logReactorSummaryLine( project.getName(), "NOT BUILT", logger );
                }
            }
            line( logger );
        }
    }

    private static void stats( Date start,
                               MavenEmbedderLogger logger )
    {
        Date finish = new Date();

        long time = finish.getTime() - start.getTime();

        logger.info( "Total time: " + formatTime( time ) );

        logger.info( "Finished at: " + finish );

        //noinspection CallToSystemGC
        System.gc();

        Runtime r = Runtime.getRuntime();

        logger.info( "Final Memory: " + ( r.totalMemory() - r.freeMemory() ) / MB + "M/"
                     + r.totalMemory() / MB + "M" );
    }

    private static void line( MavenEmbedderLogger logger )
    {
        logger.info( "------------------------------------------------------------------------" );
    }

    private static String formatTime( long ms )
    {
        long secs = ms / MS_PER_SEC;

        long min = secs / SEC_PER_MIN;

        secs = secs % SEC_PER_MIN;

        String msg = "";

        if ( min > 1 )
        {
            msg = min + " minutes ";
        }
        else if ( min == 1 )
        {
            msg = "1 minute ";
        }

        if ( secs > 1 )
        {
            msg += secs + " seconds";
        }
        else if ( secs == 1 )
        {
            msg += "1 second";
        }
        else if ( min == 0 )
        {
            msg += "< 1 second";
        }
        return msg;
    }

    private static void logReactorSummaryLine( String name,
                                               String status,
                                               MavenEmbedderLogger logger )
    {
        logReactorSummaryLine( name, status, -1, logger );
    }

    private static void logReactorSummaryLine( String name,
                                               String status,
                                               long time,
                                               MavenEmbedderLogger logger )
    {
        StringBuffer messageBuffer = new StringBuffer();

        messageBuffer.append( name );

        int dotCount = 54;

        dotCount -= name.length();

        messageBuffer.append( " " );

        for ( int i = 0; i < dotCount; i++ )
        {
            messageBuffer.append( '.' );
        }

        messageBuffer.append( " " );

        messageBuffer.append( status );

        if ( time >= 0 )
        {
            messageBuffer.append( " [" );

            messageBuffer.append( getFormattedTime( time ) );

            messageBuffer.append( "]" );
        }

        logger.info( messageBuffer.toString() );
    }

    private static String getFormattedTime( long time )
    {
        String pattern = "s.SSS's'";
        if ( time / 60000L > 0 )
        {
            pattern = "m:s" + pattern;
            if ( time / 3600000L > 0 )
            {
                pattern = "H:m" + pattern;
            }
        }
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.format( new Date( time ) );
    }
}
