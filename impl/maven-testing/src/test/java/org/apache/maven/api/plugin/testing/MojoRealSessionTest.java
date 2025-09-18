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
package org.apache.maven.api.plugin.testing;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for @MojoTest(realSession=...) support.
 */
class MojoRealSessionTest {

    @Nested
    @MojoTest
    class DefaultMock {
        @Inject
        Session session;

        @Test
        void hasMockSession() {
            assertNotNull(session);
            assertTrue(org.mockito.Mockito.mockingDetails(session).isMock());
        }
    }

    @Nested
    @MojoTest(realSession = true)
    class RealSession {
        @Inject
        Session session;

        @Test
        void hasRealSession() {
            assertNotNull(session);
            // Real session must not be a Mockito mock
            assertFalse(Mockito.mockingDetails(session).isMock());
        }
    }

    @Nested
    @MojoTest
    class CustomMock {
        @Inject
        Session session;

        @Provides
        @Singleton
        static Session createSession() {
            return SessionMock.getMockSession("target/local-repo");
        }

        @Test
        void hasCustomMockSession() {
            assertNotNull(session);
            assertTrue(Mockito.mockingDetails(session).isMock());
        }
    }

    @Nested
    @MojoTest(realSession = true)
    class CustomRealOverridesFlag {
        @Inject
        Session session;

        @Provides
        @Singleton
        static Session createSession() {
            Path basedir = Paths.get(System.getProperty("basedir", ""));
            Path localRepoPath = basedir.resolve("target/local-repo");
            // Rely on DI discovery for SecDispatcherProvider to avoid duplicate bindings
            return ApiRunner.createSession(null, localRepoPath);
        }

        @Test
        void customProviderWinsOverFlag() {
            assertNotNull(session);
            assertFalse(Mockito.mockingDetails(session).isMock());
        }
    }
}
