package org.apache.maven.execution.project;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.AbstractMavenExecutionRequestHandler;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.lifecycle.session.MavenSession;

import java.io.File;
import java.util.Date;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenProjectExecutionRequestHandler
    extends AbstractMavenExecutionRequestHandler
{
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

    protected MavenSession createSession( MavenExecutionRequest request )
        throws Exception
    {
        MavenSession session = super.createSession( request );

        MavenProject project = getProject( ( (MavenProjectExecutionRequest) request ).getPom(), request.getLocalRepository() );

        session.setProject( project );

        return session;
    }
}
