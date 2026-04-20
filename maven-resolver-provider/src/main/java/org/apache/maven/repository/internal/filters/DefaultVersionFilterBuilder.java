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
package org.apache.maven.repository.internal.filters;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.VersionFilterBuilder;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.graph.version.ChainedVersionFilter;
import org.eclipse.aether.util.graph.version.ContextPredicateDelegatingVersionFilter;
import org.eclipse.aether.util.graph.version.ContextualSnapshotVersionFilter;
import org.eclipse.aether.util.graph.version.GenericQualifiersVersionFilter;
import org.eclipse.aether.util.graph.version.HighestVersionFilter;
import org.eclipse.aether.util.graph.version.LowestVersionFilter;
import org.eclipse.aether.util.graph.version.ReleaseVersionFilter;
import org.eclipse.aether.util.graph.version.SnapshotVersionFilter;
import org.eclipse.aether.util.graph.version.VersionPredicateVersionFilter;
import org.eclipse.aether.version.VersionConstraint;

import static java.util.Objects.requireNonNull;

/**
 * Builds {@link VersionFilter} instances out of input expression string.
 *
 * Expression is a semicolon separated list of filters to apply. By default, no version filter is applied (like in Maven 3).
 * <br/>
 * Supported filters:
 * <ul>
 *     <li>{@code "s"} - contextual snapshot filter (project version decides are snapshots allowed or not)</li>
 *     <li>{@code "nosnapshot"} - unconditional snapshot filter (no snapshot versions selected from ranges)</li>
 *     <li>{@code "norelease"} - unconditional release filter (no release versions selected from ranges)</li>
 *     <li>{@code "nopreview"} - unconditional preview filter (no preview versions selected from ranges)</li>
 *     <li>{@code "noprerelease"} - unconditional pre-release filter (no preview and rc/cr versions selected from ranges)</li>
 *     <li>{@code "noqualifier"} - unconditional any-qualifier filter (no version with any qualifier selected from ranges)</li>
 *     <li>{@code "h"} (shorthand of {@code h(1)}) or {@code "h(num)"} - highest N version (based on version ordering)</li>
 *     <li>{@code "l"} (shorthand of {@code l(1)}) or {@code "l(num)"} - lowest N version (based on version ordering)</li>
 *     <li>{@code "e(V)"} - exclusion filter (excludes versions matching V version constraint)</li>
 *     <li>{@code "i(V)"} - inclusion filter (includes versions matching V version constraint)</li>
 * </ul>
 * Every filter expression may have "scope" applied, in form of {@code @G[:A]}. Presence of "scope" narrows the
 * application of filter to given G or G:A.
 * <p>
 * In case of multiple "similar" rule scopes, user should enlist rules from "most specific" to "least specific".
 * <p>
 * Example filter expression: <code>"h(5);s;e(1)@org.foo:bar</code> will cause:
 * <ul>
 *     <li>ranges are filtered for "top 5" (instead of full range)</li>
 *     <li>snapshots are banned if root project is not a snapshot</li>
 *     <li>if range for <code>org.foo:bar</code> is being processed, version 1 is omitted</li>
 * </ul>
 * Values in this property builds <code>org.eclipse.aether.collection.VersionFilter</code> instance.
 *
 * @since 3.10.0
 */
@Singleton
@Named
public class DefaultVersionFilterBuilder implements VersionFilterBuilder {
    /**
     * Builds a version filter based on the given filter expression.
     *
     * @param filterExpression a string containing filter expressions, may be {@code null}.
     * @param versionConstraintParser version constraint parts to be used during parsing, must not be {@code null}.
     * @return optional version filter, never {@code null}.
     */
    @Override
    public Optional<VersionFilter> buildVersionFilter(
            String filterExpression, Function<String, VersionConstraint> versionConstraintParser) {
        requireNonNull(versionConstraintParser);
        ArrayList<VersionFilter> filters = new ArrayList<>();
        if (filterExpression != null) {
            List<String> expressions = Arrays.stream(filterExpression.split(";"))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(Collectors.toList());
            for (String expression : expressions) {
                Predicate<Artifact> scopePredicate;
                VersionFilter filter;
                if (expression.contains("@")) {
                    String remainder = expression.substring(expression.indexOf('@') + 1);
                    if (remainder.contains(":")) {
                        String g = remainder.substring(0, remainder.indexOf(':'));
                        String a = remainder.substring(remainder.indexOf(':') + 1);
                        scopePredicate =
                                artifact -> g.equals(artifact.getGroupId()) && a.equals(artifact.getArtifactId());
                    } else {
                        scopePredicate = artifact -> remainder.equals(artifact.getGroupId());
                    }
                    expression = expression.substring(0, expression.indexOf('@'));
                } else {
                    scopePredicate = null;
                }
                if ("s".equals(expression)) {
                    filter = new ContextualSnapshotVersionFilter();
                } else if ("nosnapshot".equals(expression)) {
                    filter = new SnapshotVersionFilter();
                } else if ("norelease".equals(expression)) {
                    filter = new ReleaseVersionFilter();
                } else if ("nopreview".equals(expression)) {
                    filter = GenericQualifiersVersionFilter.previewVersionFilter();
                } else if ("noprerelease".equals(expression)) {
                    filter = GenericQualifiersVersionFilter.preReleaseVersionFilter();
                } else if ("noqualifier".equals(expression)) {
                    filter = GenericQualifiersVersionFilter.anyQualifierVersionFilter();
                } else if ("h".equals(expression)) {
                    filter = new HighestVersionFilter();
                } else if ("l".equals(expression)) {
                    filter = new LowestVersionFilter();
                } else if ((expression.startsWith("h(") || expression.startsWith("l(")) && expression.endsWith(")")) {
                    int num = Integer.parseInt(expression.substring(2, expression.length() - 1));
                    if (expression.startsWith("h(")) {
                        filter = new HighestVersionFilter(num);
                    } else {
                        filter = new LowestVersionFilter(num);
                    }
                } else if ((expression.startsWith("e(") || (expression.startsWith("i("))) && expression.endsWith(")")) {
                    VersionConstraint versionConstraint =
                            versionConstraintParser.apply(expression.substring(2, expression.length() - 1));
                    if (expression.startsWith("e(")) {
                        // exclude
                        filter = new VersionPredicateVersionFilter(v -> !versionConstraint.containsVersion(v));
                    } else {
                        // include
                        filter = new VersionPredicateVersionFilter(versionConstraint::containsVersion);
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported filter expression: " + expression);
                }

                filters.add(contextPredicate(scopePredicate, filter));
            }
        }
        if (filters.isEmpty()) {
            return Optional.empty();
        } else if (filters.size() == 1) {
            return Optional.of(filters.get(0));
        } else {
            return Optional.of(ChainedVersionFilter.newInstance(filters));
        }
    }

    private VersionFilter contextPredicate(Predicate<Artifact> artifactPredicate, VersionFilter filter) {
        Predicate<VersionFilter.VersionFilterContext> contextPredicate =
                c -> !ConfigUtils.getBoolean(c.getSession(), false, MAVEN_VERSION_FILTER_SUPPRESSED);
        if (artifactPredicate != null) {
            contextPredicate = contextPredicate.and(
                    c -> artifactPredicate.test(c.getDependency().getArtifact()));
        }
        return new ContextPredicateDelegatingVersionFilter(contextPredicate, filter);
    }
}
