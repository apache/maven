package org.apache.maven.execution;

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
import org.apache.maven.execution.project.MavenProjectExecutionRequest;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class AbstractMavenExecutionRequest
implements MavenExecutionRequest
{
    /** @todo [BP] is this required? This hands off to MavenSession, but could be passed through the handler.handle function (+ createSession). */
    protected ArtifactRepository localRepository;
    protected final Properties parameters;
    protected List goals;
    protected String type;
    protected MavenSession session;

    public AbstractMavenExecutionRequest( ArtifactRepository localRepository, Properties parameters, List goals )
    {
        this.localRepository = localRepository;
        this.parameters = parameters;
        this.goals = goals;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public List getGoals()
    {
        return goals;
    }

    public String getType()
    {
        return type;
    }

    public String getParameter( String name )
    {
        return parameters.getProperty( name );
    }

    // ----------------------------------------------------------------------
    // Putting the session here but it can probably be folded right in here.
    // ----------------------------------------------------------------------

    public MavenSession getSession()
    {
        return session;
    }

    public void setSession( MavenSession session )
    {
        this.session = session;
    }

    public MavenProjectExecutionRequest createProjectExecutionRequest( MavenProject project )
    {
        return new MavenProjectExecutionRequest( getLocalRepository(), parameters, getGoals(), project.getFile() );
    }
}
