package org.apache.maven.plugin.internal;

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

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;

import javax.inject.Provider;

import static java.util.Objects.requireNonNull;

/**
 * Mojo {@link Log} implementation that lazily creates {@link Logger} instances. To achieve "lazyness" it uses
 * {@link Provider} for logger, but guarantees that {@link Provider#get()} is invoked only once, and once got
 * Logger instance is reused.
 *
 * @since TBD
 */
public class MojoLog
    implements Log
{
    private final Provider<Logger> loggerProvider;

    public MojoLog( Provider<Logger> loggerProvider )
    {
        this.loggerProvider = memoize( loggerProvider );
    }

    private Logger getLogger()
    {
        return loggerProvider.get();
    }

    private String toString( CharSequence content )
    {
        if ( content == null )
        {
            return "";
        }
        else
        {
            return content.toString();
        }
    }

    @Override
    public void debug( CharSequence content )
    {
        getLogger().debug( toString( content ) );
    }

    @Override
    public void debug( CharSequence format, Object... arguments )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( toString( format ), arguments );
        }
    }

    @Override
    public void debug( CharSequence content, Throwable error )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( toString( content ), error );
        }
    }

    @Override
    public void debug( Throwable error )
    {
        getLogger().debug( "", error );
    }

    @Override
    public void info( CharSequence content )
    {
        getLogger().info( toString( content ) );
    }

    @Override
    public void info( CharSequence content, Throwable error )
    {
        getLogger().info( toString( content ), error );
    }

    @Override
    public void info( Throwable error )
    {
        getLogger().info( "", error );
    }

    @Override
    public void warn( CharSequence content )
    {
        getLogger().warn( toString( content ) );
    }

    @Override
    public void warn( CharSequence content, Throwable error )
    {
        getLogger().warn( toString( content ), error );
    }

    @Override
    public void warn( Throwable error )
    {
        getLogger().warn( "", error );
    }

    @Override
    public void error( CharSequence content )
    {
        getLogger().error( toString( content ) );
    }

    @Override
    public void error( CharSequence content, Throwable error )
    {
        getLogger().error( toString( content ), error );
    }

    @Override
    public void error( Throwable error )
    {
        getLogger().error( "", error );
    }

    @Override
    public boolean isDebugEnabled()
    {
        return getLogger().isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled()
    {
        return getLogger().isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled()
    {
        return getLogger().isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled()
    {
        return getLogger().isErrorEnabled();
    }

    /**
     * A helper bit to make users of this class easier: it wraps {@link Provider} into "memoizing" provider: the
     * wrapped (delegate) provider will be invoked only once, and the returned value will be stored for subsequent
     * invocations of {@link Provider#get()} on wrapper.
     */
    private static <T> Provider<T> memoize( Provider<T> provider )
    {
        requireNonNull( provider );
        return new Provider<T>()
        {
            Provider<T> delegate = this::memoize;

            volatile boolean memoized = false;

            @Override
            public T get()
            {
                return delegate.get();
            }

            private synchronized T memoize()
            {
                if ( !memoized )
                {
                    T value = provider.get();
                    delegate = () -> value;
                    memoized = true;
                }
                return delegate.get();
            }
        };
    }
}
