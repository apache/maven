package org.apache.maven.plugin;

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

import org.apache.maven.monitor.logging.Log;
import org.apache.maven.monitor.logging.SystemStreamLog;

/**
 * @version $Id$
 */
public abstract class AbstractPlugin
    implements Plugin
{
    private Log log;

    /**
     * Default behaviour to mimic old behaviour.
     *
     * @deprecated
     */
    public void execute( PluginExecutionRequest request )
        throws PluginExecutionException
    {
        PluginExecutionResponse response = new PluginExecutionResponse();
        try
        {
            execute( request, response );
        }
        catch ( Exception e )
        {
            throw new PluginExecutionException( e.getMessage(), e );
        }
        if ( response.isExecutionFailure() )
        {
            throw new PluginExecutionException( response.getFailureResponse().getSource(),
                                                response.getFailureResponse().shortMessage(),
                                                response.getFailureResponse().longMessage() );
        }
    }

    /**
     * @deprecated
     */
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        throw new UnsupportedOperationException(
            "If you are using the old technioque, you must override execute(req,resp)" );
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    public Log getLog()
    {
        synchronized ( this )
        {
            if ( log == null )
            {
                log = new SystemStreamLog();
            }
        }

        return log;
    }

    public void execute()
        throws PluginExecutionException
    {
        if ( supportsNewMojoParadigm() )
        {
            throw new PluginExecutionException( "You must override execute() if you implement the new paradigm" );
        }
    }

    public boolean supportsNewMojoParadigm()
    {
        return false;
    }
}
