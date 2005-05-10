package org.apache.maven.monitor.logging;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

/**
 * @author jdcasey
 */
public class DefaultLog
    implements Log
{

    private final Logger logger;

    public DefaultLog( Logger logger )
    {
        this.logger = logger;
    }

    public void debug( CharSequence content )
    {
        logger.debug( content.toString() );
    }

    public void debug( CharSequence content, Throwable error )
    {
        logger.debug( content.toString(), error );
    }

    public void debug( Throwable error )
    {
        logger.debug( "", error );
    }

    public void info( CharSequence content )
    {
        logger.info( content.toString() );
    }

    public void info( CharSequence content, Throwable error )
    {
        logger.info( content.toString(), error );
    }

    public void info( Throwable error )
    {
        logger.info( "", error );
    }

    public void warn( CharSequence content )
    {
        logger.warn( content.toString() );
    }

    public void warn( CharSequence content, Throwable error )
    {
        logger.warn( content.toString(), error );
    }

    public void warn( Throwable error )
    {
        logger.warn( "", error );
    }

    public void error( CharSequence content )
    {
        logger.error( content.toString() );
    }

    public void error( CharSequence content, Throwable error )
    {
        logger.error( content.toString(), error );
    }

    public void error( Throwable error )
    {
        logger.error( "", error );
    }

    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

}