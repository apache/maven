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
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for order preservation in WrapperProperties used by Model
 */
public class WrapperPropertiesOrderTest {

    @Test
    public void testOrderPreservationAfterModification() {
        // Create a model with properties
        Model model = new Model();
        Properties modelProps = model.getProperties();

        // Add properties in specific order
        modelProps.setProperty("first", "1");
        modelProps.setProperty("second", "2");

        // Modify existing property
        modelProps.setProperty("first", "modified");

        // Add new property
        modelProps.setProperty("third", "3");

        // Collect keys in iteration order
        List<String> keys = new ArrayList<>();
        modelProps.keySet().forEach(k -> keys.add(k.toString()));

        // Verify order is preserved (first should still be first since it was modified, not re-added)
        assertEquals(3, keys.size());
        assertEquals("first", keys.get(0));
        assertEquals("second", keys.get(1));
        assertEquals("third", keys.get(2));

        // Verify value was updated
        assertEquals("modified", modelProps.getProperty("first"));
    }
}
