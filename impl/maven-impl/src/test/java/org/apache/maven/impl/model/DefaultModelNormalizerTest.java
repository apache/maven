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
    void testExpandDependencyId2parts() {
        Dependency dep = Dependency.newBuilder(false).id("org.example:lib").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib", result.getArtifactId());
        assertNull(result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyId2partsTrailingColon() {
        Dependency dep = Dependency.newBuilder(false).id("org.example:lib:").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib", result.getArtifactId());
        assertNull(result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyId3parts() {
        Dependency dep =
                Dependency.newBuilder(false).id("org.slf4j:slf4j-api:2.0.17").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.slf4j", result.getGroupId());
        assertEquals("slf4j-api", result.getArtifactId());
        assertEquals("2.0.17", result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyId4parts() {
        Dependency dep =
                Dependency.newBuilder(false).id("org.example:lib-b:pom:1.0").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib-b", result.getArtifactId());
        assertEquals("pom", result.getType());
        assertEquals("1.0", result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyId4partsTrailingColon() {
        Dependency dep =
                Dependency.newBuilder(false).id("org.example:lib-b:pom:").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib-b", result.getArtifactId());
        assertEquals("pom", result.getType());
        assertNull(result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyId5parts() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.example:lib-c:jar:sources:1.0")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib-c", result.getArtifactId());
        assertEquals("jar", result.getType());
        assertEquals("sources", result.getClassifier());
        assertEquals("1.0", result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyId5partsTrailingColon() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.example:lib-c:jar:sources:")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib-c", result.getArtifactId());
        assertEquals("jar", result.getType());
        assertEquals("sources", result.getClassifier());
        assertNull(result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyIdDoesNotOverrideExistingFields() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.slf4j:slf4j-api:2.0.17")
                .groupId("org.override")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.override", result.getGroupId());
        assertEquals("slf4j-api", result.getArtifactId());
        assertEquals("2.0.17", result.getVersion());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyIdInvalidFormat() {
        Dependency dep = Dependency.newBuilder(false).id("invalid-no-colons").build();

        Dependency result = normalizer.expandDependencyId(dep);

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

        assertEquals("org.example", result.getGroupId());
        assertEquals("my-lib", result.getArtifactId());
        assertEquals("1.0", result.getVersion());
    }

    @Test
    void testExpandDependencyIdAlsoExpandsExclusions() {
        Exclusion exc = Exclusion.newBuilder(false).id("com.example:unwanted").build();
        Dependency dep = Dependency.newBuilder(false)
                .id("org.slf4j:slf4j-api:2.0.17")
                .exclusions(List.of(exc))
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.slf4j", result.getGroupId());
        assertEquals("slf4j-api", result.getArtifactId());
        assertEquals("2.0.17", result.getVersion());
        assertNull(result.getId());
        assertEquals(1, result.getExclusions().size());
        assertEquals("com.example", result.getExclusions().get(0).getGroupId());
        assertEquals("unwanted", result.getExclusions().get(0).getArtifactId());
        assertNull(result.getExclusions().get(0).getId());
    }

    // ===== @scope and ? (optional) tests =====

    @Test
    void testExpandDependencyIdWithScope() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.junit.jupiter:junit-jupiter-api:5.0@test")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.junit.jupiter", result.getGroupId());
        assertEquals("junit-jupiter-api", result.getArtifactId());
        assertEquals("5.0", result.getVersion());
        assertEquals("test", result.getScope());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyIdWithOptional() {
        Dependency dep =
                Dependency.newBuilder(false).id("commons-io:commons-io:2.11.0?").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("commons-io", result.getGroupId());
        assertEquals("commons-io", result.getArtifactId());
        assertEquals("2.11.0", result.getVersion());
        assertEquals("true", result.getOptional());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyIdWithScopeAndOptional() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.apache.maven:maven-core:3.9.0@provided?")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.apache.maven", result.getGroupId());
        assertEquals("maven-core", result.getArtifactId());
        assertEquals("3.9.0", result.getVersion());
        assertEquals("provided", result.getScope());
        assertEquals("true", result.getOptional());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyIdWithImportScope() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.junit:junit-bom:5.12.0@import")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.junit", result.getGroupId());
        assertEquals("junit-bom", result.getArtifactId());
        assertEquals("5.12.0", result.getVersion());
        assertEquals("import", result.getScope());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyIdScopeDoesNotOverrideExisting() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.junit.jupiter:junit-jupiter-api:5.0@test")
                .scope("compile")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("compile", result.getScope());
    }

    @Test
    void testExpandDependencyIdOptionalDoesNotOverrideExisting() {
        Dependency dep = Dependency.newBuilder(false)
                .id("commons-io:commons-io:2.11.0?")
                .optional("false")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("false", result.getOptional());
    }

    @Test
    void testExpandDependencyIdPlainGavNoScopeOrOptional() {
        Dependency dep =
                Dependency.newBuilder(false).id("org.slf4j:slf4j-api:2.0.17").build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.slf4j", result.getGroupId());
        assertEquals("slf4j-api", result.getArtifactId());
        assertEquals("2.0.17", result.getVersion());
        assertNull(result.getScope());
        assertNull(result.getOptional());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyIdManagedWithScope() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.junit.jupiter:junit-jupiter-api@test")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.junit.jupiter", result.getGroupId());
        assertEquals("junit-jupiter-api", result.getArtifactId());
        assertNull(result.getVersion());
        assertEquals("test", result.getScope());
        assertNull(result.getId());
    }

    @Test
    void testExpandDependencyId4partsWithScope() {
        Dependency dep = Dependency.newBuilder(false)
                .id("org.example:lib:pom:1.0@import")
                .build();

        Dependency result = normalizer.expandDependencyId(dep);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib", result.getArtifactId());
        assertEquals("pom", result.getType());
        assertEquals("1.0", result.getVersion());
        assertEquals("import", result.getScope());
        assertNull(result.getId());
    }

    // ===== Exclusion tests =====

    @Test
    void testExpandExclusionId() {
        Exclusion exc =
                Exclusion.newBuilder(false).id("com.example:unwanted-lib").build();

        Exclusion result = normalizer.expandExclusionId(exc);

        assertEquals("com.example", result.getGroupId());
        assertEquals("unwanted-lib", result.getArtifactId());
        assertNull(result.getId());
    }

    @Test
    void testExpandExclusionIdWildcard() {
        Exclusion exc = Exclusion.newBuilder(false).id("*:*").build();

        Exclusion result = normalizer.expandExclusionId(exc);

        assertEquals("*", result.getGroupId());
        assertEquals("*", result.getArtifactId());
        assertNull(result.getId());
    }

    @Test
    void testExpandExclusionIdNull() {
        Exclusion exc =
                Exclusion.newBuilder().groupId("org.example").artifactId("lib").build();

        Exclusion result = normalizer.expandExclusionId(exc);

        assertEquals("org.example", result.getGroupId());
        assertEquals("lib", result.getArtifactId());
    }

    // ===== Mixin tests =====

    @Test
    void testExpandMixinGav() {
        Mixin mixin = Mixin.newBuilder(false)
                .gav("com.example.mixins:java-mixin:1.0.0")
                .build();

        Mixin result = normalizer.expandMixinGav(mixin);

        assertEquals("com.example.mixins", result.getGroupId());
        assertEquals("java-mixin", result.getArtifactId());
        assertEquals("1.0.0", result.getVersion());
        assertNull(result.getGav());
    }

    @Test
    void testExpandMixinGavNull() {
        Mixin mixin = Mixin.newBuilder()
                .groupId("com.example")
                .artifactId("my-mixin")
                .version("2.0")
                .build();

        Mixin result = normalizer.expandMixinGav(mixin);

        assertEquals("com.example", result.getGroupId());
        assertEquals("my-mixin", result.getArtifactId());
        assertEquals("2.0", result.getVersion());
    }

    // ===== Integration tests =====

    @Test
    void testMergeDuplicatesExpandsDependencyManagement() {
        Dependency dep =
                Dependency.newBuilder(false).id("org.slf4j:slf4j-api:2.0.17").build();
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
        assertNull(expanded.getId());
    }

    @Test
    void testMergeDuplicatesExpandsProfileDependencies() {
        Dependency dep =
                Dependency.newBuilder(false).id("org.slf4j:slf4j-api:2.0.17").build();
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
        assertNull(expanded.getId());
    }

    @Test
    void testMergeDuplicatesExpandsProfileDependencyManagement() {
        Dependency dep =
                Dependency.newBuilder(false).id("org.slf4j:slf4j-api:2.0.17").build();
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
        assertNull(expanded.getId());
    }
}
