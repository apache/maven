package org.apache.maven.execution.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.AbstractMavenExecutionRequestHandler;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.Date;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenProjectExecutionRequestHandler
    extends AbstractMavenExecutionRequestHandler
{
    public void handle( MavenExecutionRequest request, MavenExecutionResponse response )
        throws Exception
    {
        Date start = null;

        Date finish = null;

        try
        {
            MavenProject project = getProject( ( (MavenProjectExecutionRequest) request ).getPom(), request.getLocalRepository() );

            start = new Date();

            response = lifecycleManager.execute( createSession( request, project ) );
        }
        catch ( Exception e )
        {
            finish = new Date();

            response.setException( e );

            response.setStart( start );

            response.setFinish( finish );

            logError( response );

            return;
        }

        finish = new Date();

        response.setStart( start );

        response.setFinish( finish );

        if ( response.isExecutionFailure() )
        {
            logFailure( response );
        }
        else
        {
            logSuccess( response );
        }
    }

    // ----------------------------------------------------------------------
    // Project building
    // ----------------------------------------------------------------------

    public MavenProject getProject( File pom, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        if ( pom.exists() )
        {
            if ( pom.length() == 0 )
            {
                throw new ProjectBuildingException( i18n.format( "empty.descriptor.error", pom.getName() ) );
            }
        }

        return projectBuilder.build( pom, localRepository );
    }
}
