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
import org.apache.maven.lifecycle.session.MavenSession;

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class AbstractMavenExecutionRequest
    implements MavenExecutionRequest
{
    protected ArtifactRepository localRepository;

    protected List goals;

    protected String type;

    protected MavenSession session;

    public AbstractMavenExecutionRequest( ArtifactRepository localRepository, List goals )
    {
        this.localRepository = localRepository;

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
}
