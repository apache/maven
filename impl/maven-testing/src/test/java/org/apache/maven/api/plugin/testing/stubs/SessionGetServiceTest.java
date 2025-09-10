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
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.OsService;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.impl.InternalSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the getService method behavior in SessionMock and SessionStub.
 * This test verifies that both implementations correctly throw NoSuchElementException
 * when a service is not found, as specified in the Session interface contract.
 */
class SessionGetServiceTest {

    private static final String LOCAL_REPO = System.getProperty("java.io.tmpdir") + File.separator + "test-repo";

    /**
     * Test that SessionStub.getService throws NoSuchElementException for unknown services.
     */
    @Test
    void testSessionStubGetServiceThrowsNoSuchElementException() {
        Session session = new SessionStub();

        // Test with a service interface that should not be available
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> session.getService(OsService.class),
                "SessionStub.getService should throw NoSuchElementException for unknown services");

        // Verify the exception message contains the service class name
        assertNotNull(exception.getMessage());
        assert exception.getMessage().contains(OsService.class.getName());
    }

    /**
     * Test that SessionStub.getService throws NoSuchElementException for any service.
     * Since SessionStub is a minimal stub, it should not provide any services.
     */
    @Test
    void testSessionStubGetServiceThrowsForAnyService() {
        Session session = new SessionStub();

        // Test with various service types
        assertThrows(NoSuchElementException.class, () -> session.getService(ArtifactManager.class));
        assertThrows(NoSuchElementException.class, () -> session.getService(ProjectBuilder.class));
        assertThrows(NoSuchElementException.class, () -> session.getService(VersionParser.class));
        assertThrows(NoSuchElementException.class, () -> session.getService(RepositoryFactory.class));
        assertThrows(NoSuchElementException.class, () -> session.getService(ArtifactFactory.class));
        assertThrows(NoSuchElementException.class, () -> session.getService(ProjectManager.class));
    }

    /**
     * Test that regular SessionMock.getService returns null for unknown services.
     * This is Mockito's default behavior. For the enhanced behavior that throws
     * NoSuchElementException, use getMockSessionWithEnhancedServiceBehavior().
     */
    @Test
    void testSessionMockGetServiceReturnsNullForUnknownServices() {
        InternalSession session = SessionMock.getMockSession(LOCAL_REPO);

        // Test with a service that is not configured in SessionMock
        Object result = session.getService(OsService.class);
        assertNull(result, "Regular SessionMock.getService returns null for unknown services");
    }

    /**
     * Test that enhanced SessionMock.getService throws NoSuchElementException for unknown services.
     */
    @Test
    void testEnhancedSessionMockGetServiceThrowsNoSuchElementExceptionForUnknownServices() {
        InternalSession session = SessionMock.getMockSessionWithEnhancedServiceBehavior(LOCAL_REPO);

        // Test with a service that is not configured in SessionMock
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> session.getService(OsService.class),
                "Enhanced SessionMock.getService should throw NoSuchElementException for unknown services");

        // Verify the exception message contains the service class name
        assertNotNull(exception.getMessage());
        assert exception.getMessage().contains(OsService.class.getName());
    }

    /**
     * Test that SessionMock.getService returns configured services successfully.
     */
    @Test
    void testSessionMockGetServiceReturnsConfiguredServices() {
        InternalSession session = SessionMock.getMockSession(LOCAL_REPO);

        // Test services that are configured in SessionMock
        assertNotNull(session.getService(RepositoryFactory.class));
        assertNotNull(session.getService(VersionParser.class));
        assertNotNull(session.getService(ArtifactManager.class));
        assertNotNull(session.getService(ProjectManager.class));
        assertNotNull(session.getService(ArtifactFactory.class));
        assertNotNull(session.getService(ProjectBuilder.class));
        assertNotNull(session.getService(ModelXmlFactory.class));
    }

    /**
     * Test that enhanced SessionMock.getService throws NoSuchElementException for custom service interfaces.
     */
    @Test
    void testEnhancedSessionMockGetServiceThrowsForCustomServices() {
        InternalSession session = SessionMock.getMockSessionWithEnhancedServiceBehavior(LOCAL_REPO);

        // Test with a custom service interface
        assertThrows(
                NoSuchElementException.class,
                () -> session.getService(CustomTestService.class),
                "Enhanced SessionMock.getService should throw NoSuchElementException for custom services");
    }

    /**
     * Custom service interface for testing purposes.
     */
    interface CustomTestService extends Service {
        void doSomething();
    }
}
