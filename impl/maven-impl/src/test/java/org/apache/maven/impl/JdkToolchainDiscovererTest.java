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
package org.apache.maven.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.api.toolchain.ToolchainModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkToolchainDiscovererTest {

    @TempDir
    Path tempDir;

    private final JdkToolchainDiscoverer discoverer = new JdkToolchainDiscoverer();

    @Test
    void isValidJdkHomeWithJavac() throws IOException {
        Path jdkHome = createFakeJdk(tempDir, "jdk-17");
        assertTrue(discoverer.isValidJdkHome(jdkHome));
    }

    @Test
    void isValidJdkHomeWithoutJavac() throws IOException {
        Path jdkHome = tempDir.resolve("jdk-no-javac");
        Files.createDirectories(jdkHome.resolve("bin"));
        assertFalse(discoverer.isValidJdkHome(jdkHome));
    }

    @Test
    void isValidJdkHomeNonExistent() {
        assertFalse(discoverer.isValidJdkHome(tempDir.resolve("nonexistent")));
    }

    @Test
    void readVersionFromReleaseFile() throws IOException {
        Path jdkHome = tempDir.resolve("jdk-17");
        Files.createDirectories(jdkHome);
        Files.writeString(jdkHome.resolve("release"), "JAVA_VERSION=\"17.0.2\"\nIMPLEMENTOR=\"Eclipse Adoptium\"\n");

        assertEquals("17.0.2", discoverer.readVersionFromRelease(jdkHome));
    }

    @Test
    void readVersionFromReleaseFileJdk8Format() throws IOException {
        Path jdkHome = tempDir.resolve("jdk-8");
        Files.createDirectories(jdkHome);
        Files.writeString(jdkHome.resolve("release"), "JAVA_VERSION=\"1.8.0_392\"\n");

        assertEquals("1.8.0_392", discoverer.readVersionFromRelease(jdkHome));
    }

    @Test
    void readVersionFromReleaseFileNoQuotes() throws IOException {
        Path jdkHome = tempDir.resolve("jdk-11");
        Files.createDirectories(jdkHome);
        Files.writeString(jdkHome.resolve("release"), "JAVA_VERSION=11.0.21\n");

        assertEquals("11.0.21", discoverer.readVersionFromRelease(jdkHome));
    }

    @Test
    void readVersionFromMissingReleaseFile() {
        assertNull(discoverer.readVersionFromRelease(tempDir.resolve("no-release")));
    }

    @Test
    void readVersionFromReleaseFileWithoutJavaVersion() throws IOException {
        Path jdkHome = tempDir.resolve("jdk-bad");
        Files.createDirectories(jdkHome);
        Files.writeString(jdkHome.resolve("release"), "IMPLEMENTOR=\"Some Vendor\"\n");

        assertNull(discoverer.readVersionFromRelease(jdkHome));
    }

    @Test
    void buildToolchainModelFromValidJdk() throws IOException {
        Path jdkHome = createFakeJdkWithRelease(tempDir, "jdk-17", "17.0.2");

        Optional<ToolchainModel> result = discoverer.buildToolchainModel(jdkHome);

        assertTrue(result.isPresent());
        ToolchainModel model = result.get();
        assertEquals("jdk", model.getType());
        assertEquals("17", model.getProvides().get("version"));
        assertNotNull(model.getConfiguration());
        assertEquals(
                jdkHome.toString(), model.getConfiguration().child("jdkHome").value());
    }

    @Test
    void buildToolchainModelFromJdk8() throws IOException {
        Path jdkHome = createFakeJdkWithRelease(tempDir, "jdk-8", "1.8.0_392");

        Optional<ToolchainModel> result = discoverer.buildToolchainModel(jdkHome);

        assertTrue(result.isPresent());
        assertEquals("8", result.get().getProvides().get("version"));
    }

    @Test
    void buildToolchainModelWithoutReleaseFile() throws IOException {
        Path jdkHome = createFakeJdk(tempDir, "jdk-old");

        Optional<ToolchainModel> result = discoverer.buildToolchainModel(jdkHome);

        assertFalse(result.isPresent());
    }

    @Test
    void resolveJdkHomeDirectPath() throws IOException {
        Path jdkHome = createFakeJdk(tempDir, "jdk-17");

        assertEquals(jdkHome, discoverer.resolveJdkHome(jdkHome));
    }

    @Test
    void resolveJdkHomeMacOsBundle() throws IOException {
        Path bundleRoot = tempDir.resolve("jdk-17.jdk");
        Path contentsHome = bundleRoot.resolve("Contents").resolve("Home");
        Files.createDirectories(contentsHome.resolve("bin"));
        Files.createFile(contentsHome.resolve("bin").resolve("javac"));

        assertEquals(contentsHome, discoverer.resolveJdkHome(bundleRoot));
    }

    @Test
    void resolveJdkHomeInvalidPath() {
        assertNull(discoverer.resolveJdkHome(tempDir.resolve("nonexistent")));
    }

    @Test
    void discoverToolchainsIsCached() {
        // Two calls should return the same list instance (cached)
        var first = discoverer.discoverToolchains();
        var second = discoverer.discoverToolchains();
        assertNotNull(first);
        assertTrue(first == second, "Expected cached result (same instance)");
    }

    private Path createFakeJdk(Path parent, String name) throws IOException {
        Path jdkHome = parent.resolve(name);
        Path binDir = jdkHome.resolve("bin");
        Files.createDirectories(binDir);
        Files.createFile(binDir.resolve("javac"));
        return jdkHome;
    }

    private Path createFakeJdkWithRelease(Path parent, String name, String version) throws IOException {
        Path jdkHome = createFakeJdk(parent, name);
        Files.writeString(jdkHome.resolve("release"), "JAVA_VERSION=\"" + version + "\"\n");
        return jdkHome;
    }
}
