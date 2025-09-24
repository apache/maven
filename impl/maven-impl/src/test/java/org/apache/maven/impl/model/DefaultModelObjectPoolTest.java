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

import java.util.Map;
import java.util.Objects;
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
        // Note: Due to the static processor being active, these may already be pooled
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

        // Due to static processing, they may already be the same instance
        // This is actually the expected behavior - pooling is working!

        // Process them through our specific processor instance
        Dependency pooled1 = processor.process(dep1);
        Dependency pooled2 = processor.process(dep2);

        // They should be the same instance after processing
        assertSame(pooled1, pooled2);

        // The pooled instances should be semantically equal to the originals
        assertTrue(dependenciesEqual(dep1, pooled1));
        assertTrue(dependenciesEqual(dep2, pooled2));
    }

    /**
     * Helper method to check complete equality of dependencies.
     */
    private boolean dependenciesEqual(Dependency dep1, Dependency dep2) {
        return Objects.equals(dep1.getGroupId(), dep2.getGroupId())
                && Objects.equals(dep1.getArtifactId(), dep2.getArtifactId())
                && Objects.equals(dep1.getVersion(), dep2.getVersion())
                && Objects.equals(dep1.getType(), dep2.getType())
                && Objects.equals(dep1.getClassifier(), dep2.getClassifier())
                && Objects.equals(dep1.getScope(), dep2.getScope())
                && Objects.equals(dep1.getSystemPath(), dep2.getSystemPath())
                && Objects.equals(dep1.getExclusions(), dep2.getExclusions())
                && Objects.equals(dep1.getOptional(), dep2.getOptional())
                && Objects.equals(dep1.getLocationKeys(), dep2.getLocationKeys())
                && locationsEqual(dep1, dep2)
                && Objects.equals(dep1.getImportedFrom(), dep2.getImportedFrom());
    }

    /**
     * Helper method to check locations equality.
     */
    private boolean locationsEqual(Dependency dep1, Dependency dep2) {
        var keys1 = dep1.getLocationKeys();
        var keys2 = dep2.getLocationKeys();

        if (!Objects.equals(keys1, keys2)) {
            return false;
        }

        for (Object key : keys1) {
            if (!Objects.equals(dep1.getLocation(key), dep2.getLocation(key))) {
                return false;
            }
        }
        return true;
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
        // Configure to only pool Dependencies
        ModelObjectProcessor processor = new DefaultModelObjectPool(Map.of(Constants.MAVEN_MODEL_PROCESSOR_POOLED_TYPES, "Dependency"));

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
    }

    @Test
    void testPerTypeReferenceType() {
        // Set default to WEAK and Dependency-specific to HARD
        ModelObjectProcessor processor = new DefaultModelObjectPool(Map.of(
                Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE, "WEAK",
                Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE_PREFIX + "Dependency", "HARD"
        ));

        // Test that dependencies still work with per-type configuration
        Dependency dep = Dependency.newBuilder()
                .groupId("test")
                .artifactId("test")
                .version("1.0")
                .build();

        Dependency result = processor.process(dep);
        assertNotNull(result);
        assertEquals(dep, result);
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
