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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface MavenExecutionRequest
{
    ArtifactRepository getLocalRepository();

    List getGoals();

    void setSession( MavenSession session );

    MavenSession getSession();

    List getFiles();

    void setLog( Log log );

    Log getLog();

    void addEventMonitor( EventMonitor monitor );

    EventDispatcher getEventDispatcher();

    UserModel getUserModel();

    String getBaseDirectory();

    void setRecursive( boolean recursive );

    boolean isRecursive();
}
