package org.apache.maven.execution.initialize;

import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.execution.reactor.MavenReactorExecutionRequest;
import org.apache.maven.execution.project.MavenProjectExecutionRequestHandler;
import org.apache.maven.execution.MavenExecutionRequestHandler;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenInitializingExecutionRequestHandler
    extends MavenProjectExecutionRequestHandler
    implements MavenExecutionRequestHandler
{
    public void handle( MavenExecutionRequest request, MavenExecutionResponse response )
        throws Exception
    {
            super.handle( null, null );
    }
}
