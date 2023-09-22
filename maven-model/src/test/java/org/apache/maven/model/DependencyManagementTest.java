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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@code DependencyManagement}.
 *
 */
class DependencyManagementTest {

    @Test
    void testHashCodeNullSafe() {
        new DependencyManagement().hashCode();
    }

    @Test
    void testEqualsNullSafe() {
        assertFalse(new DependencyManagement().equals(null));

        new DependencyManagement().equals(new DependencyManagement());
    }

    @Test
    void testEqualsIdentity() {
        DependencyManagement thing = new DependencyManagement();
        assertTrue(thing.equals(thing));
    }

    @Test
    void testToStringNullSafe() {
        assertNotNull(new DependencyManagement().toString());
    }

    @Test
    void testDependencies() {
        DependencyManagement dm = new DependencyManagement();
        Dependency d1 = new Dependency();
        d1.setGroupId("myGroupId");
        assertNotNull(dm.getDependencies());
        assertEquals(0, dm.getDependencies().size());
        dm.addDependency(d1);
        assertNotNull(dm.getDependencies());
        assertEquals(1, dm.getDependencies().size());
        dm.getDependencies().get(0).setArtifactId("myArtifactId");
        assertEquals("myArtifactId", dm.getDependencies().get(0).getArtifactId());
        dm.setDependencies(null);
        assertNotNull(dm.getDependencies());
        assertEquals(0, dm.getDependencies().size());
    }
}
