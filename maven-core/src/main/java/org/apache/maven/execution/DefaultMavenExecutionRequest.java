package org.apache.maven.execution;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.settings.Settings;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.EventMonitor;

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultMavenExecutionRequest
    implements MavenExecutionRequest
{
    /**
     * @todo [BP] is this required? This hands off to MavenSession, but could be passed through the handler.handle function (+ createSession).
     */
    private final ArtifactRepository localRepository;

    private final List goals;

    private final List files;

    protected MavenSession session;

    private final EventDispatcher eventDispatcher;

    private final Settings settings;

    private final String baseDirectory;

    private boolean recursive = true;

    public DefaultMavenExecutionRequest( ArtifactRepository localRepository, Settings settings,
                                         EventDispatcher eventDispatcher, List goals, List files, String baseDirectory )
    {
        this.localRepository = localRepository;

        this.settings = settings;

        this.goals = goals;

        this.eventDispatcher = eventDispatcher;

        this.files = files;

        this.baseDirectory = baseDirectory;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public String getBaseDirectory()
    {
        return baseDirectory;
    }

    public boolean isRecursive()
    {
        return recursive;
    }

    public void setRecursive( boolean recursive )
    {
        this.recursive = false;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public List getGoals()
    {
        return goals;
    }

    // ----------------------------------------------------------------------
    // Putting the session here but it can probably be folded right in here.
    // ----------------------------------------------------------------------

    public MavenSession getSession()
    {
        return session;
    }

    public List getProjectFiles()
    {
        return files;
    }

    public void setSession( MavenSession session )
    {
        this.session = session;
    }

    public void addEventMonitor( EventMonitor monitor )
    {
        eventDispatcher.addEventMonitor( monitor );
    }

    public EventDispatcher getEventDispatcher()
    {
        return eventDispatcher;
    }

    public List getFiles()
    {
        return files;
    }
}
