package org.apache.maven.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * A Slf4j logger bridged onto a Plexus logger.
 */
class PlexusLogger
    implements Logger
{

    private final org.codehaus.plexus.logging.Logger logger;

    public PlexusLogger( org.codehaus.plexus.logging.Logger logger )
    {
        this.logger = logger;
    }

    public String getName()
    {
        return logger.getName();
    }

    public boolean isTraceEnabled()
    {
        return isDebugEnabled();
    }

    public void trace( String msg )
    {
        debug( msg );
    }

    public void trace( String format, Object arg )
    {
        debug( format, arg );
    }

    public void trace( String format, Object arg1, Object arg2 )
    {
        debug( format, arg1, arg2 );
    }

    public void trace( String format, Object[] argArray )
    {
        debug( format, argArray );
    }

    public void trace( String msg, Throwable t )
    {
        debug( msg, t );
    }

    public boolean isTraceEnabled( Marker marker )
    {
        return isTraceEnabled();
    }

    public void trace( Marker marker, String msg )
    {
        trace( msg );
    }

    public void trace( Marker marker, String format, Object arg )
    {
        trace( format, arg );
    }

    public void trace( Marker marker, String format, Object arg1, Object arg2 )
    {
        trace( format, arg1, arg2 );
    }

    public void trace( Marker marker, String format, Object[] argArray )
    {
        trace( format, argArray );
    }

    public void trace( Marker marker, String msg, Throwable t )
    {
        trace( msg, t );
    }

    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    public void debug( String msg )
    {
        logger.debug( msg );
    }

    public void debug( String format, Object arg )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg );
        logger.debug( ft.getMessage(), ft.getThrowable() );
    }

    public void debug( String format, Object arg1, Object arg2 )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg1, arg2 );
        logger.debug( ft.getMessage(), ft.getThrowable() );
    }

    public void debug( String format, Object[] argArray )
    {
        FormattingTuple ft = MessageFormatter.arrayFormat( format, argArray );
        logger.debug( ft.getMessage(), ft.getThrowable() );
    }

    public void debug( String msg, Throwable t )
    {
        logger.debug( msg, t );
    }

    public boolean isDebugEnabled( Marker marker )
    {
        return isDebugEnabled();
    }

    public void debug( Marker marker, String msg )
    {
        debug( msg );
    }

    public void debug( Marker marker, String format, Object arg )
    {
        debug( format, arg );
    }

    public void debug( Marker marker, String format, Object arg1, Object arg2 )
    {
        debug( format, arg1, arg2 );
    }

    public void debug( Marker marker, String format, Object[] argArray )
    {
        debug( format, argArray );
    }

    public void debug( Marker marker, String msg, Throwable t )
    {
        debug( msg, t );
    }

    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    public void info( String msg )
    {
        logger.info( msg );
    }

    public void info( String format, Object arg )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg );
        logger.info( ft.getMessage(), ft.getThrowable() );
    }

    public void info( String format, Object arg1, Object arg2 )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg1, arg2 );
        logger.info( ft.getMessage(), ft.getThrowable() );
    }

    public void info( String format, Object[] argArray )
    {
        FormattingTuple ft = MessageFormatter.arrayFormat( format, argArray );
        logger.info( ft.getMessage(), ft.getThrowable() );
    }

    public void info( String msg, Throwable t )
    {
        logger.info( msg, t );
    }

    public boolean isInfoEnabled( Marker marker )
    {
        return isInfoEnabled();
    }

    public void info( Marker marker, String msg )
    {
        info( msg );
    }

    public void info( Marker marker, String format, Object arg )
    {
        info( format, arg );
    }

    public void info( Marker marker, String format, Object arg1, Object arg2 )
    {
        info( format, arg1, arg2 );
    }

    public void info( Marker marker, String format, Object[] argArray )
    {
        info( format, argArray );
    }

    public void info( Marker marker, String msg, Throwable t )
    {
        info( msg, t );
    }

    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    public void warn( String msg )
    {
        logger.warn( msg );
    }

    public void warn( String format, Object arg )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg );
        logger.warn( ft.getMessage(), ft.getThrowable() );
    }

    public void warn( String format, Object arg1, Object arg2 )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg1, arg2 );
        logger.warn( ft.getMessage(), ft.getThrowable() );
    }

    public void warn( String format, Object[] argArray )
    {
        FormattingTuple ft = MessageFormatter.arrayFormat( format, argArray );
        logger.warn( ft.getMessage(), ft.getThrowable() );
    }

    public void warn( String msg, Throwable t )
    {
        logger.warn( msg, t );
    }

    public boolean isWarnEnabled( Marker marker )
    {
        return isWarnEnabled();
    }

    public void warn( Marker marker, String msg )
    {
        warn( msg );
    }

    public void warn( Marker marker, String format, Object arg )
    {
        warn( format, arg );
    }

    public void warn( Marker marker, String format, Object arg1, Object arg2 )
    {
        warn( format, arg1, arg2 );
    }

    public void warn( Marker marker, String format, Object[] argArray )
    {
        warn( format, argArray );
    }

    public void warn( Marker marker, String msg, Throwable t )
    {
        warn( msg, t );
    }

    public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

    public void error( String msg )
    {
        logger.error( msg );
    }

    public void error( String format, Object arg )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg );
        logger.error( ft.getMessage(), ft.getThrowable() );
    }

    public void error( String format, Object arg1, Object arg2 )
    {
        FormattingTuple ft = MessageFormatter.format( format, arg1, arg2 );
        logger.error( ft.getMessage(), ft.getThrowable() );
    }

    public void error( String format, Object[] argArray )
    {
        FormattingTuple ft = MessageFormatter.arrayFormat( format, argArray );
        logger.error( ft.getMessage(), ft.getThrowable() );
    }

    public void error( String msg, Throwable t )
    {
        logger.error( msg, t );
    }

    public boolean isErrorEnabled( Marker marker )
    {
        return isErrorEnabled();
    }

    public void error( Marker marker, String msg )
    {
        error( msg );
    }

    public void error( Marker marker, String format, Object arg )
    {
        error( format, arg );
    }

    public void error( Marker marker, String format, Object arg1, Object arg2 )
    {
        error( format, arg1, arg2 );
    }

    public void error( Marker marker, String format, Object[] argArray )
    {
        error( format, argArray );
    }

    public void error( Marker marker, String msg, Throwable t )
    {
        error( msg, t );
    }

}
