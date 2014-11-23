package org.apache.maven.cli.logging;

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

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * @since 3.1.0
 */
public class Slf4jStdoutLogger
    implements Logger
{
    private static final String ERROR = "[ERROR] ";

    private PrintStream out = System.out;

    //
    // These are the only methods we need in our primordial logger
    //
    public void error( String msg )
    {
        out.print( ERROR );
        out.println( msg );
    }

    public void error( String msg, Throwable t )
    {
        error( msg );

        if ( null != t )
        {
            t.printStackTrace( out );
        }
    }

    //
    // Don't need any of this
    //
    public String getName()
    {
        return null;
    }

    public boolean isTraceEnabled()
    {
        return false;
    }

    public void trace( String msg )
    {
    }

    public void trace( String format, Object arg )
    {
    }

    public void trace( String format, Object arg1, Object arg2 )
    {
    }

    public void trace( String format, Object... arguments )
    {
    }

    public void trace( String msg, Throwable t )
    {
    }

    public boolean isTraceEnabled( Marker marker )
    {
        return false;
    }

    public void trace( Marker marker, String msg )
    {
    }

    public void trace( Marker marker, String format, Object arg )
    {
    }

    public void trace( Marker marker, String format, Object arg1, Object arg2 )
    {
    }

    public void trace( Marker marker, String format, Object... argArray )
    {
    }

    public void trace( Marker marker, String msg, Throwable t )
    {
    }

    public boolean isDebugEnabled()
    {
        return false;
    }

    public void debug( String msg )
    {
    }

    public void debug( String format, Object arg )
    {
    }

    public void debug( String format, Object arg1, Object arg2 )
    {
    }

    public void debug( String format, Object... arguments )
    {
    }

    public void debug( String msg, Throwable t )
    {
    }

    public boolean isDebugEnabled( Marker marker )
    {
        return false;
    }

    public void debug( Marker marker, String msg )
    {
    }

    public void debug( Marker marker, String format, Object arg )
    {
    }

    public void debug( Marker marker, String format, Object arg1, Object arg2 )
    {
    }

    public void debug( Marker marker, String format, Object... arguments )
    {
    }

    public void debug( Marker marker, String msg, Throwable t )
    {
    }

    public boolean isInfoEnabled()
    {
        return false;
    }

    public void info( String msg )
    {
    }

    public void info( String format, Object arg )
    {
    }

    public void info( String format, Object arg1, Object arg2 )
    {
    }

    public void info( String format, Object... arguments )
    {
    }

    public void info( String msg, Throwable t )
    {
    }

    public boolean isInfoEnabled( Marker marker )
    {
        return false;
    }

    public void info( Marker marker, String msg )
    {
    }

    public void info( Marker marker, String format, Object arg )
    {
    }

    public void info( Marker marker, String format, Object arg1, Object arg2 )
    {
    }

    public void info( Marker marker, String format, Object... arguments )
    {
    }

    public void info( Marker marker, String msg, Throwable t )
    {
    }

    public boolean isWarnEnabled()
    {
        return false;
    }

    public void warn( String msg )
    {
    }

    public void warn( String format, Object arg )
    {
    }

    public void warn( String format, Object... arguments )
    {
    }

    public void warn( String format, Object arg1, Object arg2 )
    {
    }

    public void warn( String msg, Throwable t )
    {
    }

    public boolean isWarnEnabled( Marker marker )
    {
        return false;
    }

    public void warn( Marker marker, String msg )
    {
    }

    public void warn( Marker marker, String format, Object arg )
    {
    }

    public void warn( Marker marker, String format, Object arg1, Object arg2 )
    {
    }

    public void warn( Marker marker, String format, Object... arguments )
    {
    }

    public void warn( Marker marker, String msg, Throwable t )
    {
    }

    public boolean isErrorEnabled()
    {
        return false;
    }

    public void error( String format, Object arg )
    {
    }

    public void error( String format, Object arg1, Object arg2 )
    {
    }

    public void error( String format, Object... arguments )
    {
    }

    public boolean isErrorEnabled( Marker marker )
    {
        return false;
    }

    public void error( Marker marker, String msg )
    {
    }

    public void error( Marker marker, String format, Object arg )
    {
    }

    public void error( Marker marker, String format, Object arg1, Object arg2 )
    {
    }

    public void error( Marker marker, String format, Object... arguments )
    {
    }

    public void error( Marker marker, String msg, Throwable t )
    {
    }

}
