package org.apache.maven.execution.reactor;

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
public class MavenReactorExecutionRequestHandler
    extends MavenProjectExecutionRequestHandler
    implements MavenExecutionRequestHandler
{
    public void handle( MavenExecutionRequest request, MavenExecutionResponse response )
        throws Exception
    {
        List projects = new ArrayList();

        getLogger().info( "Starting the reactor..." );

        try
        {
            List files = FileUtils.getFiles( new File( System.getProperty( "user.dir" ) ),
                                             ((MavenReactorExecutionRequest)request).getIncludes(),
                                             ((MavenReactorExecutionRequest)request).getExcludes() );

            for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
            {
                File file = (File) iterator.next();

                MavenProject project = getProject( file, request.getLocalRepository() );

                projects.add( project );
            }

            projects = projectBuilder.getSortedProjects( projects );
        }
        catch ( Exception e )
        {
            throw new ReactorException( "Error processing projects for the reactor: ", e );
        }

        getLogger().info( "Our processing order:" );

        for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();

            getLogger().info( project.getName() );
        }

        for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();

            System.out.println( "\n\n\n" );

            line();

            getLogger().info( "Building " + project.getName() );

            line();

            //MavenProjectExecutionRequest r = new MavenProjectExecutionRequest();

            super.handle( null, null );

            //response = execute( new MavenProjectExecutionRequest( request.getLocalRepository(), request.getGoals(), project ) );

            if ( response.isExecutionFailure() )
            {
                break;
            }
        }
    }

    // ----------------------------------------------------------------------
    // Reactor
    // ----------------------------------------------------------------------

    public List getSortedProjects( List projects ) throws Exception
    {
        return projectBuilder.getSortedProjects( projects );
    }
}
