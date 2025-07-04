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
package org.apache.maven.api.feature;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Features class.
 */
class FeaturesTest {

    @Test
    void testDeployBuildPomDefaultValue() {
        // Test that deployBuildPom returns true by default (when property is not set)
        Map<String, Object> emptyProperties = Map.of();
        assertTrue(Features.deployBuildPom(emptyProperties));

        // Test with null properties
        assertTrue(Features.deployBuildPom(null));
    }

    @Test
    void testDeployBuildPomWithStringTrue() {
        // Test with string "true"
        Map<String, Object> properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "true");
        assertTrue(Features.deployBuildPom(properties));
    }

    @Test
    void testDeployBuildPomWithStringFalse() {
        // Test with string "false"
        Map<String, Object> properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "false");
        assertFalse(Features.deployBuildPom(properties));
    }

    @Test
    void testDeployBuildPomWithBooleanTrue() {
        // Test with Boolean.TRUE
        Map<String, Object> properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, Boolean.TRUE);
        assertTrue(Features.deployBuildPom(properties));
    }

    @Test
    void testDeployBuildPomWithBooleanFalse() {
        // Test with Boolean.FALSE
        Map<String, Object> properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, Boolean.FALSE);
        assertFalse(Features.deployBuildPom(properties));
    }

    @Test
    void testDeployBuildPomWithVariousStringValues() {
        // Test case-insensitive string parsing
        Map<String, Object> properties;

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "TRUE");
        assertTrue(Features.deployBuildPom(properties));

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "FALSE");
        assertFalse(Features.deployBuildPom(properties));

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "True");
        assertTrue(Features.deployBuildPom(properties));

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "False");
        assertFalse(Features.deployBuildPom(properties));
    }

    @Test
    void testDeployBuildPomWithInvalidStringValues() {
        // Test that invalid string values default to false (Boolean.parseBoolean behavior)
        Map<String, Object> properties;

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "invalid");
        assertFalse(Features.deployBuildPom(properties));

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "");
        assertFalse(Features.deployBuildPom(properties));

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "yes");
        assertFalse(Features.deployBuildPom(properties));

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "1");
        assertFalse(Features.deployBuildPom(properties));
    }

    @Test
    void testDeployBuildPomWithNonStringNonBooleanValue() {
        // Test with other object types (should use toString() and then parseBoolean)
        Map<String, Object> properties;

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, 1);
        assertFalse(Features.deployBuildPom(properties)); // "1".parseBoolean() = false

        properties = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, 0);
        assertFalse(Features.deployBuildPom(properties)); // "0".parseBoolean() = false
    }

    @Test
    void testDeployBuildPomWithMutableMap() {
        // Test with a mutable map to ensure the method doesn't modify the input
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.MAVEN_DEPLOY_BUILD_POM, "false");

        assertFalse(Features.deployBuildPom(properties));

        // Verify the map wasn't modified
        assertTrue(properties.size() == 1);
        assertTrue("false".equals(properties.get(Constants.MAVEN_DEPLOY_BUILD_POM)));
    }

    @Test
    void testDeployBuildPomWithOtherProperties() {
        // Test that other properties don't interfere
        Map<String, Object> properties = Map.of(
                Constants.MAVEN_CONSUMER_POM,
                "false",
                Constants.MAVEN_MAVEN3_PERSONALITY,
                "true",
                "some.other.property",
                "value",
                Constants.MAVEN_DEPLOY_BUILD_POM,
                "false");

        assertFalse(Features.deployBuildPom(properties));
    }

    @Test
    void testConsistencyWithOtherFeatureMethods() {
        // Test that deployBuildPom behaves consistently with other feature methods
        Map<String, Object> properties = Map.of(
                Constants.MAVEN_DEPLOY_BUILD_POM, "false",
                Constants.MAVEN_CONSUMER_POM, "false");

        assertFalse(Features.deployBuildPom(properties));
        assertFalse(Features.consumerPom(properties));

        properties = Map.of(
                Constants.MAVEN_DEPLOY_BUILD_POM, "true",
                Constants.MAVEN_CONSUMER_POM, "true");

        assertTrue(Features.deployBuildPom(properties));
        assertTrue(Features.consumerPom(properties));
    }
}
