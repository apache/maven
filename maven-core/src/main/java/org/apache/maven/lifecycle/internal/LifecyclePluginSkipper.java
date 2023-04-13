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
package org.apache.maven.lifecycle.internal;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Lifecycle plugin skipper.
 *
 * @since TBD
 */
@Component(role = LifecyclePluginSkipper.class)
public class LifecyclePluginSkipper {
    private static final String PARSED_FILTER_KEY = LifecyclePluginSkipper.class.getName() + ".filter";
    private static final String MAVEN_LIFECYCLE_FILTER = "maven.lifecycle.filter";
    private static final Predicate<MojoExecution> NO_FILTER = t -> true;
    private static final Predicate<String> TRUE = t -> true;

    void processMojoExecutions(MavenSession session, List<MojoExecution> executions) {
        Predicate<MojoExecution> filter = getFilter(session);
        if (filter == NO_FILTER) {
            return;
        }
        executions.removeIf(filter);
    }

    @SuppressWarnings("unchecked")
    private Predicate<MojoExecution> getFilter(MavenSession mavenSession) {
        return (Predicate<MojoExecution>) mavenSession
                .getRepositorySession()
                .getData()
                .computeIfAbsent(PARSED_FILTER_KEY, () -> createFilter(mavenSession));
    }

    private Predicate<MojoExecution> createFilter(MavenSession mavenSession) {
        String filterString = ConfigUtils.getString(mavenSession.getRepositorySession(), null, MAVEN_LIFECYCLE_FILTER);
        if (filterString == null) {
            return NO_FILTER;
        }

        Predicate<MojoExecution> result = null;
        String[] filterExpressions = filterString.split(",");
        for (String filterExpression : filterExpressions) {
            if (result == null) {
                result = parseFilterExpression(mavenSession.getPluginGroups(), filterExpression);
            } else {
                result = result.or(parseFilterExpression(mavenSession.getPluginGroups(), filterExpression));
            }
        }
        return result;
    }

    private Predicate<MojoExecution> parseFilterExpression(List<String> pluginGroups, String filterExpression) {
        // G:A:Goal:ExecId
        //
        // G values
        // "" - default plugins (org.apache org codehaus)
        // "*" - any
        // "foo.bar" - exact G match
        //
        // A values
        // "*" - any
        // "foo-maven-plugin" - exact A match
        //
        // Goal values
        // "*" - any
        // "foo" - exact match
        //
        // ExecId values
        // "*" - any
        // "foo" - exact match

        Predicate<String> groupIdPredicate;
        Predicate<String> artifactIdPredicate = TRUE;
        Predicate<String> goalPredicate = TRUE;
        Predicate<String> executionIdPredicate = TRUE;

        String[] elements = filterExpression.split(":");
        if (elements.length == 0 || elements.length > 4) {
            throw new IllegalArgumentException("Unsupported lifecycle filter expression: " + filterExpression);
        }
        String groupId = elements[0];
        if (Objects.equals(groupId, "")) {
            groupIdPredicate = pluginGroups::contains;
        } else if (Objects.equals(groupId, "*")) {
            groupIdPredicate = TRUE;
        } else {
            groupIdPredicate = t -> Objects.equals(groupId, t);
        }

        if (elements.length > 1) {
            String artifactId = elements[1];
            if (!Objects.equals(artifactId, "*")) {
                artifactIdPredicate = t -> Objects.equals(artifactId, t);
            }

            if (elements.length > 2) {
                String goal = elements[2];
                if (!Objects.equals(goal, "*")) {
                    goalPredicate = t -> Objects.equals(goal, t);
                }

                if (elements.length > 3) {
                    String executionId = elements[3];
                    if (!Objects.equals(executionId, "*")) {
                        executionIdPredicate = t -> Objects.equals(executionId, t);
                    }
                }
            }
        }

        return new ExecutionMatcher(groupIdPredicate, artifactIdPredicate, goalPredicate, executionIdPredicate);
    }

    private static class ExecutionMatcher implements Predicate<MojoExecution> {
        private final Predicate<String> groupIdPredicate;
        private final Predicate<String> artifactIdPredicate;
        private final Predicate<String> goalPredicate;
        private final Predicate<String> executionIdPredicate;

        private ExecutionMatcher(
                Predicate<String> groupIdPredicate,
                Predicate<String> artifactIdPredicate,
                Predicate<String> goalPredicate,
                Predicate<String> executionIdPredicate) {
            this.groupIdPredicate = groupIdPredicate;
            this.artifactIdPredicate = artifactIdPredicate;
            this.goalPredicate = goalPredicate;
            this.executionIdPredicate = executionIdPredicate;
        }

        @Override
        public boolean test(MojoExecution mojoExecution) {
            return groupIdPredicate.test(mojoExecution
                            .getMojoDescriptor()
                            .getPluginDescriptor()
                            .getGroupId())
                    && artifactIdPredicate.test(mojoExecution
                            .getMojoDescriptor()
                            .getPluginDescriptor()
                            .getArtifactId())
                    && goalPredicate.test(mojoExecution.getMojoDescriptor().getGoal())
                    && executionIdPredicate.test(mojoExecution.getExecutionId());
        }
    }
}
