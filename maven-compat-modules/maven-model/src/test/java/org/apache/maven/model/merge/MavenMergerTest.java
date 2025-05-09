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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

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
        assertThat(merged.getArtifactId(), is("SOURCE"));

        merged = mavenMerger.merge(target, source, false, null);
        assertThat(merged.getArtifactId(), is("TARGET"));
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

        assertThat(merged.getContributors(), contains(contributor));
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

        assertThat(merged.getDependencies(), contains(dependency));
    }
}
