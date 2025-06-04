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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for MNG-8746: Properties order is preserved in model
 */
public class PropertiesOrderTest {

    @Test
    public void testPropertiesOrderPreservedInImmutableModel() {
        // Create properties with specific insertion order using LinkedHashMap
        Map<String, String> orderedMap = new LinkedHashMap<>();
        orderedMap.put("third", "3");
        orderedMap.put("first", "1");
        orderedMap.put("second", "2");

        // Create model and set properties
        Model model = new Model();
        Properties props = model.getProperties();

        // Create properties and populate from map to maintain order
        orderedMap.forEach(props::setProperty);

        // Get the immutable delegate (v4 API model is already immutable)
        org.apache.maven.api.model.Model immutable = model.getDelegate();

        // Verify order is preserved
        Map<String, String> resultProps = immutable.getProperties();
        assertNotNull(resultProps);

        // Check order by collecting keys in iteration order
        List<String> keys = new ArrayList<>(resultProps.keySet());

        // Verify the original insertion order is maintained
        assertEquals(3, keys.size());
        assertEquals("third", keys.get(0));
        assertEquals("first", keys.get(1));
        assertEquals("second", keys.get(2));
    }

    @Test
    public void testPropertiesOrderPreservedInMutableModel() {
        // Create ordered map to simulate properties with specific order
        Map<String, String> orderedMap = new LinkedHashMap<>();
        orderedMap.put("z-property", "z");
        orderedMap.put("a-property", "a");
        orderedMap.put("m-property", "m");

        // Create and populate model
        Model model = new Model();
        Properties props = model.getProperties();

        // Create properties and populate from map to maintain order
        orderedMap.forEach(props::setProperty);

        // Get properties back and verify order
        Properties resultProps = model.getProperties();

        // Check order by collecting keys in iteration order
        List<String> keys = new ArrayList<>();
        resultProps.keySet().forEach(k -> keys.add(k.toString()));

        // Verify the original insertion order is maintained
        assertEquals(3, keys.size());
        assertEquals("z-property", keys.get(0));
        assertEquals("a-property", keys.get(1));
        assertEquals("m-property", keys.get(2));
    }
}
