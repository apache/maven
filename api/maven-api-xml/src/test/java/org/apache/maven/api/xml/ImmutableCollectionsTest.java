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
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImmutableCollectionsTest {

    @Test
    void propertiesCopyPreservesDefaults() {
        Properties rootDefaults = new Properties();
        rootDefaults.setProperty("root", "root-value");
        rootDefaults.setProperty("shadowed", "default-value");
        rootDefaults.setProperty("non-string-shadow", "fallback-value");
        Properties defaults = new Properties(rootDefaults);
        defaults.setProperty("inherited", "default-value");
        Properties properties = new Properties(defaults);
        properties.setProperty("direct", "direct-value");
        properties.setProperty("shadowed", "direct-value");
        properties.put("non-string-shadow", 42);

        Properties copy = ImmutableCollections.copy(properties);

        assertEquals("default-value", copy.getProperty("inherited"));
        assertEquals("root-value", copy.getProperty("root"));
        assertFalse(copy.containsKey("inherited"));
        assertFalse(copy.containsKey("root"));
        assertEquals(3, copy.size());
        assertEquals("direct-value", copy.getProperty("direct"));
        assertEquals("direct-value", copy.getProperty("shadowed"));
        assertEquals(42, copy.get("non-string-shadow"));
        assertEquals("fallback-value", copy.getProperty("non-string-shadow"));

        defaults.setProperty("inherited", "changed");
        rootDefaults.setProperty("root", "changed");
        assertEquals("default-value", copy.getProperty("inherited"));
        assertEquals("root-value", copy.getProperty("root"));
    }
}
