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
import org.apache.maven.model.user.UserModel;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.monitor.logging.Log;

import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractMavenExecutionRequest
implements MavenExecutionRequest
{
    /** @todo [BP] is this required? This hands off to MavenSession, but could be passed through the handler.handle function (+ createSession). */
    protected ArtifactRepository localRepository;

    protected List goals;

    protected String type;

    protected MavenSession session;

    private Log log;
    
    private EventDispatcher eventDispatcher;

    private final UserModel userModel;

    public AbstractMavenExecutionRequest( ArtifactRepository localRepository, UserModel userModel, EventDispatcher eventDispatcher, List goals )
    {
        this.localRepository = localRepository;
        
        this.userModel = userModel;

        this.goals = goals;
        
        this.eventDispatcher = eventDispatcher;
    }
    
    public UserModel getUserModel()
    {
        return userModel;
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
    
    public void setLog(Log log)
    {
        this.log = log;
    }
    
    public Log getLog()
    {
        return log;
    }
    
    public void addEventMonitor(EventMonitor monitor)
    {
        eventDispatcher.addEventMonitor(monitor);
    }
    
    public EventDispatcher getEventDispatcher()
    {
        return eventDispatcher;
    }
    
}
