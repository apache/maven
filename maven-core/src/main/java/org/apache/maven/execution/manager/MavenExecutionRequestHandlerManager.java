package org.apache.maven.execution.manager;


import org.apache.maven.execution.MavenExecutionRequestHandler;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface MavenExecutionRequestHandlerManager
{
    String ROLE = MavenExecutionRequestHandlerManager.class.getName();

    MavenExecutionRequestHandler lookup( String roleHint )
        throws MavenExecutionRequestHandlerNotFoundException;

    int managedCount();
}
