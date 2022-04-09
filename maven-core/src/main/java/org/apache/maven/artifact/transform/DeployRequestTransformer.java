package org.apache.maven.artifact.transform;

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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;

/**
 * Can transform deployment request just before deploying.
 *
 * @since 1.8.0
 */
public interface DeployRequestTransformer
{
    /**
     * The key to set instance in {@link RepositorySystemSession#getData()}.
     */
    String KEY = DeployRequestTransformer.class.getName();

    /**
     * Transform the {@link DeployRequest} just before deploying.
     *
     * @param session       The session.
     * @param deployRequest the original deployment request being deployed.
     * @return the transformed deployment request, never {@code null}.
     */
    DeployRequest transformDeployRequest( RepositorySystemSession session, DeployRequest deployRequest );
}
