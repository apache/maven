package org.apache.maven.execution;


/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface MavenExecutionRequestHandler
{
    void handle( MavenExecutionRequest request, MavenExecutionResponse response )
        throws Exception;
}
