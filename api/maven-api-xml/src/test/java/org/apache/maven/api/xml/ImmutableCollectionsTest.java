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
package org.apache.maven.api.xml;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImmutableCollectionsTest {

    @Test
    void propertiesCopyPreservesDefaults() {
        // Create a Properties chain: defaults -> child
        Properties defaults = new Properties();
        defaults.setProperty("inherited", "default-value");
        defaults.setProperty("shadowed", "default-value");

        Properties child = new Properties(defaults);
        child.setProperty("direct", "direct-value");
        child.setProperty("shadowed", "child-value"); // shadows default

        // Copy via ImmutableCollections.copy()
        Properties copy = ImmutableCollections.copy(child);

        // Direct entries must be present
        assertEquals("direct-value", copy.getProperty("direct"));
        // Shadowed entry should use child's value
        assertEquals("child-value", copy.getProperty("shadowed"));
        // Inherited entry from defaults must be visible via getProperty
        assertEquals("default-value", copy.getProperty("inherited"));

        // Verify immutability of defaults: changing the original shouldn't affect the copy
        defaults.setProperty("inherited", "changed");
        assertEquals("default-value", copy.getProperty("inherited"));
    }
}
