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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nullable;

/**
 * A dependency exclusion.
 *
 * @since 4.0.0
 * @see DependencyCoordinates#getExclusions()
 */
@Experimental
public interface Exclusion {
    /**
     * Returns the groupId for this exclusion, or {@code null} if unset.
     *
     * @return the groupId, or {@code null}
     * @since 4.0.0
     */
    @Nullable
    default String groupId() {
        return getGroupId();
    }

    /**
     * @deprecated Use {@link #groupId()} instead.
     */
    @Nullable
    @Deprecated(since = "4.0.0", forRemoval = true)
    String getGroupId();

    /**
     * Returns the artifactId for this exclusion, or {@code null} if unset.
     *
     * @return the artifactId, or {@code null}
     * @since 4.0.0
     */
    @Nullable
    default String artifactId() {
        return getArtifactId();
    }

    /**
     * @deprecated Use {@link #artifactId()} instead.
     */
    @Nullable
    @Deprecated(since = "4.0.0", forRemoval = true)
    String getArtifactId();
}
