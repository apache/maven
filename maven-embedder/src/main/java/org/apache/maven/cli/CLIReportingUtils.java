package org.apache.maven.cli;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.ApplicationInformation;
import org.apache.maven.execution.DefaultRuntimeInformation;
import org.codehaus.plexus.util.Os;

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

    static void showVersion()
    {
        ApplicationInformation ai =
            DefaultRuntimeInformation.getVersion( MavenCli.class.getClassLoader(), "org.apache.maven", "maven-core" );

        System.out.println( "Maven version: " + ai.getVersion() + " built on " + ai.getBuiltOn() );

        System.out.println( "Java version: " + System.getProperty( "java.version", "<unknown java version>" ) );

        System.out.println( "Java home: " + System.getProperty( "java.home", "<unknown java home>" ) );

        System.out.println( "Default locale: " + Locale.getDefault() + ", platform encoding: "
            + System.getProperty( "file.encoding", "<unknown encoding>" ) );

        System.out.println( "OS name: \"" + Os.OS_NAME + "\" version: \"" + Os.OS_VERSION + "\" arch: \"" + Os.OS_ARCH
            + "\" family: \"" + Os.OS_FAMILY + "\"" );
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
