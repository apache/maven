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
package org.apache.maven.impl.standalone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.Session;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ApiRunner} settings handling and {@link ApiRunner.SecurityMode}.
 */
class ApiRunnerSettingsTest {

    @TempDir
    Path tempDir;

    @Test
    void testSecurityModeNone() {
        Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.NONE);
        assertNotNull(session);
        assertNotNull(session.getSettings());
    }

    @Test
    void testSecurityModeIfAvailable() {
        // plexus-sec-dispatcher is on the test classpath, so dispatchers should be bound
        Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.IF_AVAILABLE);
        assertNotNull(session);
        assertNotNull(session.getSettings());
    }

    @Test
    void testSecurityModeIfAvailableWarn() {
        // Default mode — same as IF_AVAILABLE when sec-dispatcher is on classpath
        Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.IF_AVAILABLE_WARN);
        assertNotNull(session);
        assertNotNull(session.getSettings());
    }

    @Test
    void testSecurityModeRequired() {
        // plexus-sec-dispatcher is on the test classpath, so this should succeed
        Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.REQUIRED);
        assertNotNull(session);
        assertNotNull(session.getSettings());
    }

    @Test
    void testDefaultSecurityMode() {
        // Default createSession() uses IF_AVAILABLE_WARN
        Session session = ApiRunner.createSession();
        assertNotNull(session);
        assertNotNull(session.getSettings());
    }

    @Test
    void testMavenVersionIsNeverNull() {
        Session session = ApiRunner.createSession();
        assertNotNull(session.getMavenVersion(), "getMavenVersion() should never return null");
    }

    @Test
    void testSettingsServersApplied() throws IOException {
        Path m2Dir = tempDir.resolve(".m2");
        Files.createDirectories(m2Dir);
        Files.writeString(m2Dir.resolve("settings.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
                  <servers>
                    <server>
                      <id>my-repo</id>
                      <username>myuser</username>
                      <password>mypassword</password>
                    </server>
                  </servers>
                </settings>
                """);

        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());
            Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.NONE);
            Settings settings = session.getSettings();
            assertNotNull(settings);
            assertEquals(1, settings.getServers().size());
            Server server = settings.getServers().get(0);
            assertEquals("my-repo", server.getId());
            assertEquals("myuser", server.getUsername());
            assertEquals("mypassword", server.getPassword());
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }

    @Test
    void testSettingsMirrorsApplied() throws IOException {
        Path m2Dir = tempDir.resolve(".m2");
        Files.createDirectories(m2Dir);
        Files.writeString(m2Dir.resolve("settings.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
                  <mirrors>
                    <mirror>
                      <id>my-mirror</id>
                      <url>https://mirror.example.com/maven2</url>
                      <mirrorOf>central</mirrorOf>
                    </mirror>
                  </mirrors>
                </settings>
                """);

        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());
            Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.NONE);
            Settings settings = session.getSettings();
            assertNotNull(settings);
            assertEquals(1, settings.getMirrors().size());
            assertEquals("my-mirror", settings.getMirrors().get(0).getId());
            assertEquals(
                    "https://mirror.example.com/maven2",
                    settings.getMirrors().get(0).getUrl());
            assertEquals("central", settings.getMirrors().get(0).getMirrorOf());
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }

    @Test
    void testSettingsRepositories() throws IOException {
        Path m2Dir = tempDir.resolve(".m2");
        Files.createDirectories(m2Dir);
        // Maven 4 top-level repositories in settings
        Files.writeString(m2Dir.resolve("settings.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings xmlns="http://maven.apache.org/SETTINGS/2.0.0">
                  <repositories>
                    <repository>
                      <id>custom-repo</id>
                      <url>https://repo.example.com/maven2</url>
                    </repository>
                  </repositories>
                </settings>
                """);

        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());
            Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.NONE);
            Settings settings = session.getSettings();
            assertNotNull(settings);
            assertTrue(
                    settings.getRepositories().stream().anyMatch(r -> "custom-repo".equals(r.getId())),
                    "Effective settings should include top-level repository");
            // Session should also expose them as remote repositories
            assertTrue(
                    session.getRemoteRepositories().stream().anyMatch(r -> "custom-repo".equals(r.getId())),
                    "Session should include repository from settings");
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }

    @Test
    void testLegacyEncryptedPasswordDecrypted() throws IOException {
        Path m2Dir = tempDir.resolve(".m2");
        Files.createDirectories(m2Dir);
        // Legacy master password from the existing IT test resources (mng-8379)
        // Master password "testtest" encrypted with the default settings.security key
        Files.writeString(m2Dir.resolve("settings-security.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <settingsSecurity>
                  <master>{1wQaa6S/o8MH7FnaTNL53XmhT5O0SEGXQi3gC49o6OY=}</master>
                </settingsSecurity>
                """);
        // Server password "testtest" encrypted with the master password above
        Files.writeString(m2Dir.resolve("settings.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
                  <servers>
                    <server>
                      <id>testserver</id>
                      <username>testuser</username>
                      <password>{BteqUEnqHecHM7MZfnj9FwLcYbdInWxou1C929Txa0A=}</password>
                    </server>
                  </servers>
                </settings>
                """);

        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());
            // With dispatchers active, the legacy password should be decrypted
            Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.IF_AVAILABLE);
            Settings settings = session.getSettings();
            assertNotNull(settings);
            assertEquals(1, settings.getServers().size());
            Server server = settings.getServers().get(0);
            assertEquals("testserver", server.getId());
            assertEquals("testuser", server.getUsername());
            assertEquals("testtest", server.getPassword());
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }

    @Test
    void testPlaintextPasswordUnchangedWithSecurityModeNone() throws IOException {
        // SecurityMode.NONE prevents ApiRunner from binding its own SecDispatcherBindings.
        // Note: in a test environment, discover() may still find a test-scoped SecDispatcherProvider;
        // this test verifies that plaintext passwords pass through correctly in all modes.
        Path m2Dir = tempDir.resolve(".m2");
        Files.createDirectories(m2Dir);
        Files.writeString(m2Dir.resolve("settings.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings>
                  <servers>
                    <server>
                      <id>testserver</id>
                      <username>testuser</username>
                      <password>plaintext-password</password>
                    </server>
                  </servers>
                </settings>
                """);

        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());
            Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.NONE);
            Settings settings = session.getSettings();
            assertNotNull(settings);
            assertEquals(1, settings.getServers().size());
            Server server = settings.getServers().get(0);
            assertEquals("plaintext-password", server.getPassword());
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }

    @Test
    void testSettingsOfflineMode() throws IOException {
        Path m2Dir = tempDir.resolve(".m2");
        Files.createDirectories(m2Dir);
        Files.writeString(m2Dir.resolve("settings.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
                  <offline>true</offline>
                </settings>
                """);

        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());
            Session session = ApiRunner.createSession(null, null, ApiRunner.SecurityMode.NONE);
            Settings settings = session.getSettings();
            assertNotNull(settings);
            assertTrue(settings.isOffline(), "Settings should have offline mode enabled");
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }
}
