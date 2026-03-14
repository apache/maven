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
package org.apache.maven.model.merge;

import java.util.Arrays;

import org.apache.maven.api.model.Contributor;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenMerger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MavenMerger is based on same instances, subclasses should override KeyComputer per type
 */
class MavenMergerTest {
    private MavenMerger mavenMerger = new MavenMerger();

    @Test
    void mergeArtifactId() {
        Model target = Model.newBuilder().artifactId("TARGET").build();

        Model source = Model.newBuilder().artifactId("SOURCE").build();

        Model merged = mavenMerger.merge(target, source, true, null);
        assertEquals(
                "SOURCE",
                merged.getArtifactId(),
                "Expected merged artifactId to be SOURCE but was " + merged.getArtifactId());

        merged = mavenMerger.merge(target, source, false, null);
        assertEquals(
                "TARGET",
                merged.getArtifactId(),
                "Expected merged artifactId to be TARGET but was " + merged.getArtifactId());
    }

    @Test
    void mergeSameContributors() {
        Contributor contributor =
                Contributor.newBuilder().email("contributor@maven.apache.org").build();

        Model target =
                Model.newBuilder().contributors(Arrays.asList(contributor)).build();

        Model source =
                Model.newBuilder().contributors(Arrays.asList(contributor)).build();

        Model merged = mavenMerger.merge(target, source, true, null);

        assertEquals(1, merged.getContributors().size(), "Expected exactly 1 contributor");
        assertTrue(
                merged.getContributors().contains(contributor),
                "Expected contributors to contain " + contributor + " but was " + merged.getContributors());
    }

    @Test
    void mergeSameDependencies() {
        Dependency dependency = Dependency.newBuilder()
                .groupId("groupId")
                .artifactId("artifactId")
                .type("type")
                .build();

        Model target =
                Model.newBuilder().dependencies(Arrays.asList(dependency)).build();

        Model source =
                Model.newBuilder().dependencies(Arrays.asList(dependency)).build();

        Model merged = mavenMerger.merge(target, source, true, null);

        assertEquals(1, merged.getDependencies().size(), "Expected exactly 1 dependency");
        assertTrue(
                merged.getDependencies().contains(dependency),
                "Expected dependencies to contain " + dependency + " but was " + merged.getDependencies());
    }
}
