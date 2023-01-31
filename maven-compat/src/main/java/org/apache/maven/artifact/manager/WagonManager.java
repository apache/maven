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
package org.apache.maven.artifact.manager;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;

/**
 * Manages <a href="https://maven.apache.org/wagon">Wagon</a> related operations in Maven.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 */
@Deprecated
public interface WagonManager extends org.apache.maven.repository.legacy.WagonManager {
    /**
     * this method is only here for backward compat (project-info-reports:dependencies)
     * the default implementation will return an empty AuthenticationInfo
     */
    AuthenticationInfo getAuthenticationInfo(String id);

    ProxyInfo getProxy(String protocol);

    void getArtifact(Artifact artifact, ArtifactRepository repository)
            throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifact(Artifact artifact, List<ArtifactRepository> remoteRepositories)
            throws TransferFailedException, ResourceDoesNotExistException;

    ArtifactRepository getMirrorRepository(ArtifactRepository repository);
}
