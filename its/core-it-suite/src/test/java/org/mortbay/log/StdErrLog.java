package org.mortbay.log;

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

/**
 * A modified version of the original Jetty logger which treats info level the same as debug level. This is merely meant
 * as a cosmetic tweak to reduce the log noise from Jetty on the console during our tests.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class StdErrLog
    implements Logger
{

    private static boolean debug = System.getProperty( "DEBUG" ) != null;

    private String name;

    private boolean hideStacks;

    private PrintStream out = System.err;

    private static final String PREFIX = "[JETTY] ";

    public StdErrLog()
    {
        this( null );
    }

    public StdErrLog( String name )
    {
        this.name = ( name == null ) ? "" : name;
    }

    public boolean isDebugEnabled()
    {
        return debug;
    }

    public void setDebugEnabled( boolean enabled )
    {
        debug = enabled;
    }

    public boolean isHideStacks()
    {
        return hideStacks;
    }

    public void setHideStacks( boolean hideStacks )
    {
        this.hideStacks = hideStacks;
    }

    public void debug( String msg, Throwable th )
    {
        if ( debug )
        {
            this.out.println( PREFIX + msg );
            if ( th != null )
            {
                if ( hideStacks )
                    this.out.println( th );
                else
                    th.printStackTrace( this.out );
            }
        }
    }

    public void debug( String msg, Object arg0, Object arg1 )
    {
        if ( debug )
        {
            this.out.println( PREFIX + format( msg, arg0, arg1 ) );
        }
    }

    public void info( String msg, Object arg0, Object arg1 )
    {
        if ( debug )
        {
            this.out.println( PREFIX + format( msg, arg0, arg1 ) );
        }
    }

    public void warn( String msg, Object arg0, Object arg1 )
    {
        this.out.println( PREFIX + format( msg, arg0, arg1 ) );
    }

    public void warn( String msg, Throwable th )
    {
        this.out.println( PREFIX + msg );
        if ( th != null )
        {
            if ( this.hideStacks )
            {
                this.out.println( th );
            }
            else
            {
                th.printStackTrace( this.out );
            }
        }
    }

    private String format( String msg, Object arg0, Object arg1 )
    {
        int i0 = msg.indexOf( "{}" );
        int i1 = i0 < 0 ? -1 : msg.indexOf( "{}", i0 + 2 );

        if ( arg1 != null && i1 >= 0 )
        {
            msg = msg.substring( 0, i1 ) + arg1 + msg.substring( i1 + 2 );
        }
        if ( arg0 != null && i0 >= 0 )
        {
            msg = msg.substring( 0, i0 ) + arg0 + msg.substring( i0 + 2 );
        }
        return msg;
    }

    public Logger getLogger( String name )
    {
        if ( ( name == null && this.name == null ) || ( name != null && name.equals( this.name ) ) )
        {
            return this;
        }
        return new StdErrLog( name );
    }

    public String toString()
    {
        return "STDERR" + this.name;
    }

}
