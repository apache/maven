package org.apache.maven.execution.manager;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.execution.MavenExecutionRequestHandler;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultMavenExecutionRequestHandlerManager
    extends AbstractLogEnabled
    implements MavenExecutionRequestHandlerManager
{
    private Map handlers;

    public MavenExecutionRequestHandler lookup( String roleHint )
        throws MavenExecutionRequestHandlerNotFoundException
    {
        MavenExecutionRequestHandler handler = (MavenExecutionRequestHandler) handlers.get( roleHint );

        if ( handler == null )
        {
            throw new MavenExecutionRequestHandlerNotFoundException( "Cannot find the handler with type = " + roleHint );
        }

        return handler;
    }
    
    public int managedCount()
    {
        return handlers.size();
    }
}
