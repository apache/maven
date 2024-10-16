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
import org.apache.maven.api.annotations.Nonnull;

/**
 * A version or meta-version of an artifact or a dependency.
 * A meta-version is a version suffixed with the {@code SNAPSHOT} keyword.
 * Versions are usually parsed using the {@link org.apache.maven.api.services.VersionParser} service.
 *
 * @since 4.0.0
 * @see org.apache.maven.api.services.VersionParser#parseVersion(String)
 * @see org.apache.maven.api.Session#parseVersion(String)
 * @see VersionConstraint
 * @see VersionRange
 */
@Experimental
public interface Version extends Comparable<Version> {
    /**
     * Returns a string representation of this version.
     * @return the string representation of this version
     */
    @Nonnull
    String asString();
}
