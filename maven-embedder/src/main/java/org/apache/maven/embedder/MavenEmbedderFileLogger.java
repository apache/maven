package org.apache.maven.embedder;

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

import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;

/**
 * @author Jason van Zyl
 */
public final class MavenEmbedderFileLogger
    extends AbstractMavenEmbedderLogger
{
    private PrintWriter log;

    public MavenEmbedderFileLogger( File logFile )
    {
        try
        {
            this.log = new PrintWriter( logFile );
        }
        catch ( FileNotFoundException e )
        {
            // The client must make sure the file is valid.
        }
    }

    public void debug( String message,
                       Throwable throwable )
    {
        if ( isDebugEnabled() )
        {
            print( "[DEBUG] " );
            println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( System.out );
            }
        }
    }

    public void info( String message,
                      Throwable throwable )
    {
        if ( isInfoEnabled() )
        {
            print( "[INFO] " );
            println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( System.out );
            }
        }
    }

    public void warn( String message,
                      Throwable throwable )
    {
        if ( isWarnEnabled() )
        {
            print( "[WARNING] " );
            println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( System.out );
            }
        }
    }

    public void error( String message,
                       Throwable throwable )
    {
        if ( isErrorEnabled() )
        {
            print( "[ERROR] " );
            println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( System.out );
            }
        }
    }

    public void fatalError( String message,
                            Throwable throwable )
    {
        if ( isFatalErrorEnabled() )
        {
            print( "[ERROR] " );
            println( message );

            if ( null != throwable )
            {
                throwable.printStackTrace( System.out );
            }
        }
    }

    protected void print( String message )
    {
        log.print( message );
    }

    protected void println( String message )
    {
        log.println( message );
    }

    public void close()
    {
        log.flush();

        log.close();
    }
}