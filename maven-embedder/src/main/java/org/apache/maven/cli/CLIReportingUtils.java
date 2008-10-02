package org.apache.maven.cli;

import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.errors.CoreErrorReporter;
import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.execution.ApplicationInformation;
import org.apache.maven.execution.DefaultRuntimeInformation;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.util.Os;

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

    private static final String NEWLINE = System.getProperty( "line.separator" );

    private CLIReportingUtils()
    {
    }

    static void showVersion()
    {
        ApplicationInformation ai = DefaultRuntimeInformation.getVersion( MavenCli.class.getClassLoader(), "org.apache.maven", "maven-core" );

        System.out.println( "Maven version: " + ai.getVersion() + " built on " + ai.getBuiltOn() );
        System.out.println( "Java version: " + System.getProperty( "java.version", "<unknown java version>" ) );
        System.out.println( "Default locale: " + Locale.getDefault() + ", platform encoding: " + System.getProperty( "file.encoding", "<unknown encoding>" ) );
        System.out.println( "OS name: \"" + Os.OS_NAME + "\" version: \"" + Os.OS_VERSION + "\" arch: \"" + Os.OS_ARCH + "\" family: \"" + Os.OS_FAMILY + "\"" );
    }

    /**
     * Logs result of the executed build.
     * 
     * @param request - build parameters
     * @param result - result of build
     * @param logger - the logger to use
     */
    public static void logResult( MavenExecutionRequest request, MavenExecutionResult result, MavenEmbedderLogger logger )
    {
        ReactorManager reactorManager = result.getReactorManager();

        logReactorSummary( reactorManager, logger );

        boolean printSuccess = true;
        if ( result.hasExceptions() )
        {
            for ( Iterator i = result.getExceptions().iterator(); i.hasNext(); )
            {
                Exception e = (Exception) i.next();

                showError( e, request.isShowErrors(), request.getErrorReporter(), logger );
            }

            line( logger );
            if ( !request.isShowErrors() )
            {
                logger.info( "For more information, run with the -e flag" );
                line( logger );
            }

            if ( !ReactorManager.FAIL_NEVER.equals( request.getReactorFailureBehavior() ) )
            {
                logger.info( "BUILD FAILED" );

                line( logger );

                stats( request.getStartTime(), logger );

                line( logger );

                printSuccess = false;
            }
            else
            {
                logger.info( " + Ignoring build failures" );
            }
        }

        if ( printSuccess )
        {
            line( logger );

            logger.info( "BUILD SUCCESSFUL" );

            line( logger );

            stats( request.getStartTime(), logger );

            line( logger );
        }

        logger.close();
    }

    static void showError( String message, Exception e, boolean showErrors )
    {
        MavenEmbedderLogger logger = new MavenEmbedderConsoleLogger();

        showError( message, e, showErrors, new DefaultCoreErrorReporter(), logger );

        if ( !showErrors )
        {
            logger.info( "For more information, run with the -e flag" );
        }
    }

    private static void showError( Exception e, boolean show, CoreErrorReporter reporter, MavenEmbedderLogger logger )
    {
        showError( null, e, show, reporter, logger );
    }

    /**
     * Format the exception and output it through the logger.
     * 
     * @param message - error message
     * @param e - exception that was thrown
     * @param showStackTraces
     * @param logger
     */
    //mkleint: public because used in netbeans integration
    public static void showError( String message, Exception e, boolean showStackTraces, CoreErrorReporter reporter, MavenEmbedderLogger logger )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );

        if ( message != null )
        {
            writer.write( message );
            writer.write( NEWLINE );
        }

        buildErrorMessage( e, showStackTraces, reporter, writer );

        writer.write( NEWLINE );

        if ( showStackTraces )
        {
            writer.write( "Error stacktrace:" );
            writer.write( NEWLINE );
            e.printStackTrace( new PrintWriter( writer ) );

        }

        logger.error( writer.toString() );
    }

    public static void buildErrorMessage( Exception e, boolean showStackTraces, CoreErrorReporter reporter, StringWriter writer )
    {
        if ( reporter != null )
        {
            Throwable reported = reporter.findReportedException( e );

            if ( reported != null )
            {
                writer.write( reporter.getFormattedMessage( reported ) );

                if ( showStackTraces )
                {
                    writer.write( NEWLINE );
                    writer.write( NEWLINE );
                    Throwable cause = reporter.getRealCause( reported );
                    if ( cause != null )
                    {
                        cause.printStackTrace( new PrintWriter( writer ) );
                    }
                }

                writer.write( NEWLINE );
                writer.write( NEWLINE );

                return;
            }
        }

        boolean handled = false;

        if ( e instanceof ProjectBuildingException )
        {
            handled = handleProjectBuildingException( (ProjectBuildingException) e, showStackTraces, writer );
        }
        else if ( e instanceof LifecycleExecutionException )
        {
            handled = handleLifecycleExecutionException( (LifecycleExecutionException) e, showStackTraces, writer );
        }
        else if ( e instanceof MavenExecutionException )
        {
            handled = handleMavenExecutionException( (MavenExecutionException) e, showStackTraces, writer );
        }

        if ( !handled )
        {
            handleGenericException( e, showStackTraces, writer );
        }
    }

    private static boolean handleMavenExecutionException( MavenExecutionException e, boolean showStackTraces, StringWriter writer )
    {
        handleGenericException( e, showStackTraces, writer );

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

    private static void handleGenericException( Throwable exception, boolean showStackTraces, StringWriter writer )
    {
        writer.write( exception.getMessage() );
        writer.write( NEWLINE );
    }

    private static boolean handleLifecycleExecutionException( LifecycleExecutionException e, boolean showStackTraces, StringWriter writer )
    {
        handleGenericException( e, showStackTraces, writer );

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

        return true;
    }

    private static boolean handleProjectBuildingException( ProjectBuildingException e, boolean showStackTraces, StringWriter writer )
    {
        handleGenericException( e, showStackTraces, writer );

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

        return true;
    }

    private static void logReactorSummary( ReactorManager rm, MavenEmbedderLogger logger )
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
                    logReactorSummaryLine( project.getName(), "FAILED", rm.getBuildFailure( project ).getTime(), logger );
                }
                else if ( rm.isBlackListed( project ) )
                {
                    logReactorSummaryLine( project.getName(), "SKIPPED (dependency build failed or was skipped)", logger );
                }
                else if ( rm.hasBuildSuccess( project ) )
                {
                    logReactorSummaryLine( project.getName(), "SUCCESS", rm.getBuildSuccess( project ).getTime(), logger );
                }
                else
                {
                    logReactorSummaryLine( project.getName(), "NOT BUILT", logger );
                }
            }
            line( logger );
        }
    }

    private static void stats( Date start, MavenEmbedderLogger logger )
    {
        Date finish = new Date();

        long time = finish.getTime() - start.getTime();

        logger.info( "Total time: " + formatTime( time ) );

        logger.info( "Finished at: " + finish );

        //noinspection CallToSystemGC
        System.gc();

        Runtime r = Runtime.getRuntime();

        logger.info( "Final Memory: " + ( r.totalMemory() - r.freeMemory() ) / MB + "M/" + r.totalMemory() / MB + "M" );
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

    private static void logReactorSummaryLine( String name, String status, MavenEmbedderLogger logger )
    {
        logReactorSummaryLine( name, status, -1, logger );
    }

    private static void logReactorSummaryLine( String name, String status, long time, MavenEmbedderLogger logger )
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
