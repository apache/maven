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
package org.apache.maven.api.di.testing;

import java.io.File;

import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.apache.maven.api.di.testing.MavenDIExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MavenDITest
public class SimpleDITest {

    private static final String LOCAL_REPO = getBasedir() + File.separator + "target" + File.separator + "local-repo";

    @Inject
    Session session;

    @Test
    void testSession() {
        assertNotNull(session);
        assertNotNull(session.getLocalRepository());
    }

    @Provides
    Session createSession() {
        return SessionMock.getMockSession(LOCAL_REPO);
    }

    @Test
    void testSetupContainerWithNullContext() {
        MavenDIExtension extension = new MavenDIExtension();
        MavenDIExtension.context = null;
        assertThrows(IllegalStateException.class, extension::setupContainer);
    }

    @Test
    void testSetupContainerWithNullTestClass() {
        final MavenDIExtension extension = new MavenDIExtension();
        final ExtensionContext context = mock(ExtensionContext.class);
        when(context.getRequiredTestClass()).thenReturn(null); // Mock null test class
        when(context.getRequiredTestInstance()).thenReturn(new TestClass()); // Valid instance
        MavenDIExtension.context = context;
        assertThrows(
                IllegalStateException.class,
                extension::setupContainer,
                "Should throw IllegalStateException for null test class");
    }

    static class TestClass {}
}
