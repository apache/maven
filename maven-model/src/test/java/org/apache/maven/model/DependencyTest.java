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
package org.apache.maven.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@code Dependency}.
 *
 */
class DependencyTest {

    private Dependency dependency;

    @BeforeEach
    public void setUp() {
        dependency = new Dependency();
        dependency.setGroupId("groupId");
        dependency.setArtifactId("artifactId");
        dependency.setVersion("1.0");
    }

    @Test
    void testHashCodeNullSafe() {
        new Dependency().hashCode();
    }

    @Test
    void testEqualsNullSafe() {
        assertFalse(new Dependency().equals(null));

        new Dependency().equals(new Dependency());
    }

    @Test
    void testEqualsIdentity() {
        Dependency thing = new Dependency();
        assertTrue(thing.equals(thing));
    }

    @Test
    void testToStringNullSafe() {
        assertNotNull(new Dependency().toString());
    }

    @Test
    public void testDependencyExclusions() {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("excludedGroupId");
        exclusion.setArtifactId("excludedArtifactId");

        dependency.addExclusion(exclusion);

        assertEquals(1, dependency.getExclusions().size());
        Exclusion addedExclusion = dependency.getExclusions().get(0);
        assertEquals("excludedGroupId", addedExclusion.getGroupId());
        assertEquals("excludedArtifactId", addedExclusion.getArtifactId());
    }
}
