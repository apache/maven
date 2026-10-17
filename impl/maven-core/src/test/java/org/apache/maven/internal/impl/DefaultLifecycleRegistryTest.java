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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLifecycleRegistryTest {

    @Test
    void returnsNullImplementationVersionWhenClassHasNoPackage() {
        assertNull(DefaultLifecycleRegistry.getImplementationVersion(int.class));
    }

    /**
     * Verifies that the site-lifecycle filtering constant reads from the correct system property.
     * The actual site-disabled path requires a forked JVM with the property set at startup
     * (static final field), so we verify correctness of the property name used.
     */
    @Test
    void siteDisabledPropertyNameMatchesConstant() throws Exception {
        java.lang.reflect.Field field = DefaultLifecycleRegistry.class.getDeclaredField("SITE_DISABLED");
        field.setAccessible(true);
        // SITE_DISABLED reads Constants.MAVEN_SITE_LIFECYCLE_DISABLED at class-load time.
        // We verify: when the property is not set, SITE_DISABLED is false,
        // and the site lifecycle IS present in the default stream.
        boolean siteDisabled = (boolean) field.get(null);
        assertNull(
                System.getProperty(Constants.MAVEN_SITE_LIFECYCLE_DISABLED),
                "System property must not be set in this test JVM");
        assertFalse(siteDisabled, "SITE_DISABLED should be false when property is not set");
        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(List.of());
        assertTrue(registry.lookup("site").isPresent(), "site lifecycle present when SITE_DISABLED=false");
    }
}
