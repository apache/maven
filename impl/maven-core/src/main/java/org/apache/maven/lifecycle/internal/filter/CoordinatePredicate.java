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
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;

/**
 * A {@link FilterPredicate} that matches {@link MojoExecution}s by plugin coordinate or prefix.
 *
 * <h2>Syntax: {@code ([G[:A]]|P)[:v][:g[@e]]}</h2>
 *
 * <p>The {@code @} separator for execution ID is compatible with Maven's existing
 * {@code plugin:version:goal@executionId} notation used in
 * {@code DefaultLifecycleExecutionPlanCalculator} for goal tasks.
 *
 * <p>Matching uses {@link MojoExecution#getDescriptor()} for goal-level fields and
 * {@link MojoExecution#getPlugin()} for plugin-level coordinates (groupId, artifactId, version,
 * goal prefix).
 *
 * <p>Forms:
 * <ul>
 *   <li>{@code *} — matches every mojo execution</li>
 *   <li>{@code :A} — any groupId, specific artifactId (e.g. {@code :maven-enforcer-plugin})</li>
 *   <li>{@code G:A} — exact groupId:artifactId (e.g. {@code org.apache.maven.plugins:maven-enforcer-plugin}).
 *       The groupId <strong>must contain at least one {@code '.'}</strong> for this form to be recognised;
 *       groupIds without a dot (e.g. {@code commons-io}) are routed to prefix-based matching instead.
 *       Use the {@code :A} form to match by artifactId only when the groupId has no dot.</li>
 *   <li>{@code P} — plugin prefix (e.g. {@code enforcer}), resolved against
 *       {@link MojoExecution#getMojoDescriptor()} goal prefix</li>
 *   <li>{@code P:v:g} — prefix + version + goal</li>
 *   <li>{@code P:v:g@e} — prefix + version + goal + executionId</li>
 * </ul>
 *
 * <p>When any field is {@code null} or not specified, it is treated as a wildcard (matches any value).
 *
 * @since 4.1.0
 */
public class CoordinatePredicate implements FilterPredicate {

    /** Wildcard token — matches any value. */
    private static final String ANY = null;

    private final boolean matchAll;
    private final String groupId; // null = any, non-null = exact match
    private final String artifactId; // null = any, non-null = exact match
    private final String prefix; // null = not used, non-null = match by goal prefix
    private final String version; // null = any
    private final String goal; // null = any
    private final String executionId; // null = any

    /** Wildcard predicate — matches everything. */
    public static final CoordinatePredicate MATCH_ALL = new CoordinatePredicate();

    private CoordinatePredicate() {
        this.matchAll = true;
        this.groupId = ANY;
        this.artifactId = ANY;
        this.prefix = ANY;
        this.version = ANY;
        this.goal = ANY;
        this.executionId = ANY;
    }

    private CoordinatePredicate(
            String groupId, String artifactId, String prefix, String version, String goal, String executionId) {
        this.matchAll = false;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.prefix = prefix;
        this.version = version;
        this.goal = goal;
        this.executionId = executionId;
    }

