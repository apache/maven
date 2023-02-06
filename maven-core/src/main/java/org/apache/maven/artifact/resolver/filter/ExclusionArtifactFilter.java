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

import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;

/**
 * Filter to exclude from a list of artifact patterns.
 */
public class ExclusionArtifactFilter implements ArtifactFilter {
    private static final String WILDCARD = "*";

    private final List<Exclusion> exclusions;

    public ExclusionArtifactFilter(List<Exclusion> exclusions) {
        this.exclusions = exclusions;
    }

    private Predicate<Exclusion> sameArtifactId(Artifact artifact) {
        return exclusion -> exclusion.getArtifactId().equals(artifact.getArtifactId());
    }

    private Predicate<Exclusion> sameGroupId(Artifact artifact) {
        return exclusion -> exclusion.getGroupId().equals(artifact.getGroupId());
    }

    private Predicate<Exclusion> groupIdIsWildcard = exclusion -> WILDCARD.equals(exclusion.getGroupId());

    private Predicate<Exclusion> artifactIdIsWildcard = exclusion -> WILDCARD.equals(exclusion.getArtifactId());

    private Predicate<Exclusion> groupIdAndArtifactIdIsWildcard = groupIdIsWildcard.and(artifactIdIsWildcard);

    private Predicate<Exclusion> exclude(Artifact artifact) {
        return groupIdAndArtifactIdIsWildcard
                .or(groupIdIsWildcard.and(sameArtifactId(artifact)))
                .or(artifactIdIsWildcard.and(sameGroupId(artifact)))
                .or(sameGroupId(artifact).and(sameArtifactId(artifact)));
    }

    @Override
    public boolean include(Artifact artifact) {
        return !exclusions.stream().anyMatch(exclude(artifact));
    }
}
