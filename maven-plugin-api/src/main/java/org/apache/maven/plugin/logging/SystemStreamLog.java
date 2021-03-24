package org.apache.maven.plugin.logging;

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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Logger with "standard" output and error output stream.
 *
 * @author jdcasey
 *
 * @deprecated Use SLF4J directly
 */
@Deprecated
public class SystemStreamLog
    implements Log
{
    /**
     * @see org.apache.maven.plugin.logging.Log#debug(java.lang.CharSequence)
     */
    public void debug( CharSequence content )
    {
        print( "debug", content );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#debug(java.lang.CharSequence, java.lang.Throwable)
     */
    public void debug( CharSequence content, Throwable error )
    {
        print( "debug", content, error );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#debug(java.lang.Throwable)
     */
    public void debug( Throwable error )
    {
        print( "debug", error );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#info(java.lang.CharSequence)
     */
    public void info( CharSequence content )
    {
        print( "info", content );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#info(java.lang.CharSequence, java.lang.Throwable)
     */
    public void info( CharSequence content, Throwable error )
    {
        print( "info", content, error );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#info(java.lang.Throwable)
     */
    public void info( Throwable error )
    {
        print( "info", error );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#warn(java.lang.CharSequence)
     */
    public void warn( CharSequence content )
    {
        print( "warn", content );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#warn(java.lang.CharSequence, java.lang.Throwable)
     */
    public void warn( CharSequence content, Throwable error )
    {
        print( "warn", content, error );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#warn(java.lang.Throwable)
     */
    public void warn( Throwable error )
    {
        print( "warn", error );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#error(java.lang.CharSequence)
     */
    public void error( CharSequence content )
    {
        System.err.println( "[error] " + content.toString() );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#error(java.lang.CharSequence, java.lang.Throwable)
     */
    public void error( CharSequence content, Throwable error )
    {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        error.printStackTrace( pWriter );

        System.err.println( "[error] " + content.toString()
                            + System.lineSeparator() + System.lineSeparator() + sWriter.toString() );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#error(java.lang.Throwable)
     */
    public void error( Throwable error )
    {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        error.printStackTrace( pWriter );

        System.err.println( "[error] " + sWriter.toString() );
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#isDebugEnabled()
     */
    public boolean isDebugEnabled()
    {
        // TODO Not sure how best to set these for this implementation...
        return false;
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#isInfoEnabled()
     */
    public boolean isInfoEnabled()
    {
        return true;
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#isWarnEnabled()
     */
    public boolean isWarnEnabled()
    {
        return true;
    }

    /**
     * @see org.apache.maven.plugin.logging.Log#isErrorEnabled()
     */
    public boolean isErrorEnabled()
    {
        return true;
    }

    private void print( String prefix, CharSequence content )
    {
        System.out.println( "[" + prefix + "] " + content.toString() );
    }

    private void print( String prefix, Throwable error )
    {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        error.printStackTrace( pWriter );

        System.out.println( "[" + prefix + "] " + sWriter.toString() );
    }

    private void print( String prefix, CharSequence content, Throwable error )
    {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        error.printStackTrace( pWriter );

        System.out.println( "[" + prefix + "] " + content.toString()
                            + System.lineSeparator() + System.lineSeparator() + sWriter.toString() );
    }
}
