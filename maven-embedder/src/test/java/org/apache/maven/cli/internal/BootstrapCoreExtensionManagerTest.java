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
package org.apache.maven.cli.internal;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the JSR330 visibility filtering bypass: it must apply only when the TCCL is (a child of) a core extension realm.
 */
class BootstrapCoreExtensionManagerTest {
    private final ClassLoader origTccl = Thread.currentThread().getContextClassLoader();

    private ClassWorld world;

    @BeforeEach
    void setUp() {
        world = new ClassWorld();
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(origTccl);
    }

    @Test
    void plainTcclIsNotCoreExtensionRealm() {
        assertFalse(BootstrapCoreExtensionManager.isContextRealmCoreExtensionRealm());
    }

    @Test
    void coreExtensionRealmTccl() throws Exception {
        Thread.currentThread().setContextClassLoader(world.newRealm("coreExtension>g:a:1", null));
        assertTrue(BootstrapCoreExtensionManager.isContextRealmCoreExtensionRealm());
    }

    @Test
    void childOfCoreExtensionRealmTccl() throws Exception {
        Thread.currentThread()
                .setContextClassLoader(
                        new java.net.URLClassLoader(new java.net.URL[0], world.newRealm("coreExtension>g:a:1", null)));
        assertTrue(BootstrapCoreExtensionManager.isContextRealmCoreExtensionRealm());
    }

    @Test
    void otherRealmTccl() throws Exception {
        Thread.currentThread().setContextClassLoader(world.newRealm("plugin>g:a:1", null));
        assertFalse(BootstrapCoreExtensionManager.isContextRealmCoreExtensionRealm());
    }
}
