package org.apache.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Helps to provide backward-compatibility with plugins that use legacy components. <strong>Warning:</strong> This is an
 * internal utility interface that is only public for technical reasons, it is not part of the public API. In
 * particular, this interface can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
public interface LegacySupport
{

    /**
     * Sets the currently active session. Some legacy components are basically stateful and their API is missing
     * parameters that would be required to delegate to a stateless component. Saving the session (in a thread-local
     * variable) is our best effort to record any state that is required to enable proper delegation.
     *
     * @param session The currently active session, may be {@code null}.
     */
    void setSession( MavenSession session );

    /**
     * Gets the currently active session.
     *
     * @return The currently active session or {@code null} if none.
     */
    MavenSession getSession();

    /**
     * Gets the currently active repository session.
     *
     * @return The currently active repository session or {@code null} if none.
     */
    RepositorySystemSession getRepositorySession();

}
