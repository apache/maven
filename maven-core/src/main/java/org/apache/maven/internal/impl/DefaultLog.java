package org.apache.maven.internal.impl;

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

import java.util.function.Supplier;

import org.apache.maven.api.plugin.Log;
import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;

public class DefaultLog implements Log
{
    private final Logger logger;

    public DefaultLog( Logger logger )
    {
        this.logger = requireNonNull( logger );
    }

    public void debug( CharSequence content )
    {
        logger.debug( toString( content ) );
    }

    @Override
    public void debug( CharSequence content, Throwable error )
    {
        logger.debug( toString( content ), error );
    }

    @Override
    public void debug( Throwable error )
    {
        logger.debug( "", error );
    }

    @Override
    public void debug( Supplier<String> content )
    {
        if ( isDebugEnabled() )
        {
            logger.debug( content.get() );
        }
    }

    @Override
    public void debug( Supplier<String> content, Throwable error )
    {
        if ( isDebugEnabled() )
        {
            logger.debug( content.get(), error );
        }
    }

    @Override
    public void info( CharSequence content )
    {
        logger.info( toString( content ) );
    }

    @Override
    public void info( CharSequence content, Throwable error )
    {
        logger.info( toString( content ), error );
    }

    @Override
    public void info( Throwable error )
    {
        logger.info( "", error );
    }

    @Override
    public void info( Supplier<String> content )
    {
        if ( isInfoEnabled() )
        {
            logger.info( content.get() );
        }
    }

    @Override
    public void info( Supplier<String> content, Throwable error )
    {
        if ( isInfoEnabled() )
        {
            logger.info( content.get(), error );
        }
    }

    @Override
    public void warn( CharSequence content )
    {
        logger.warn( toString( content ) );
    }

    @Override
    public void warn( CharSequence content, Throwable error )
    {
        logger.warn( toString( content ), error );
    }

    @Override
    public void warn( Throwable error )
    {
        logger.warn( "", error );
    }

    @Override
    public void warn( Supplier<String> content )
    {
        if ( isWarnEnabled() )
        {
            logger.warn( content.get() );
        }
    }

    @Override
    public void warn( Supplier<String> content, Throwable error )
    {
        if ( isWarnEnabled() )
        {
            logger.info( content.get(), error );
        }
    }

    @Override
    public void error( CharSequence content )
    {
        logger.error( toString( content ) );
    }

    @Override
    public void error( CharSequence content, Throwable error )
    {
        logger.error( toString( content ), error );
    }

    @Override
    public void error( Throwable error )
    {
        logger.error( "", error );
    }

    @Override
    public void error( Supplier<String> content )
    {
        if ( isErrorEnabled() )
        {
            logger.error( content.get() );
        }
    }

    @Override
    public void error( Supplier<String> content, Throwable error )
    {
        if ( isErrorEnabled() )
        {
            logger.error( content.get(), error );
        }
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

    private String toString( CharSequence content )
    {
        return content != null ? content.toString() : "";
    }

}
