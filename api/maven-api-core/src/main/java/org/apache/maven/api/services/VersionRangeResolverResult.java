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

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Repository;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

@Experimental
public interface VersionRangeResolverResult {

    @Nonnull
    List<Exception> getExceptions();

    @Nonnull
    List<Version> getVersions();

    @Nonnull
    default Optional<Version> getLowerVersion() {
        return getVersions().isEmpty()
                ? Optional.empty()
                : Optional.of(getVersions().get(0));
    }

    @Nonnull
    default Optional<Version> getHigherVersion() {
        return getVersions().isEmpty()
                ? Optional.empty()
                : Optional.of(getVersions().get(getVersions().size() - 1));
    }

    @Nonnull
    Optional<Repository> getRepository(Version version);
}
