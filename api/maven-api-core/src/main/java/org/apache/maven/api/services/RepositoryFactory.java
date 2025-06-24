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
package org.apache.maven.api.services;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Repository;

/**
 * Factory service to create {@link LocalRepository} or {@link RemoteRepository} objects.
 *
 * @since 4.0.0
 */
@Experimental
public interface RepositoryFactory extends Service {

    @Nonnull
    LocalRepository createLocal(@Nonnull Path path);

    @Nonnull
    RemoteRepository createRemote(@Nonnull String id, @Nonnull String url);

    @Nonnull
    RemoteRepository createRemote(@Nonnull Repository repository);

    @Nonnull
    List<RemoteRepository> aggregate(
            @Nonnull Session session,
            @Nonnull List<RemoteRepository> dominant,
            @Nonnull List<RemoteRepository> recessive,
            boolean processRecessive);
}
