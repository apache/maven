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
package org.apache.maven.repository.legacy;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

/**
 * UpdateCheckManager
 */
@Deprecated
public interface UpdateCheckManager {

    boolean isUpdateRequired(Artifact artifact, ArtifactRepository repository);

    void touch(Artifact artifact, ArtifactRepository repository, String error);

    String getError(Artifact artifact, ArtifactRepository repository);

    boolean isUpdateRequired(RepositoryMetadata metadata, ArtifactRepository repository, File file);

    void touch(RepositoryMetadata metadata, ArtifactRepository repository, File file);
}
