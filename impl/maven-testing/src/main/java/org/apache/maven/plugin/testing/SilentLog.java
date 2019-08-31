package org.apache.maven.plugin.testing;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

/**
 * This logger implements both types of logs currently in use. It can be injected where needed
 * to turn off logs during testing where they aren't desired.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class SilentLog
    implements Log, Logger
{
    /**
     * @return <code>false</code>
     * @see org.apache.maven.plugin.logging.Log#isDebugEnabled()
     */
    @Override
    public boolean isDebugEnabled()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#debug(java.lang.CharSequence)
     */
    @Override
    public void debug( CharSequence content )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#debug(java.lang.CharSequence, java.lang.Throwable)
     */
    @Override
    public void debug( CharSequence content, Throwable error )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#debug(java.lang.Throwable)
     */
    @Override
    public void debug( Throwable error )
    {
        // nop
    }

    /**
     * @return <code>false</code>
     * @see org.apache.maven.plugin.logging.Log#isInfoEnabled()
     */
    @Override
    public boolean isInfoEnabled()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#info(java.lang.CharSequence)
     */
    @Override
    public void info( CharSequence content )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#info(java.lang.CharSequence, java.lang.Throwable)
     */
    @Override
    public void info( CharSequence content, Throwable error )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#info(java.lang.Throwable)
     */
    @Override
    public void info( Throwable error )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#isWarnEnabled()
     */
    @Override
    public boolean isWarnEnabled()
    {
        // nop
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#warn(java.lang.CharSequence)
     */
    @Override
    public void warn( CharSequence content )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#warn(java.lang.CharSequence, java.lang.Throwable)
     */
    @Override
    public void warn( CharSequence content, Throwable error )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#warn(java.lang.Throwable)
     */
    @Override
    public void warn( Throwable error )
    {
        // nop
    }

    /**
     * @return <code>false</code>
     * @see org.apache.maven.plugin.logging.Log#isErrorEnabled()
     */
    @Override
    public boolean isErrorEnabled()
    {
        return false;
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#error(java.lang.CharSequence)
     */
    @Override
    public void error( CharSequence content )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#error(java.lang.CharSequence, java.lang.Throwable)
     */
    @Override
    public void error( CharSequence content, Throwable error )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.apache.maven.plugin.logging.Log#error(java.lang.Throwable)
     */
    @Override
    public void error( Throwable error )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#debug(java.lang.String)
     */
    @Override
    public void debug( String message )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#debug(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void debug( String message, Throwable throwable )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#info(java.lang.String)
     */
    @Override
    public void info( String message )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#info(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void info( String message, Throwable throwable )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#warn(java.lang.String)
     */
    @Override
    public void warn( String message )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#warn(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void warn( String message, Throwable throwable )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#error(java.lang.String)
     */
    @Override
    public void error( String message )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#error(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error( String message, Throwable throwable )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#fatalError(java.lang.String)
     */
    @Override
    public void fatalError( String message )
    {
        // nop
    }

    /**
     * By default, do nothing.
     *
     * @see org.codehaus.plexus.logging.Logger#fatalError(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void fatalError( String message, Throwable throwable )
    {
        // nop
    }

    /**
     * @return <code>false</code>
     * @see org.codehaus.plexus.logging.Logger#isFatalErrorEnabled()
     */
    @Override
    public boolean isFatalErrorEnabled()
    {
        return false;
    }

    /**
     * @return <code>null</code>
     * @see org.codehaus.plexus.logging.Logger#getChildLogger(java.lang.String)
     */
    @Override
    public Logger getChildLogger( String name )
    {
        return null;
    }

    /**
     * @return <code>0</code>
     * @see org.codehaus.plexus.logging.Logger#getThreshold()
     */
    @Override
    public int getThreshold()
    {
        return 0;
    }

    /**
     * @return <code>null</code>
     * @see org.codehaus.plexus.logging.Logger#getName()
     */
    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public void setThreshold( int threshold )
    {
        // TODO Auto-generated method stub
        
    }
}
