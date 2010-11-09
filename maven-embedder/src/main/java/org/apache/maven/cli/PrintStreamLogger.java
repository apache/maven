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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintStream;

import org.apache.maven.Maven;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

/**
 * Logs to a user-supplied {@link PrintStream}.
 * 
 * @author Benjamin Bentmann
 */
public class PrintStreamLogger
    extends AbstractLogger
{

    static interface Provider
    {
        PrintStream getStream();
    }

    private Provider provider;

    private static final String FATAL_ERROR = "[FATAL] ";

    private static final String ERROR = "[ERROR] ";

    private static final String WARNING = "[WARNING] ";

    private static final String INFO = "[INFO] ";

    private static final String DEBUG = "[DEBUG] ";

    public PrintStreamLogger( Provider provider )
    {
        super( Logger.LEVEL_INFO, Maven.class.getName() );

        if ( provider == null )
        {
            throw new IllegalArgumentException( "output stream provider missing" );
        }
        this.provider = provider;
    }

    public PrintStreamLogger( PrintStream out )
    {
        super( Logger.LEVEL_INFO, Maven.class.getName() );

        setStream( out );
    }

    public void setStream( final PrintStream out )
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "output stream missing" );
        }

        this.provider = new Provider()
        {
            public PrintStream getStream()
            {
                return out;
            }
        };
    }

    public void debug( String message, Throwable throwable )
    {
        if ( isDebugEnabled() )
        {
            PrintStream out = provider.getStream();

            out.print( DEBUG );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void info( String message, Throwable throwable )
    {
        if ( isInfoEnabled() )
        {
            PrintStream out = provider.getStream();

            out.print( INFO );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void warn( String message, Throwable throwable )
    {
        if ( isWarnEnabled() )
        {
            PrintStream out = provider.getStream();

            out.print( WARNING );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void error( String message, Throwable throwable )
    {
        if ( isErrorEnabled() )
        {
            PrintStream out = provider.getStream();

            out.print( ERROR );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void fatalError( String message, Throwable throwable )
    {
        if ( isFatalErrorEnabled() )
        {
            PrintStream out = provider.getStream();

            out.print( FATAL_ERROR );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void close()
    {
        PrintStream out = provider.getStream();

        if ( out == System.out || out == System.err )
        {
            out.flush();
        }
        else
        {
            out.close();
        }
    }

    public Logger getChildLogger( String arg0 )
    {
        return this;
    }

}
