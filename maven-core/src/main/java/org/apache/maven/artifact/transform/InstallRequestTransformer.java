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
import org.eclipse.aether.installation.InstallRequest;

/**
 * Can transform installation request just before installing.
 *
 * @since 1.8.0
 */
public interface InstallRequestTransformer
{
    /**
     * The key to set instance in {@link RepositorySystemSession#getData()}.
     */
    String KEY = InstallRequestTransformer.class.getName();

    /**
     * Transform the {@link InstallRequest} just before installing.
     *
     * @param session        The session.
     * @param installRequest the original installation request being installed.
     * @return the transformed installation request, never {@code null}.
     */
    InstallRequest transformInstallRequest( RepositorySystemSession session, InstallRequest installRequest );
}
