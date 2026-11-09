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
package org.apache.maven.lifecycle;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link PluginVersions} constants are properly loaded
 * from the filtered {@code plugin-versions.properties} resource.
 */
class PluginVersionsTest {

    @Test
    void allConstantsAreResolvedAndNotPlaceholders() throws Exception {
        int count = 0;
        for (Field field : PluginVersions.class.getFields()) {
            if (field.getType() == String.class
                    && Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())) {
                String value = (String) field.get(null);
                assertNotNull(value, field.getName() + " is null");
                assertFalse(value.startsWith("${"), field.getName() + " contains unfiltered placeholder: " + value);
                assertFalse(value.isEmpty(), field.getName() + " is empty");
                count++;
            }
        }
        // Ensure we actually tested something — catches the case where
        // all constants are accidentally removed or made non-public.
        assertTrue(count >= 13, "Expected at least 13 plugin version constants, found " + count);
    }
}
