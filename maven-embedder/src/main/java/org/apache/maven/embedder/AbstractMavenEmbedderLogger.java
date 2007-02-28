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

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public abstract class AbstractMavenEmbedderLogger
    implements MavenEmbedderLogger
{
    private int threshold = MavenEmbedderLogger.LEVEL_INFO;

    private String name;

    public int getThreshold()
    {
        return threshold;
    }

    public void setThreshold( int threshold )
    {
        this.threshold = threshold;
    }

    public String getName()
    {
        return name;
    }

    public void debug( String message )
    {
        debug( message, null );
    }

    public boolean isDebugEnabled()
    {
        return threshold <= LEVEL_DEBUG;
    }

    public void info( String message )
    {
        info( message, null );
    }

    public boolean isInfoEnabled()
    {
        return threshold <= LEVEL_INFO;
    }

    public void warn( String message )
    {
        warn( message, null );
    }

    public boolean isWarnEnabled()
    {
        return threshold <= LEVEL_WARN;
    }

    public void error( String message )
    {
        error( message, null );
    }

    public boolean isErrorEnabled()
    {
        return threshold <= LEVEL_ERROR;
    }

    public void fatalError( String message )
    {
        fatalError( message, null );
    }

    public boolean isFatalErrorEnabled()
    {
        return threshold <= LEVEL_FATAL;
    }

    protected boolean isValidThreshold( int threshold )
    {
        if ( threshold == LEVEL_DEBUG )
        {
            return true;
        }
        if ( threshold == LEVEL_INFO )
        {
            return true;
        }
        if ( threshold == LEVEL_WARN )
        {
            return true;
        }
        if ( threshold == LEVEL_ERROR )
        {
            return true;
        }
        if ( threshold == LEVEL_FATAL )
        {
            return true;
        }
        if ( threshold == LEVEL_DISABLED )
        {
            return true;
        }

        return false;
    }
}
