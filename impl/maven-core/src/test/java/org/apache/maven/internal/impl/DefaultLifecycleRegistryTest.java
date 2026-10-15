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
package org.apache.maven.internal.impl;

import java.util.List;

import org.apache.maven.api.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLifecycleRegistryTest {

    @Test
    void returnsNullImplementationVersionWhenClassHasNoPackage() {
        assertNull(DefaultLifecycleRegistry.getImplementationVersion(int.class));
    }

    /**
     * Verifies that when {@code maven.site.lifecycle.disabled} is not set,
     * the site lifecycle is present in the default stream.
     */
    @Test
    void siteLifecyclePresentWhenPropertyNotSet() {
        assertNull(
                System.getProperty(Constants.MAVEN_SITE_LIFECYCLE_DISABLED),
                "System property must not be set in this test JVM");
        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(List.of());
        assertTrue(registry.lookup("site").isPresent(), "site lifecycle present when property is not set");
    }
}
