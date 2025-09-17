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

import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExclusionArtifactFilterTest {
    private Artifact artifact;
    private Artifact artifact2;

    @BeforeEach
    void setup() {
        artifact = mock(Artifact.class);
        when(artifact.getGroupId()).thenReturn("org.apache.maven");
        when(artifact.getArtifactId()).thenReturn("maven-core");

        artifact2 = mock(Artifact.class);
        when(artifact2.getGroupId()).thenReturn("org.junit.jupiter");
        when(artifact2.getArtifactId()).thenReturn("junit-jupiter-engine");
    }

    @Test
    void testExcludeExact() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("org.apache.maven");
        exclusion.setArtifactId("maven-core");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertFalse(filter.include(artifact), "Artifact should be excluded by exact match");
    }

    @Test
    void testExcludeNoMatch() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("org.apache.maven");
        exclusion.setArtifactId("maven-model");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertTrue(filter.include(artifact), "Artifact should not be excluded when no match");
    }

    @Test
    void testExcludeGroupIdWildcard() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("*");
        exclusion.setArtifactId("maven-core");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertFalse(filter.include(artifact), "Artifact should be excluded by groupId wildcard");
    }

    @Test
    void testExcludeGroupIdWildcardNoMatch() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("*");
        exclusion.setArtifactId("maven-compat");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertTrue(
                filter.include(artifact),
                "Artifact should not be excluded when groupId wildcard doesn't match artifactId");
    }

    @Test
    void testExcludeArtifactIdWildcard() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("org.apache.maven");
        exclusion.setArtifactId("*");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertFalse(filter.include(artifact), "Artifact should be excluded by artifactId wildcard");
    }

    @Test
    void testExcludeArtifactIdWildcardNoMatch() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("org.apache.groovy");
        exclusion.setArtifactId("*");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertTrue(
                filter.include(artifact),
                "Artifact should not be excluded when artifactId wildcard doesn't match groupId");
    }

    @Test
    void testExcludeAllWildcard() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("*");
        exclusion.setArtifactId("*");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertFalse(filter.include(artifact), "Artifact should be excluded by all wildcard");
    }

    @Test
    void testMultipleExclusionsExcludeArtifactIdWildcard() {
        Exclusion exclusion1 = new Exclusion();
        exclusion1.setGroupId("org.apache.groovy");
        exclusion1.setArtifactId("*");

        Exclusion exclusion2 = new Exclusion();
        exclusion2.setGroupId("org.apache.maven");
        exclusion2.setArtifactId("maven-core");

        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Arrays.asList(exclusion1, exclusion2));

        assertFalse(filter.include(artifact), "Artifact should be excluded by multiple exclusions");
    }

    @Test
    void testMultipleExclusionsExcludeGroupIdWildcard() {
        Exclusion exclusion1 = new Exclusion();
        exclusion1.setGroupId("*");
        exclusion1.setArtifactId("maven-model");

        Exclusion exclusion2 = new Exclusion();
        exclusion2.setGroupId("org.apache.maven");
        exclusion2.setArtifactId("maven-core");

        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Arrays.asList(exclusion1, exclusion2));

        assertFalse(
                filter.include(artifact), "Artifact should be excluded by multiple exclusions with groupId wildcard");
    }

    @Test
    void testExcludeWithGlob() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("*");
        exclusion.setArtifactId("maven-*");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertFalse(filter.include(artifact), "Maven artifact should be excluded by glob pattern");
        assertTrue(filter.include(artifact2), "JUnit artifact should not be excluded by maven-* glob pattern");
    }

    @Test
    void testExcludeWithGlobStar() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("**");
        exclusion.setArtifactId("maven-**");
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter(Collections.singletonList(exclusion));

        assertFalse(filter.include(artifact), "Maven artifact should be excluded by glob star pattern");
        assertTrue(filter.include(artifact2), "JUnit artifact should not be excluded by maven-** glob star pattern");
    }
}