    /**
     * Parses a coordinate predicate from a string token.
     *
     * <p>Supported forms:
     * <ul>
     *   <li>{@code *} — match all</li>
     *   <li>{@code :A} — by artifactId only</li>
     *   <li>{@code G:A} — by groupId:artifactId. The groupId must contain at least one {@code '.'} to be
     *       recognised as a G:A form; groupIds without a dot are treated as a plugin prefix instead.</li>
     *   <li>{@code P} — by prefix</li>
     *   <li>{@code P:v:g} — by prefix + version + goal</li>
     *   <li>{@code P:v:g@e} — by prefix + version + goal + executionId</li>
     * </ul>
     *
     * @param token the filter expression token (not {@code null}, not blank)
     * @return the parsed predicate
     */
    public static CoordinatePredicate parse(String token) {
        if ("*".equals(token)) {
            return MATCH_ALL;
        }

        if (token.startsWith(":")) {
            // :A form — any groupId, specific artifactId, optional :v:g[@e]
            // e.g. ":maven-enforcer-plugin" or ":maven-enforcer-plugin:3.0.0:enforce@enforce-id"
            String rest = token.substring(1); // remove leading ':'
            String[] parts = rest.split(":", 3);
            String artifactId = emptyToNull(parts[0]);
            String version = parts.length > 1 ? emptyToNull(parts[1]) : null;
            String goalAndExec = parts.length > 2 ? parts[2] : null;
            String[] ge = splitGoalExecution(goalAndExec);
            return new CoordinatePredicate(ANY, artifactId, ANY, version, ge[0], ge[1]);
        }

        // Try to detect G:A form: the token contains ':' AND the part before the first ':' looks like a
        // groupId (contains a '.' suggesting it's a Java package name like org.apache.maven).
        // This distinguishes "org.apache.maven.plugins:maven-enforcer-plugin" from "enforcer:3.1.0:enforce".
        // Limitation: groupIds that do not contain a '.' (e.g. "commons-io:commons-io") are misrouted
        // to prefix-based matching. For such groupIds use the explicit ":A" form for artifactId-only
        // matching, or the full "G:A" form only when the groupId contains at least one '.'.
        int firstColon = token.indexOf(':');
        if (firstColon > 0 && token.substring(0, firstColon).contains(".")) {
            // G:A[:v[:g[@e]]] form
            String[] parts = token.split(":", 4);
            String groupId = emptyToNull(parts[0]);
            String artifactId = parts.length > 1 ? emptyToNull(parts[1]) : null;
            String version = parts.length > 2 ? emptyToNull(parts[2]) : null;
            String goalAndExec = parts.length > 3 ? parts[3] : null;
            String[] ge = splitGoalExecution(goalAndExec);
            return new CoordinatePredicate(groupId, artifactId, ANY, version, ge[0], ge[1]);
        }

        // P[:v[:g[@e]]] form — prefix-based
        String[] parts = token.split(":", 3);
        String prefix = emptyToNull(parts[0]);
        String version = parts.length > 1 ? emptyToNull(parts[1]) : null;
        String goalAndExec = parts.length > 2 ? parts[2] : null;
        String[] ge = splitGoalExecution(goalAndExec);
        return new CoordinatePredicate(ANY, ANY, prefix, version, ge[0], ge[1]);
    }

    /**
     * Splits a {@code "goal"} or {@code "goal@executionId"} string into a 2-element array
     * {@code [goal, executionId]}. Either element may be {@code null}.
     */
    private static String[] splitGoalExecution(String goalAndExec) {
        if (goalAndExec == null || goalAndExec.isEmpty()) {
            return new String[] {null, null};
        }
        int atIdx = goalAndExec.indexOf('@');
        if (atIdx < 0) {
            return new String[] {emptyToNull(goalAndExec), null};
        }
        return new String[] {emptyToNull(goalAndExec.substring(0, atIdx)), emptyToNull(goalAndExec.substring(atIdx + 1))
        };
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    @Override
    public boolean matches(MojoExecution execution) {
        if (matchAll) {
            return true;
        }

        PluginDescriptor descriptor =
                execution.getPlugin() != null ? execution.getPlugin().getDescriptor() : null;

        // prefix-based matching
        if (prefix != null) {
            String execPrefix = descriptor != null ? descriptor.getGoalPrefix() : null;
            if (!prefix.equals(execPrefix)) {
                return false;
            }
        } else {
            // coordinate-based matching
            if (groupId != null) {
                String execGroupId = descriptor != null ? descriptor.getGroupId() : null;
                if (!groupId.equals(execGroupId)) {
                    return false;
                }
            }
            if (artifactId != null) {
                String execArtifactId = descriptor != null ? descriptor.getArtifactId() : null;
                if (!artifactId.equals(execArtifactId)) {
                    return false;
                }
            }
        }

        if (version != null) {
            String execVersion = descriptor != null ? descriptor.getVersion() : null;
            if (!version.equals(execVersion)) {
                return false;
            }
        }
        if (goal != null && !goal.equals(execution.getGoal())) {
            return false;
        }
        if (executionId != null && !executionId.equals(execution.getExecutionId())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        if (matchAll) {
            return "*";
        }
        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix);
        } else {
            if (groupId != null) {
                sb.append(groupId);
            }
            sb.append(':');
            if (artifactId != null) {
                sb.append(artifactId);
            }
        }
        if (version != null) {
            sb.append(':').append(version);
        }
        if (goal != null) {
            sb.append(':').append(goal);
            if (executionId != null) {
                sb.append('@').append(executionId);
            }
        }
        return sb.toString();
    }
}
