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
package org.apache.maven.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputSourceTest {

    @Test
    void testSourceCreation() {
        String modelId = "group:artifact:1.0";
        String location = "pom.xml";

        InputSource source = InputSource.source(modelId, location);

        assertEquals(modelId, source.getModelId());
        assertEquals(location, source.getLocation());
        assertNull(source.getImportedFrom());
    }

    @Test
    void testSourceMerge() {
        InputSource src1 = InputSource.source("g1:a1:1.0", "pom1.xml");
        InputSource src2 = InputSource.source("g2:a2:1.0", "pom2.xml");

        InputSource merged = InputSource.merge(src1, src2);

        assertNull(merged.getModelId()); // Merged sources reset modelId
        assertNull(merged.getLocation()); // and location
        assertTrue(merged.toString().contains("merged")); // Should indicate merged state
    }

    @Test
    void testSourceEquality() {
        InputSource src1 = InputSource.source("g:a:1.0", "pom.xml");
        InputSource src2 = InputSource.source("g:a:1.0", "pom.xml");
        InputSource src3 = InputSource.source("g:a:2.0", "pom.xml");

        assertEquals(src1, src2);
        assertNotEquals(src1, src3);
    }
}
