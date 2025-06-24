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

import org.apache.maven.api.Constants;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.ModelObjectProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for DefaultModelObjectPool.
 */
class DefaultModelObjectPoolTest {

    @Test
    void testServiceLoading() {
        // Test that the static method works
        String testString = "test";
        String result = ModelObjectProcessor.processObject(testString);
        assertNotNull(result);
        assertEquals(testString, result);
    }

    @Test
    void testDependencyPooling() {
        ModelObjectProcessor processor = new DefaultModelObjectPool();

        // Create two identical dependencies
        Dependency dep1 = Dependency.newBuilder()
                .groupId("org.apache.maven")
                .artifactId("maven-core")
                .version("4.0.0")
                .build();

        Dependency dep2 = Dependency.newBuilder()
                .groupId("org.apache.maven")
                .artifactId("maven-core")
                .version("4.0.0")
                .build();

        // They should be different instances initially
        assertNotSame(dep1, dep2);
        assertEquals(dep1, dep2);

        // After processing, they should be the same instance
        Dependency pooled1 = processor.process(dep1);
        Dependency pooled2 = processor.process(dep2);

        assertSame(pooled1, pooled2);
        assertEquals(dep1, pooled1);
        assertEquals(dep2, pooled2);
    }

    @Test
    void testNonDependencyObjects() {
        ModelObjectProcessor processor = new DefaultModelObjectPool();

        String testString = "test";
        String result = processor.process(testString);

        // Non-dependency objects should be returned as-is
        assertSame(testString, result);
    }

    @Test
    void testConfigurableReferenceType() {
        // Test that the reference type can be configured via system property
        String originalValue = System.getProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE);

        try {
            // Set a different reference type
            System.setProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE, "SOFT");

            // Create a new processor (this would use the new setting in a real scenario)
            ModelObjectProcessor processor = new DefaultModelObjectPool();

            // Test that it still works (the actual reference type is used internally)
            Dependency dep = Dependency.newBuilder()
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0")
                    .build();

            Dependency result = processor.process(dep);
            assertNotNull(result);
            assertEquals(dep, result);

        } finally {
            // Restore original value
            if (originalValue != null) {
                System.setProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE, originalValue);
            } else {
                System.clearProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE);
            }
        }
    }

    @Test
    void testConfigurablePooledTypes() {
        String originalPooledTypes = System.getProperty(Constants.MAVEN_MODEL_PROCESSOR_POOLED_TYPES);

        try {
            // Configure to only pool Dependencies
            System.setProperty(Constants.MAVEN_MODEL_PROCESSOR_POOLED_TYPES, "Dependency");

            ModelObjectProcessor processor = new DefaultModelObjectPool();

            // Dependencies should be pooled
            Dependency dep1 = Dependency.newBuilder()
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0")
                    .build();

            Dependency dep2 = Dependency.newBuilder()
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0")
                    .build();

            Dependency result1 = processor.process(dep1);
            Dependency result2 = processor.process(dep2);

            // Should be the same instance due to pooling
            assertSame(result1, result2);

            // Non-dependency objects should not be pooled (pass through)
            String str1 = "test";
            String str2 = processor.process(str1);
            assertSame(str1, str2); // Same instance because it's not pooled

        } finally {
            if (originalPooledTypes != null) {
                System.setProperty(Constants.MAVEN_MODEL_PROCESSOR_POOLED_TYPES, originalPooledTypes);
            } else {
                System.clearProperty(Constants.MAVEN_MODEL_PROCESSOR_POOLED_TYPES);
            }
        }
    }

    @Test
    void testPerTypeReferenceType() {
        String originalDefault = System.getProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE);
        String originalDependency =
                System.getProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE_PREFIX + "Dependency");

        try {
            // Set default to WEAK and Dependency-specific to HARD
            System.setProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE, "WEAK");
            System.setProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE_PREFIX + "Dependency", "HARD");

            ModelObjectProcessor processor = new DefaultModelObjectPool();

            // Test that dependencies still work with per-type configuration
            Dependency dep = Dependency.newBuilder()
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0")
                    .build();

            Dependency result = processor.process(dep);
            assertNotNull(result);
            assertEquals(dep, result);

        } finally {
            if (originalDefault != null) {
                System.setProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE, originalDefault);
            } else {
                System.clearProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE);
            }

            if (originalDependency != null) {
                System.setProperty(
                        Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE_PREFIX + "Dependency", originalDependency);
            } else {
                System.clearProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE_PREFIX + "Dependency");
            }
        }
    }

    @Test
    void testStatistics() {
        ModelObjectProcessor processor = new DefaultModelObjectPool();

        // Process some dependencies
        for (int i = 0; i < 5; i++) {
            Dependency dep = Dependency.newBuilder()
                    .groupId("test")
                    .artifactId("test-" + (i % 2)) // Create some duplicates
                    .version("1.0")
                    .build();
            processor.process(dep);
        }

        // Check that statistics are available
        String stats = DefaultModelObjectPool.getStatistics(Dependency.class);
        assertNotNull(stats);
        assertTrue(stats.contains("Dependency"));

        String allStats = DefaultModelObjectPool.getAllStatistics();
        assertNotNull(allStats);
        assertTrue(allStats.contains("ModelObjectPool Statistics"));
    }
}
