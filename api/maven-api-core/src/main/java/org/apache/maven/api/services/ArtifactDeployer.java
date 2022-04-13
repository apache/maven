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

import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;

/**
 * 
 */
public interface ArtifactDeployer extends Service
{

    /**
     * @param request {@link ArtifactDeployerRequest}
     * @throws ArtifactDeployerException if the deployment failed
     */
    void deploy( @Nonnull ArtifactDeployerRequest request );

    /**
     * @param session the repository session
     * @param repository the repository to deploy to
     * @param artifacts the collection of artifacts to deploy
     * @throws ArtifactDeployerException if the deployment failed
     * @throws IllegalArgumentException if an argument is {@code null} or invalid
     */
    default void deploy( @Nonnull Session session,
                         @Nonnull RemoteRepository repository,
                         @Nonnull Collection<Artifact> artifacts )
    {
        deploy( ArtifactDeployerRequest.build( session, repository, artifacts ) );
    }

}
