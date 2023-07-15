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
package org.apache.maven.artifact.resolver.filter;

import java.lang.reflect.Proxy;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;

/**
 * Filter to exclude from a list of artifact patterns.
 */
public class ExclusionArtifactFilter implements ArtifactFilter {

    private final List<Exclusion> exclusions;
    private final List<Predicate<Artifact>> predicates;

    public ExclusionArtifactFilter(List<Exclusion> exclusions) {
        this.exclusions = exclusions;
        this.predicates =
                exclusions.stream().map(ExclusionArtifactFilter::toPredicate).collect(Collectors.toList());
    }

    @Override
    public boolean include(Artifact artifact) {
        return predicates.stream().noneMatch(p -> p.test(artifact));
    }

    private static Predicate<Artifact> toPredicate(Exclusion exclusion) {
        PathMatcher groupId = FileSystems.getDefault().getPathMatcher("glob:" + exclusion.getGroupId());
        PathMatcher artifactId = FileSystems.getDefault().getPathMatcher("glob:" + exclusion.getArtifactId());
        Predicate<Artifact> predGroupId = a -> groupId.matches(createPathProxy(a.getGroupId()));
        Predicate<Artifact> predArtifactId = a -> artifactId.matches(createPathProxy(a.getArtifactId()));
        return predGroupId.and(predArtifactId);
    }

    /**
     * In order to reuse the glob matcher from the filesystem, we need
     * to create Path instances.  Those are only used with the toString method.
     * This hack works because the only system-dependent thing is the path
     * separator which should not be part of the groupId or artifactId.
     */
    private static Path createPathProxy(String value) {
        return (Path) Proxy.newProxyInstance(
                ExclusionArtifactFilter.class.getClassLoader(), new Class[] {Path.class}, (proxy1, method, args) -> {
                    if ("toString".equals(method.getName())) {
                        return value;
                    }
                    throw new UnsupportedOperationException();
                });
    }
}
