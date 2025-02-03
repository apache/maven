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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Nonnull;

/**
 * Represents a repository backed by an IDE workspace, the output of a build session,
 * or similar ad-hoc collections of artifacts. This repository is considered read-only
 * within the context of a session, meaning it can only be used for artifact resolution,
 * not for installation or deployment. This interface does not provide direct access
 * to artifacts; that functionality is handled by a {@code WorkspaceReader}.
 */
public interface WorkspaceRepository extends Repository {

    /**
     * {@return the type of the repository, i.e. "workspace"}
     */
    @Nonnull
    @Override
    default String getType() {
        return "workspace";
    }
}
