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
package org.apache.maven.resolver;

import org.apache.maven.execution.MavenExecutionRequest;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;

/**
 * Factory for Resolver session.
 *
 * @since 4.0.0
 */
public interface RepositorySystemSessionFactory {
    /**
     * Creates "ready to use" session builder instance. The factory does not set up one thing: the
     * {@link org.eclipse.aether.repository.WorkspaceReader}s, that is caller duty to figure out. Workspace readers
     * should be set up as very last thing before using resolver session, that is built by invoking
     * {@link SessionBuilder#build()} method.
     *
     * @param request The maven execution request, must not be {@code null}.
     * @return The session builder "ready to use" without workspace readers.
     */
    SessionBuilder newRepositorySessionBuilder(MavenExecutionRequest request);
}
