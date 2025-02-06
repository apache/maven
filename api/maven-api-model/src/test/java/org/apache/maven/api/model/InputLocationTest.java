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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputLocationTest {

    @Test
    void testBasicLocationCreation() {
        int line = 10;
        int column = 20;
        InputSource source = InputSource.source("group:artifact:1.0", "pom.xml");

        InputLocation location = InputLocation.location(line, column, source);

        assertEquals(line, location.getLineNumber());
        assertEquals(column, location.getColumnNumber());
        assertEquals(source, location.getSource());
    }

    @Test
    void testLocationMergeWithoutSource() {
        InputLocation loc1 = InputLocation.location(1, 1);
        InputLocation loc2 = InputLocation.location(2, 2);

        InputLocation merged = InputLocation.merge(loc1, loc2, true);

        assertEquals(-1, merged.getLineNumber()); // Merged locations reset line/column
        assertEquals(-1, merged.getColumnNumber());
    }

    @Test
    void testLocationMergeWithIndices() {
        InputLocation loc1 = InputLocation.location(1, 1);
        InputLocation loc2 = InputLocation.location(2, 2);
        List<Integer> indices = Arrays.asList(0, 1, ~0);

        InputLocation merged = InputLocation.merge(loc1, loc2, indices);

        assertNotNull(merged);
        assertEquals(-1, merged.getLineNumber());
    }
}
