package org.apache.maven.monitor.logging;

/**
 * @author jdcasey
 */
public interface Log
{

    void debug( CharSequence content );

    void debug( CharSequence content, Throwable error );

    void debug( Throwable error );

    void info( CharSequence content );

    void info( CharSequence content, Throwable error );

    void info( Throwable error );

    void warn( CharSequence content );

    void warn( CharSequence content, Throwable error );

    void warn( Throwable error );

    void error( CharSequence content );

    void error( CharSequence content, Throwable error );

    void error( Throwable error );

}