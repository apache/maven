package org.apache.maven.repository;

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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Mirror;

/**
 * Handles the selection of mirrors for repositories.
 *
 * @author Benjamin Bentmann
 */
public interface MirrorSelector
{

    /**
     * Determines the mirror for the specified repository.
     *
     * @param repository The repository to determine the mirror for, must not be {@code null}.
     * @param mirrors The available mirrors, may be {@code null}.
     * @return The mirror specification for the repository or {@code null} if no mirror matched.
     */
    Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors );

}
