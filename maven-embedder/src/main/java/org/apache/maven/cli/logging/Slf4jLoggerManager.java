package org.apache.maven.cli.logging;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Use an SLF4J {@link org.slf4j.ILoggerFactory} as a backing for a Plexus {@link org.codehaus.plexus.logging.LoggerManager}.
 *
 * @author Jason van Zyl
 * @since 3.1
 */
public class Slf4jLoggerManager
    implements LoggerManager
{

    private ILoggerFactory loggerFactory;

    public Slf4jLoggerManager()
    {
        loggerFactory = LoggerFactory.getILoggerFactory();
    }

    public Logger getLoggerForComponent( String role )
    {
        return new Slf4jLogger( loggerFactory.getLogger( role ) );
    }

    public Logger getLoggerForComponent( String role, String hint )
    {
        return ( null == hint
            ? getLoggerForComponent( role )
            : new Slf4jLogger( loggerFactory.getLogger( role + "." + hint ) ) );
    }

    //
    // Trying to give loggers back is a bad idea. Ceki said so :-)
    //
    public void returnComponentLogger( String role )
    {
    }

    public void returnComponentLogger( String role, String hint )
    {
    }

    public int getThreshold()
    {
        return 0;
    }

    public void setThreshold( int threshold )
    {
    }

    public void setThresholds( int threshold )
    {
    }

    public int getActiveLoggerCount()
    {
        return 0;
    }

}
