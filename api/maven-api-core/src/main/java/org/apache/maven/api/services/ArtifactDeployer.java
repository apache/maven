package org.apache.maven.api.services;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;

import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.RemoteRepository;

/**
 * 
 */
public interface ArtifactDeployer extends Service
{

    /**
     * @param request {@link ArtifactDeployerRequest}
     * @throws ArtifactDeployerException in case of an error.
     * @throws IllegalArgumentException in case of parameter <code>request</code> is <code>null</code> or parameter
     *             <code>mavenArtifacts</code> is <code>null</code> or <code>mavenArtifacts.isEmpty()</code> is
     *             <code>true</code>.
     */
    void deploy( ArtifactDeployerRequest request )
        throws ArtifactDeployerException, IllegalArgumentException;

    /**
     * @param session the repository session
     * @param repository the repository to deploy to
     * @param artifacts the collection of artifacts to deploy
     * @throws ArtifactDeployerException in case of an error.
     * @throws IllegalArgumentException in case of parameter <code>request</code> is <code>null</code> or parameter
     *             <code>artifacts</code> is <code>null</code> or <code>artifacts.isEmpty()</code> is
     *             <code>true</code>.
     */
    default void deploy( Session session,
                         RemoteRepository repository,
                         Collection<Artifact> artifacts )
        throws ArtifactDeployerException, IllegalArgumentException
    {
        deploy( ArtifactDeployerRequest.build( session, repository, artifacts ) );
    }

}
