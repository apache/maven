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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.Constants;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.internal.impl.DefaultMojoExecution;

/**
 * Parses the {@code maven.lifecycle.filter} user property value into a list of {@link FilterPredicate}s,
 * and applies them at mojo execution time in {@code MojoExecutor}.
 *
 * <p>The property value is a comma-separated list of predicates, OR-ed together:
 * a mojo execution matching <em>any</em> predicate is skipped (a {@code MojoSkipped} event is fired).
 *
 * <p>Supported predicate forms:
 * <ul>
 *   <li>{@code *} — skip all mojo executions</li>
 *   <li>{@code :A} — skip by artifactId (e.g. {@code :maven-enforcer-plugin})</li>
 *   <li>{@code G:A} — skip by groupId:artifactId</li>
 *   <li>{@code P} — skip by plugin prefix (e.g. {@code enforcer})</li>
 *   <li>{@code P:v:g} — skip by prefix:version:goal</li>
 *   <li>{@code P:v:g@e} — skip by prefix:version:goal@executionId</li>
 *   <li>{@code phase(name)} — skip all mojos bound to the named phase</li>
 * </ul>
 *
 * @since 4.1.0
 */
public class MojoExecutionFilter {

    /** The name of the user property that activates the filter. */
    public static final String PROPERTY_NAME = Constants.MAVEN_LIFECYCLE_FILTER;

    private MojoExecutionFilter() {
        // utility class
    }

    /**
     * Parses a filter expression string into a list of {@link FilterPredicate}s.
     *
     * @param expression the comma-separated filter expression (may be {@code null} or blank)
     * @return list of parsed predicates; empty if the expression is absent or blank
     */
    public static List<FilterPredicate> parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        List<FilterPredicate> predicates = new ArrayList<>();
        for (String token : expression.split(",")) {
            token = token.strip();
            if (token.isEmpty()) {
                continue;
            }
            predicates.add(parseToken(token));
        }
        return List.copyOf(predicates);
    }

    private static FilterPredicate parseToken(String token) {
        if (token.startsWith("phase(") && token.endsWith(")")) {
            String phaseName = token.substring("phase(".length(), token.length() - 1);
            return new PhasePredicate(phaseName);
        }
        return CoordinatePredicate.parse(token);
    }

    /**
     * Returns {@code true} if the given legacy mojo execution matches any predicate in the list
     * and should be skipped.
     *
     * <p>Uses {@link DefaultMojoExecution} with a {@code null} session: only string fields
     * ({@link MojoExecution#getDescriptor()}, {@link MojoExecution#getGoal()}, etc.) are accessed
     * by the predicates.
     *
     * @param execution  the legacy mojo execution to test
     * @param predicates the predicates to apply (OR-ed)
     * @return {@code true} if the execution should be skipped
     */
    public static boolean matches(org.apache.maven.plugin.MojoExecution execution, List<FilterPredicate> predicates) {
        if (predicates.isEmpty()) {
            return false;
        }
        MojoExecution apiExecution = new DefaultMojoExecution(null, execution);
        for (FilterPredicate predicate : predicates) {
            if (predicate.matches(apiExecution)) {
                return true;
            }
        }
        return false;
    }
}
