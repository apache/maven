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
package org.apache.maven.repository.internal;

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.version.VersionConstraint;

/**
 * Builds {@link VersionFilter} instances out of input expression string.
 *
 * @since 3.10.0
 */
public interface VersionFilterBuilder {
    /**
     * User property for version filter suppression. Presence of this key will suppress filter created by this builder.
     */
    String MAVEN_VERSION_FILTER_SUPPRESSED = "maven.session.versionFilter.suppressed";

    /**
     * Builds a version filter based on the given filter expression.
     *
     * @param filterExpression a string containing filter expressions, may be {@code null}.
     * @param versionConstraintParser version constraint parts to be used during parsing, must not be {@code null}.
     * @return optional version filter, never {@code null}.
     */
    Optional<VersionFilter> buildVersionFilter(
            String filterExpression, Function<String, VersionConstraint> versionConstraintParser);
}
