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
package org.apache.maven.lifecycle.internal.filter;

import org.apache.maven.api.MojoExecution;

/**
 * Predicate for filtering {@link MojoExecution} entries from the lifecycle build plan.
 *
 * <p>Used by {@code -Dmaven.lifecycle.filter} and by {@code <filter>} in {@code .mvn/reactor.xml}
 * to remove matching executions before the build starts.
 * New predicate types can be added without breaking existing filter expressions.
 *
 * @see MojoExecutionFilter
 * @since 4.1.0
 */
public interface FilterPredicate {

    /**
     * Returns {@code true} if the given mojo execution should be <em>excluded</em> from the build plan.
     *
     * @param execution the mojo execution to test
     * @return {@code true} to remove this execution from the plan, {@code false} to keep it
     */
    boolean matches(MojoExecution execution);
}
