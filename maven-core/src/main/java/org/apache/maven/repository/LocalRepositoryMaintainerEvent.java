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

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Describes an event to be consumed by {@link LocalRepositoryMaintainer}.
 * 
 * @author Benjamin Bentmann
 */
public interface LocalRepositoryMaintainerEvent
{

    /**
     * The local ArtifactRepository instance that generated the event.
     * 
     * @return Source artifact repository, never {@code null}.
     */
    ArtifactRepository getLocalRepository();

    /**
     * The group id of the artifact.
     * 
     * @return The group id of the artifact, never {@code null}.
     */
    String getGroupId();

    /**
     * The artifact id of the artifact.
     * 
     * @return The artifact id of the artifact, never {@code null}.
     */
    String getArtifactId();

    /**
     * The version of the artifact.
     * 
     * @return The version of the artifact, never {@code null}.
     */
    String getVersion();

    /**
     * The classifier of the artifact.
     * 
     * @return The classifier of the artifact or an empty string if none, never {@code null}.
     */
    String getClassifier();

    /**
     * The type of the artifact.
     * 
     * @return The type of the artifact, never {@code null}.
     */
    String getType();

    /**
     * The path to the artifact.
     * 
     * @return The path to the artifact, never {@code null}.
     */
    File getFile();

}
