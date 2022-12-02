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
package org.apache.maven.repository.legacy.repository;

import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/** @author jdcasey */
public interface ArtifactRepositoryFactory {

    String DEFAULT_LAYOUT_ID = "default";

    String LOCAL_REPOSITORY_ID = "local";

    @Deprecated
    ArtifactRepositoryLayout getLayout(String layoutId) throws UnknownRepositoryLayoutException;

    @Deprecated
    ArtifactRepository createDeploymentArtifactRepository(String id, String url, String layoutId, boolean uniqueVersion)
            throws UnknownRepositoryLayoutException;

    ArtifactRepository createDeploymentArtifactRepository(
            String id, String url, ArtifactRepositoryLayout layout, boolean uniqueVersion);

    ArtifactRepository createArtifactRepository(
            String id,
            String url,
            String layoutId,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases)
            throws UnknownRepositoryLayoutException;

    ArtifactRepository createArtifactRepository(
            String id,
            String url,
            ArtifactRepositoryLayout repositoryLayout,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases);

    void setGlobalUpdatePolicy(String snapshotPolicy);

    void setGlobalChecksumPolicy(String checksumPolicy);
}
