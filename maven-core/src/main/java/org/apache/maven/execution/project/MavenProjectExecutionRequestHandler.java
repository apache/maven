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
        try
        {
            MavenProject project = getProject( ( (MavenProjectExecutionRequest) request ).getPom(), request.getLocalRepository() );

            Date s = new Date();

            response = sessionPhaseManager.execute( createSession( request, project ) );

            response.setStart( s );

            response.setFinish( new Date() );
        }
        catch ( Exception e )
        {
            response.setFinish( new Date() );

            response.setException( e );

            logError( response );

            return;
        }

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
