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
import org.codehaus.plexus.logging.Logger;

/**
 * Logs to a user-supplied {@link PrintStream}.
 * 
 * @author Benjamin Bentmann
 */
public class PrintStreamLogger
    implements Logger
{
    private final PrintStream out;

    private static final String FATAL_ERROR = "[FATAL] ";

    private static final String ERROR = "[ERROR] ";

    private static final String WARNING = "[WARNING] ";

    private static final String INFO = "[INFO] ";

    private static final String DEBUG = "[DEBUG] ";

    public PrintStreamLogger( PrintStream out )
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "output stream missing" );
        }

        this.out = out;
    }

    public void debug( String message )
    {
        debug( message, null );
    }
    
    public void debug( String message, Throwable throwable )
    {
        if ( isDebugEnabled() )
        {
            out.print( DEBUG );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void info( String message )
    {
        info( message, null );
    }

    public void info( String message, Throwable throwable )
    {
        if ( isInfoEnabled() )
        {
            out.print( INFO );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void warn( String message )
    {
        warn( message, null );
    }
    
    public void warn( String message, Throwable throwable )
    {
        if ( isWarnEnabled() )
        {
            out.print( WARNING );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void error( String message )
    {
        error( message, null );
    }
    
    public void error( String message, Throwable throwable )
    {
        if ( isErrorEnabled() )
        {
            out.print( ERROR );
            out.println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public void fatalError( String message )
    {
        fatalError( message, null );
    }
    
    public void fatalError( String message, Throwable throwable )
    {
        if ( isFatalErrorEnabled() )
        {
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

    public String getName()
    {
        return Maven.class.getName();
    }

    public int getThreshold()
    {
        return 0;
    }

    public boolean isDebugEnabled()
    {
        return false;
    }

    public boolean isErrorEnabled()
    {
        return false;
    }

    public boolean isFatalErrorEnabled()
    {
        return false;
    }

    public boolean isInfoEnabled()
    {
        return false;
    }

    public boolean isWarnEnabled()
    {
        return false;
    }

    public void setThreshold( int arg0 )
    {
    }
}
