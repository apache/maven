package org.apache.maven.execution.manager;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.apache.maven.execution.MavenExecutionRequestHandler;

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
