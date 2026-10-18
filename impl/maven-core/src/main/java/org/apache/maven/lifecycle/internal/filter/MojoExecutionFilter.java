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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Constants;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;
import org.apache.maven.api.xml.XmlNode;

/**
 * Parses the {@code maven.lifecycle.filter} user property value into a list of {@link FilterPredicate}s,
 * and applies them at mojo execution time in {@code MojoExecutor}.
 *
 * <p>The property value is a comma-separated list of predicates, OR-ed together:
 * a mojo execution matching <em>any</em> predicate is skipped.
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
        if (token.startsWith("phase(")) {
            if (!token.endsWith(")")) {
                throw new IllegalArgumentException("Invalid phase() predicate '" + token + "': missing closing ')'");
            }
            String phaseName = token.substring("phase(".length(), token.length() - 1);
            if (phaseName.isBlank()) {
                throw new IllegalArgumentException("phase() predicate requires a non-blank phase name");
            }
            if (phaseName.contains(")")) {
                throw new IllegalArgumentException(
                        "Invalid phase() predicate '" + token + "': phase name must not contain ')'");
            }
            return new PhasePredicate(phaseName);
        }
        return CoordinatePredicate.parse(token);
    }

    /**
     * Returns {@code true} if the given legacy mojo execution matches any predicate in the list
     * and should be skipped.
     *
     * <p>Adapts the legacy {@link org.apache.maven.plugin.MojoExecution} into a lightweight
     * {@link MojoExecution} view — only the fields accessed by the predicates
     * ({@code plugin()}, {@code goal()}, {@code executionId()}, {@code lifecyclePhase()})
     * are populated. No session is required.
     *
     * @param execution  the legacy mojo execution to test
     * @param predicates the predicates to apply (OR-ed)
     * @return {@code true} if the execution should be skipped
     */
    public static boolean matches(org.apache.maven.plugin.MojoExecution execution, List<FilterPredicate> predicates) {
        if (predicates.isEmpty()) {
            return false;
        }
        MojoExecution apiExecution = adapt(execution);
        for (FilterPredicate predicate : predicates) {
            if (predicate.matches(apiExecution)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adapts a legacy {@link org.apache.maven.plugin.MojoExecution} to the API
     * {@link MojoExecution} interface using only the fields needed by the predicates.
     * No session is required.
     */
    private static MojoExecution adapt(org.apache.maven.plugin.MojoExecution legacy) {
        org.apache.maven.plugin.descriptor.MojoDescriptor legacyMojo = legacy.getMojoDescriptor();
        org.apache.maven.plugin.descriptor.PluginDescriptor legacyPlugin =
                legacyMojo != null ? legacyMojo.getPluginDescriptor() : null;
        PluginDescriptor descriptorV4 = legacyPlugin != null ? legacyPlugin.getPluginDescriptorV4() : null;

        Plugin plugin = new Plugin() {
            @Override
            public org.apache.maven.api.model.Plugin getModel() {
                return null;
            }

            @Override
            @Nullable // adapter may lack a descriptor; all predicates null-guard this
            public PluginDescriptor getDescriptor() {
                return descriptorV4;
            }

            @Override
            public List<Lifecycle> getLifecycles() {
                return Collections.emptyList();
            }

            @Override
            public ClassLoader getClassLoader() {
                throw new UnsupportedOperationException("getClassLoader() is not available in the filter adapter");
            }

            @Override
            public Artifact getArtifact() {
                return null;
            }

            @Override
            public java.util.Map<String, Dependency> getDependenciesMap() {
                return Collections.emptyMap();
            }
        };

        String goal = legacy.getGoal();
        Optional<String> executionId = Optional.ofNullable(legacy.getExecutionId());
        Optional<String> lifecyclePhase = Optional.ofNullable(legacy.getLifecyclePhase());

        return new MojoExecution() {
            @Override
            public Plugin plugin() {
                return plugin;
            }

            @Override
            public Optional<PluginExecution> model() {
                return Optional.empty();
            }

            @Override
            @Nullable
            public org.apache.maven.api.plugin.descriptor.MojoDescriptor descriptor() {
                return legacyMojo != null ? legacyMojo.getMojoDescriptorV4() : null;
            }

            @Override
            public Optional<String> executionId() {
                return executionId;
            }

            @Override
            public String goal() {
                return goal;
            }

            @Override
            public Optional<String> lifecyclePhase() {
                return lifecyclePhase;
            }

            @Override
            public Optional<XmlNode> configuration() {
                return Optional.empty();
            }
        };
    }
}
