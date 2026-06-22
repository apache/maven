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
package org.apache.maven.impl.model;

import java.util.List;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.Mixin;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultModelNormalizerTest {

    private final DefaultModelNormalizer normalizer = new DefaultModelNormalizer();

    @Test
    void testExpandDependencyId() {
        Dependency dep =
                Dependency.newBuilder().id("org.slf4j:slf4j-api:2.0.17").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.slf4j", result.getGroupId());
        assertEquals("slf4j-api", result.getArtifactId());
        assertEquals("2.0.17", result.getVersion());
        assertEquals("org.slf4j:slf4j-api:2.0.17", result.getId());
    }

    @Test
    void testExpandDependencyIdDoesNotOverrideExistingFields() {
        Dependency dep = Dependency.newBuilder()
                .id("org.slf4j:slf4j-api:2.0.17")
                .groupId("org.override")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        // Existing groupId should not be overridden
        assertEquals("org.override", result.getGroupId());
        assertEquals("slf4j-api", result.getArtifactId());
        assertEquals("2.0.17", result.getVersion());
    }

    @Test
    void testExpandDependencyIdInvalidFormat() {
        Dependency dep = Dependency.newBuilder().id("invalid-no-colons").build();

        Dependency result = normalizer.expandDependencyId(dep);

        // Invalid format — fields not populated, validator will catch this
        assertNull(result.getGroupId());
        assertNull(result.getArtifactId());
    }

    @Test
    void testExpandDependencyIdNull() {
        Dependency dep = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("my-lib")
                .version("1.0")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        // No id attribute, should return unchanged
        assertEquals("org.example", result.getGroupId());
        assertEquals("my-lib", result.getArtifactId());
        assertEquals("1.0", result.getVersion());
    }

    @Test
    void testExpandExclusionId() {
        Exclusion exc = Exclusion.newBuilder().id("com.example:unwanted-lib").build();

        Exclusion result = normalizer.expandExclusionId(exc);

        assertEquals("com.example", result.getGroupId());
        assertEquals("unwanted-lib", result.getArtifactId());
    }

    @Test
    void testExpandExclusionIdWildcard() {
        Exclusion exc = Exclusion.newBuilder().id("*:*").build();

        Exclusion result = normalizer.expandExclusionId(exc);

        assertEquals("*", result.getGroupId());
        assertEquals("*", result.getArtifactId());
    }

    @Test
    void testExpandExclusionIdNull() {
        Exclusion exc =
                Exclusion.newBuilder().groupId("org.example").artifactId("lib").build();

        Exclusion result = normalizer.expandExclusionId(exc);

        // No id attribute, should return same instance
        assertEquals("org.example", result.getGroupId());
        assertEquals("lib", result.getArtifactId());
    }

    @Test
    void testExpandMixinGav() {
        Mixin mixin =
                Mixin.newBuilder().gav("com.example.mixins:java-mixin:1.0.0").build();

        Mixin result = normalizer.expandMixinGav(mixin);

        assertEquals("com.example.mixins", result.getGroupId());
        assertEquals("java-mixin", result.getArtifactId());
        assertEquals("1.0.0", result.getVersion());
    }

    @Test
    void testExpandMixinGavNull() {
        Mixin mixin = Mixin.newBuilder()
                .groupId("com.example")
                .artifactId("my-mixin")
                .version("2.0")
                .build();

        Mixin result = normalizer.expandMixinGav(mixin);

        // No gav attribute, should return same instance
        assertEquals("com.example", result.getGroupId());
        assertEquals("my-mixin", result.getArtifactId());
        assertEquals("2.0", result.getVersion());
    }

    @Test
    void testExpandDependencyIdAlsoExpandsExclusions() {
        Exclusion exc = Exclusion.newBuilder().id("com.example:unwanted").build();
        Dependency dep = Dependency.newBuilder()
                .id("org.slf4j:slf4j-api:2.0.17")
                .exclusions(List.of(exc))
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.slf4j", result.getGroupId());
        assertEquals("slf4j-api", result.getArtifactId());
        assertEquals("2.0.17", result.getVersion());
        assertEquals(1, result.getExclusions().size());
        assertEquals("com.example", result.getExclusions().get(0).getGroupId());
        assertEquals("unwanted", result.getExclusions().get(0).getArtifactId());
    }

    @Test
    void testMergeDuplicatesExpandsDependencyManagement() {
        Dependency dep =
                Dependency.newBuilder().id("org.slf4j:slf4j-api:2.0.17").build();
        Model model = Model.newBuilder()
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(dep))
                        .build())
                .build();

        Model result = normalizer.mergeDuplicates(model, null, null);

        assertEquals(1, result.getDependencyManagement().getDependencies().size());
        Dependency expanded = result.getDependencyManagement().getDependencies().get(0);
        assertEquals("org.slf4j", expanded.getGroupId());
        assertEquals("slf4j-api", expanded.getArtifactId());
        assertEquals("2.0.17", expanded.getVersion());
    }

    @Test
    void testMergeDuplicatesExpandsProfileDependencies() {
        Dependency dep =
                Dependency.newBuilder().id("org.slf4j:slf4j-api:2.0.17").build();
        Profile profile = Profile.newBuilder()
                .id("test-profile")
                .dependencies(List.of(dep))
                .build();
        Model model = Model.newBuilder().profiles(List.of(profile)).build();

        Model result = normalizer.mergeDuplicates(model, null, null);

        assertEquals(1, result.getProfiles().size());
        assertEquals(1, result.getProfiles().get(0).getDependencies().size());
        Dependency expanded = result.getProfiles().get(0).getDependencies().get(0);
        assertEquals("org.slf4j", expanded.getGroupId());
        assertEquals("slf4j-api", expanded.getArtifactId());
        assertEquals("2.0.17", expanded.getVersion());
    }

    @Test
    void testMergeDuplicatesExpandsProfileDependencyManagement() {
        Dependency dep =
                Dependency.newBuilder().id("org.slf4j:slf4j-api:2.0.17").build();
        Profile profile = Profile.newBuilder()
                .id("test-profile")
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(dep))
                        .build())
                .build();
        Model model = Model.newBuilder().profiles(List.of(profile)).build();

        Model result = normalizer.mergeDuplicates(model, null, null);

        assertEquals(1, result.getProfiles().size());
        DependencyManagement mgmt = result.getProfiles().get(0).getDependencyManagement();
        assertEquals(1, mgmt.getDependencies().size());
        Dependency expanded = mgmt.getDependencies().get(0);
        assertEquals("org.slf4j", expanded.getGroupId());
        assertEquals("slf4j-api", expanded.getArtifactId());
        assertEquals("2.0.17", expanded.getVersion());
    }
}
