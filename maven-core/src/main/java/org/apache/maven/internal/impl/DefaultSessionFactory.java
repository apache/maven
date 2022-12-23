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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.Session;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.SessionData;

@Singleton
@Named
public class DefaultSessionFactory {
    private final RepositorySystem repositorySystem;
    private final MavenRepositorySystem mavenRepositorySystem;
    private final PlexusContainer plexusContainer;
    private final RuntimeInformation runtimeInformation;

    @Inject
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultSessionFactory(
            RepositorySystem repositorySystem,
            MavenRepositorySystem mavenRepositorySystem,
            PlexusContainer plexusContainer,
            RuntimeInformation runtimeInformation) {
        this.repositorySystem = repositorySystem;
        this.mavenRepositorySystem = mavenRepositorySystem;
        this.plexusContainer = plexusContainer;
        this.runtimeInformation = runtimeInformation;
    }

    public Session getSession(MavenSession mavenSession) {
        SessionData data = mavenSession.getRepositorySession().getData();
        return (Session) data.computeIfAbsent(DefaultSession.class, () -> newSession(mavenSession));
    }

    private Session newSession(MavenSession mavenSession) {
        return new DefaultSession(
                mavenSession, repositorySystem, null, mavenRepositorySystem, plexusContainer, runtimeInformation);
    }
}
