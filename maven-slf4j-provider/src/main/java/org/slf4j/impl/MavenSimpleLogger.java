package org.slf4j.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

import java.io.PrintStream;

/**
 * Logger for Maven, that support colorization of levels and stacktraces.
 * This class implements 2 methods introduced in slf4j-simple provider local copy.
 * @since 3.4.0
 */
public class MavenSimpleLogger
    extends SimpleLogger
{
    private static final String NEWLINE = System.getProperty( "line.separator" );

    MavenSimpleLogger( String name )
    {
        super( name );
    }

    @Override
    protected String renderLevel( int level )
    {
        switch ( level )
        {
            case LOG_LEVEL_TRACE:
                return buffer().debug( "TRACE" ).toString();
            case LOG_LEVEL_DEBUG:
                return buffer().debug( "DEBUG" ).toString();
            case LOG_LEVEL_INFO:
                return buffer().info( "INFO" ).toString();
            case LOG_LEVEL_WARN:
                return buffer().warning( "WARNING" ).toString();
            case LOG_LEVEL_ERROR:
            default:
                return buffer().error( "ERROR" ).toString();
        }
    }

    @Override
    protected void renderThrowable( Throwable t, PrintStream stream )
    {
        stream.append( buffer().failure( t.getClass().getName() ).toString() );
        if ( t.getMessage() != null )
        {
            stream.append( ": " );
            stream.append( buffer().failure( t.getMessage() ).toString() );
        }
        stream.append( NEWLINE );

        while ( t != null )
        {
            for ( StackTraceElement e : t.getStackTrace() )
            {
                stream.append( "    " );
                stream.append( buffer().strong( "at" ).toString() );
                stream.append( " " + e.getClassName() + "." + e.getMethodName() );
                stream.append( buffer().a( " (" ).strong( getLocation( e ) ).a( ")" ).toString() );
                stream.append( NEWLINE );
            }

            t = t.getCause();
            if ( t != null )
            {
                stream.append( buffer().strong( "Caused by" ).a( ": " ).a( t.getClass().getName() ).toString() );
                if ( t.getMessage() != null )
                {
                    stream.append( ": " );
                    stream.append( buffer().failure( t.getMessage() ).toString() );
                }
                stream.append( NEWLINE );
            }
        }
    }

    protected String getLocation( final StackTraceElement e )
    {
        assert e != null;

        if ( e.isNativeMethod() )
        {
            return "Native Method";
        }
        else if ( e.getFileName() == null )
        {
            return "Unknown Source";
        }
        else if ( e.getLineNumber() >= 0 )
        {
            return String.format( "%s:%s", e.getFileName(), e.getLineNumber() );
        }
        else
        {
            return e.getFileName();
        }
    }
}
