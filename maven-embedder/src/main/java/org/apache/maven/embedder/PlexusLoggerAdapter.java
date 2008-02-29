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

import org.codehaus.plexus.logging.Logger;/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class PlexusLoggerAdapter
    implements Logger
{
    private MavenEmbedderLogger logger;

    public PlexusLoggerAdapter( MavenEmbedderLogger logger )
    {
        this.logger = logger;
    }

    public void debug( String message )
    {
        logger.debug( message );
    }

    public void debug( String message,
                       Throwable throwable )
    {
        logger.debug( message, throwable );
    }

    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    public void info( String message )
    {
        logger.info( message );
    }

    public void info( String message,
                      Throwable throwable )
    {
        logger.info( message, throwable );
    }

    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    public void warn( String message )
    {
        logger.warn( message );
    }

    public void warn( String message,
                      Throwable throwable )
    {
        logger.warn( message, throwable );
    }

    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    public void error( String message )
    {
        logger.error( message );
    }

    public void error( String message,
                       Throwable throwable )
    {
        logger.error( message, throwable );
    }

    public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

    public void fatalError( String message )
    {
        logger.fatalError( message );
    }

    public void fatalError( String message,
                            Throwable throwable )
    {
        logger.fatalError( message, throwable );
    }

    public boolean isFatalErrorEnabled()
    {
        return logger.isFatalErrorEnabled();
    }

    public void setThreshold( int i )
    {
        logger.setThreshold( i );
    }

    public int getThreshold()
    {
        return logger.getThreshold();
    }

    public String getName()
    {
        return toString();
    }

    public Logger getChildLogger( String name )
    {
        return this;
    }
}
