package org.apache.maven.cli;

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.InvalidProjectModelException;
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
                                             .getResourceAsStream(
                                                                   "META-INF/maven/org.apache.maven/maven-core/pom.properties" );
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

    static void logResult( MavenExecutionRequest request,
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

    static void showError( Exception e, boolean show, MavenEmbedderLogger logger )
    {
        showError( null, e, show, logger );
    }

    static void showError( String message, Exception e,
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

    private static void buildErrorMessage( Exception e, boolean showStackTraces, StringWriter writer )
    {
        boolean handled = false;

        if ( e instanceof BuildFailureException )
        {
            handled = handleBuildFailureException( (BuildFailureException) e, writer );
        }
        else if ( e instanceof ProjectBuildingException )
        {
            handled = handleProjectBuildingException( (ProjectBuildingException) e, writer );
        }
        else if ( e instanceof LifecycleExecutionException )
        {
            handled = handleLifecycleExecutionException( (LifecycleExecutionException) e, showStackTraces, writer );
        }
        else if ( e instanceof DuplicateProjectException )
        {
            handled = handleDuplicateProjectException( (DuplicateProjectException) e, writer );
        }
        else if ( e instanceof MavenExecutionException )
        {
            handled = handleMavenExecutionException( (MavenExecutionException) e, writer );
        }


        if ( !handled )
        {
            handleGenericException( e, writer );
        }
    }

    private static boolean handleMavenExecutionException( MavenExecutionException e,
                                                          StringWriter writer )
    {
        writer.write( "While scanning for build extensions:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        Throwable cause = e.getCause();
        if ( cause != null )
        {
            if ( cause instanceof IOException )
            {
                writer.write( e.getMessage() );
                writer.write( NEWLINE );

                handleGenericException( cause, writer );

                return true;
            }
            else if ( cause instanceof ExtensionScanningException )
            {
                Throwable nestedCause = cause.getCause();
                if ( ( nestedCause != null ) && ( nestedCause instanceof ProjectBuildingException ) )
                {
                    return handleProjectBuildingException( (ProjectBuildingException) nestedCause, writer );
                }
                else
                {
                    handleGenericException( cause, writer );

                    return true;
                }
            }
            else if ( cause instanceof ProjectBuildingException )
            {
                return handleProjectBuildingException( (ProjectBuildingException) cause, writer );
            }
        }

        return false;
    }

    private static boolean handleDuplicateProjectException( DuplicateProjectException e,
                                                            StringWriter writer )
    {
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

    private static void handleGenericException( Throwable exception, StringWriter writer )
    {
        writer.write( exception.getMessage() );
        writer.write( NEWLINE );
    }

    private static boolean handleLifecycleExecutionException( LifecycleExecutionException e,
                                                              boolean showStackTraces, StringWriter writer )
    {
        Throwable cause = e.getCause();
        if ( cause != null )
        {
            if ( cause instanceof PluginNotFoundException )
            {
//                Plugin plugin = ( (PluginNotFoundException) cause ).getPlugin();

                ArtifactNotFoundException artifactException = (ArtifactNotFoundException) ( (PluginNotFoundException) cause ).getCause();

                writer.write( NEWLINE );
                writer.write( NEWLINE );

                handleGenericException( artifactException, writer );

                return true;
            }
        }

        return false;
    }

    private static boolean handleProjectBuildingException( ProjectBuildingException e,
                                                           StringWriter writer )
    {
        boolean result = false;

        Throwable cause = e.getCause();
        if ( cause instanceof XmlPullParserException )
        {
            writer.write( "Error parsing POM:" );
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

            result = true;
        }
        else if ( e instanceof InvalidProjectModelException )
        {
            InvalidProjectModelException error = (InvalidProjectModelException) e;
            writer.write( error.getMessage() );
            writer.write( NEWLINE );
            writer.write( "The following POM validation errors occurred:" );
            writer.write( NEWLINE );

            for ( Iterator it = error.getValidationResult().getMessages().iterator(); it.hasNext(); )
            {
                String message = (String) it.next();
                writer.write( NEWLINE );
                writer.write( " - " );
                writer.write( message );
            }

            writer.write( NEWLINE );
            writer.write( NEWLINE );

            result = true;
        }

        writer.write( NEWLINE );
        writer.write( "Project Id: " );
        writer.write( e.getProjectId() );
        writer.write( NEWLINE );
        writer.write( "Project File: " );
        writer.write( e.getPomLocation() );
        writer.write( NEWLINE );

        return result;
    }

    private static boolean handleBuildFailureException( BuildFailureException e, StringWriter writer )
    {
        Throwable cause = e.getCause();
        if ( ( cause != null ) && ( cause instanceof MojoFailureException ) )
        {
            writer.write( ( (AbstractMojoExecutionException) cause ).getLongMessage() );
            writer.write( NEWLINE );
            return true;
        }

        return false;
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
                    logReactorSummaryLine( project.getName(), "FAILED",
                                           rm.getBuildFailure( project ).getTime(), logger );
                }
                else if ( rm.isBlackListed( project ) )
                {
                    logReactorSummaryLine( project.getName(),
                                           "SKIPPED (dependency build failed or was skipped)",
                                           logger );
                }
                else if ( rm.hasBuildSuccess( project ) )
                {
                    logReactorSummaryLine( project.getName(), "SUCCESS",
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
