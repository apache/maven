package org.apache.maven.monitor.logging;

import org.codehaus.plexus.logging.Logger;

/**
 * @author jdcasey
 */
public class DefaultLog
    implements Log
{

    private final Logger logger;

    public DefaultLog( Logger logger )
    {
        this.logger = logger;
    }

    public void debug( CharSequence content )
    {
        logger.debug( content.toString() );
    }

    public void debug( CharSequence content, Throwable error )
    {
        logger.debug( content.toString(), error );
    }

    public void debug( Throwable error )
    {
        logger.debug( "", error );
    }

    public void info( CharSequence content )
    {
        logger.info( content.toString() );
    }

    public void info( CharSequence content, Throwable error )
    {
        logger.info( content.toString(), error );
    }

    public void info( Throwable error )
    {
        logger.info( "", error );
    }

    public void warn( CharSequence content )
    {
        logger.warn( content.toString() );
    }

    public void warn( CharSequence content, Throwable error )
    {
        logger.warn( content.toString(), error );
    }

    public void warn( Throwable error )
    {
        logger.warn( "", error );
    }

    public void error( CharSequence content )
    {
        logger.error( content.toString() );
    }

    public void error( CharSequence content, Throwable error )
    {
        logger.error( content.toString(), error );
    }

    public void error( Throwable error )
    {
        logger.error( "", error );
    }

}