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
package org.apache.maven.api.plugin.testing.stubs;

import java.io.File;
import java.util.NoSuchElementException;

import org.apache.maven.api.Service;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.impl.InternalSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test to demonstrate the improved default behavior of SessionMock.getService().
 * This test shows that the mock correctly throws NoSuchElementException for unknown
 * services while still returning configured services.
 */
class SessionMockDefaultBehaviorTest {

    private static final String LOCAL_REPO = System.getProperty("java.io.tmpdir") + File.separator + "test-repo";

    /**
     * Test that demonstrates the clean default behavior:
     * - Configured services work correctly
     * - Unknown services throw NoSuchElementException by default
     * - No need to maintain explicit exclusion lists
     */
    @Test
    void testCleanDefaultBehavior() {
        InternalSession session = SessionMock.getMockSession(LOCAL_REPO);

        // Configured services should work
        assertNotNull(session.getService(RepositoryFactory.class));
        assertNotNull(session.getService(ArtifactManager.class));

        // Unknown services should throw NoSuchElementException by default
        assertThrows(NoSuchElementException.class, () -> session.getService(CustomService.class));
        assertThrows(NoSuchElementException.class, () -> session.getService(AnotherCustomService.class));
    }

    /**
     * Custom service interface for testing - should throw NoSuchElementException
     */
    interface CustomService extends Service {
        void doSomething();
    }

    /**
     * Another custom service interface for testing - should throw NoSuchElementException
     */
    interface AnotherCustomService extends Service {
        void doSomethingElse();
    }
}
