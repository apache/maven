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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Comprehensive test suite for Properties behavior in Maven models.
 * Tests order preservation, caching behavior, and WrapperProperties functionality.
 */
class PropertiesTest {

    @Nested
    class OrderPreservationTests {

        @Test
        void testPropertiesOrderPreservedInImmutableModel() {
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
        void testPropertiesOrderPreservedInMutableModel() {
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

        @Test
        void testOrderPreservationAfterModification() {
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

    @Nested
    class WrapperPropertiesBehaviorTests {

        @Test
        void testWriteOperationBehavior() {
            // Create a Model with initial properties
            Model model = new Model();

            // Set initial properties using setProperties to establish the backend
            Properties initialProps = new Properties();
            initialProps.setProperty("initial.key", "initial.value");
            model.setProperties(initialProps);

            // Get the WrapperProperties instance
            Properties wrapperProps = model.getProperties();

            // First read - should initialize cache
            assertEquals("initial.value", wrapperProps.getProperty("initial.key"));

            // Simulate external change by directly calling setProperties (another WrapperProperties instance)
            Properties externalProps = new Properties();
            externalProps.setProperty("initial.key", "externally.modified");
            externalProps.setProperty("external.key", "external.value");
            model.setProperties(externalProps);

            // Read again - should return fresh value (no caching in current implementation)
            assertEquals("externally.modified", wrapperProps.getProperty("initial.key"));

            // Now perform a write operation
            wrapperProps.setProperty("new.key", "new.value");

            // Read the initial key again - should return the current value
            assertEquals("externally.modified", wrapperProps.getProperty("initial.key"));

            // Read the external key that was set before the write operation
            assertEquals("external.value", wrapperProps.getProperty("external.key"));

            // Read the new key that was just set
            assertEquals("new.value", wrapperProps.getProperty("new.key"));
        }

        @Test
        void testMultipleWrapperPropertiesShareSameBackend() {
            // Create a Model with initial properties
            Model model = new Model();

            Properties initialProps = new Properties();
            initialProps.setProperty("shared.key", "initial.value");
            model.setProperties(initialProps);

            // Get two WrapperProperties instances from the same Model
            Properties wrapper1 = model.getProperties();
            Properties wrapper2 = model.getProperties();

            // Both wrappers should read the same initial value
            assertEquals("initial.value", wrapper1.getProperty("shared.key"));
            assertEquals("initial.value", wrapper2.getProperty("shared.key"));

            // Write through wrapper1
            wrapper1.setProperty("from.wrapper1", "value1");

            // wrapper2 should see the changes immediately (no caching)
            assertEquals("value1", wrapper2.getProperty("from.wrapper1"));
            assertEquals("initial.value", wrapper2.getProperty("shared.key"));

            // Now wrapper2 performs a write operation
            wrapper2.setProperty("from.wrapper2", "value2");

            // Both wrappers should see all changes immediately
            assertEquals("value1", wrapper1.getProperty("from.wrapper1"));
            assertEquals("value2", wrapper1.getProperty("from.wrapper2"));
            assertEquals("value1", wrapper2.getProperty("from.wrapper1"));
            assertEquals("value2", wrapper2.getProperty("from.wrapper2"));

            // Add another property through wrapper1
            wrapper1.setProperty("another.key", "another.value");
            assertEquals("another.value", wrapper1.getProperty("another.key"));
            assertEquals("another.value", wrapper2.getProperty("another.key"));
        }

        @Test
        void testVariousWriteOperations() {
            // Create a Model with initial properties
            Model model = new Model();

            Properties initialProps = new Properties();
            initialProps.setProperty("key1", "value1");
            model.setProperties(initialProps);

            Properties wrapper = model.getProperties();

            // Initial read
            assertEquals("value1", wrapper.getProperty("key1"));

            // Test put() method
            wrapper.put("key2", "value2");
            assertEquals("value2", wrapper.getProperty("key2"));

            // Simulate external change
            Properties externalProps1 = new Properties();
            externalProps1.setProperty("key1", "modified_after_put");
            externalProps1.setProperty("key2", "value2");
            externalProps1.setProperty("external.key", "external.value");
            model.setProperties(externalProps1);
            assertEquals("modified_after_put", wrapper.getProperty("key1"));

            // Test remove() method
            wrapper.remove("key2");
            assertEquals(null, wrapper.getProperty("key2"));

            // Simulate external change
            Properties externalProps2 = new Properties();
            externalProps2.setProperty("key1", "modified_after_remove");
            externalProps2.setProperty("external.key", "external.value");
            model.setProperties(externalProps2);
            assertEquals("modified_after_remove", wrapper.getProperty("key1"));

            // Test putAll() method
            Properties newProps = new Properties();
            newProps.setProperty("putall.key1", "putall.value1");
            newProps.setProperty("putall.key2", "putall.value2");
            wrapper.putAll(newProps);
            assertEquals("putall.value1", wrapper.getProperty("putall.key1"));
            assertEquals("putall.value2", wrapper.getProperty("putall.key2"));
        }
    }
}
