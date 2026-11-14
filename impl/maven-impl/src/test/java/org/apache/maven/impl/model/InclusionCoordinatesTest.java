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

import org.apache.maven.api.model.Inclusion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InclusionCoordinatesTest {

    @Test
    void testCoordinatesFromChildElements() {
        Inclusion inc = Inclusion.newBuilder()
                .groupId("org.apache.maven")
                .artifactId("maven-core")
                .build();
        assertEquals("org.apache.maven", InclusionCoordinates.getGroupId(inc));
        assertEquals("maven-core", InclusionCoordinates.getArtifactId(inc));
    }

    @Test
    void testCoordinatesFromCompactId() {
        Inclusion inc = Inclusion.newBuilder().id("org.apache.maven:maven-core").build();
        assertEquals("org.apache.maven", InclusionCoordinates.getGroupId(inc));
        assertEquals("maven-core", InclusionCoordinates.getArtifactId(inc));
    }

    @Test
    void testChildElementsTakePrecedenceOverId() {
        Inclusion inc = Inclusion.newBuilder()
                .id("other:id")
                .groupId("org.apache.maven")
                .artifactId("maven-core")
                .build();
        assertEquals("org.apache.maven", InclusionCoordinates.getGroupId(inc));
        assertEquals("maven-core", InclusionCoordinates.getArtifactId(inc));
    }

    @Test
    void testCompactIdWithoutColon() {
        Inclusion inc = Inclusion.newBuilder().id("invalid-format").build();
        assertEquals("invalid-format", InclusionCoordinates.getGroupId(inc));
        assertNull(InclusionCoordinates.getArtifactId(inc));
    }

    @Test
    void testEmptyInclusion() {
        Inclusion inc = Inclusion.newBuilder().build();
        assertNull(InclusionCoordinates.getGroupId(inc));
        assertNull(InclusionCoordinates.getArtifactId(inc));
    }
}
