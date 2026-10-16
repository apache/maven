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
package org.apache.maven.utils;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsTest {
    @Test
    void usesUnknownWhenNoFamilyMatches() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String testClasses = Path.of(OsTest.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI())
                .toString();
        String utilities =
                Os.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
        for (String name : new String[] {"unrecognised-platform", "unknown-platform"}) {
            Process process = new ProcessBuilder(
                            java,
                            "-Dpath.separator=|",
                            "-cp",
                            testClasses,
                            UnknownFamilyProbe.class.getName(),
                            utilities,
                            name)
                    .redirectErrorStream(true)
                    .start();
            try {
                assertTrue(process.waitFor(30, TimeUnit.SECONDS), "OS probe timed out");
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(0, process.exitValue(), output);
                assertEquals("unknown", output.strip());
            } finally {
                process.destroyForcibly();
            }
        }
    }

    public static class UnknownFamilyProbe {
        public static void main(String[] args) throws Exception {
            System.setProperty("os.name", args[1]);
            try (URLClassLoader loader =
                    new URLClassLoader(new URL[] {URI.create(args[0]).toURL()}, null)) {
                Class<?> os = loader.loadClass("org.apache.maven.utils.Os");
                System.out.println(os.getField("OS_FAMILY").get(null));
            }
        }
    }

    @Test
    void distinguishesWindowsNtFromWindows9x() {
        for (String name : new String[] {"windows 95", "windows 98", "windows me", "windows ce"}) {
            assertTrue(Os.isFamily("windows", name), name);
            assertTrue(Os.isFamily("win9x", name), name);
            assertFalse(Os.isFamily(Os.FAMILY_NT, name), name);
        }
        for (String name : new String[] {"windows nt", "windows 10", "windows 11", "windows server 2025"}) {
            assertTrue(Os.isFamily("windows", name), name);
            assertTrue(Os.isFamily(Os.FAMILY_NT, name), name);
            assertFalse(Os.isFamily("win9x", name), name);
        }
        assertFalse(Os.isFamily("windows", "darwin"));
        assertFalse(Os.isFamily("win9x", "linux"));
        assertFalse(Os.isFamily(Os.FAMILY_NT, "linux"));
    }

    @Test
    void recognizesMacAndDarwin() {
        assertTrue(Os.isFamily("mac", "mac os x"));
        assertTrue(Os.isFamily("mac", "mac os 9"));
        assertTrue(Os.isFamily("mac", "darwin"));
        assertFalse(Os.isFamily("mac", "linux"));
    }

    @Test
    void preservesUnixAndDosSeparatorRules() {
        assertEquals(File.pathSeparatorChar == ':', Os.isFamily("unix", "linux"));
        assertEquals(File.pathSeparatorChar == ':', Os.isFamily("unix", "mac os x"));
        assertFalse(Os.isFamily("unix", "mac os 9"));
        assertFalse(Os.isFamily("unix", "darwin"));
        assertFalse(Os.isFamily("unix", "openvms"));
        assertEquals(File.pathSeparatorChar == ';', Os.isFamily("dos", "ms-dos"));
        assertFalse(Os.isFamily("dos", "netware"));
        assertFalse(Os.isFamily("dos", "windows 95"));
    }

    @Test
    void recognizesOtherOperatingSystemFamilies() {
        String[][] cases = {
            {"os/2", "os/2"},
            {"netware", "novell netware"},
            {"tandem", "nonstop_kernel"},
            {"z/os", "z/os"},
            {"z/os", "os/390"},
            {"os/400", "os/400"},
            {"openvms", "openvms"}
        };
        for (String[] entry : cases) {
            assertTrue(Os.isFamily(entry[0], entry[1]), entry[1]);
            assertFalse(Os.isFamily(entry[0], "linux"), entry[0]);
        }
    }

    @Test
    void preservesSubstringFallbackAndCaseHandling() {
        assertTrue(Os.isFamily("CUSTOM", "my custom operating system"));
        assertTrue(Os.isFamily("WINDOWS", "windows 11"));
        assertFalse(Os.isFamily("windows", "Windows 11"));
        assertFalse(Os.isFamily("missing", "linux"));
        assertTrue(Os.isFamily("", "linux"));
    }

    @Test
    void rejectsNullFamilyOrOsName() {
        assertThrows(NullPointerException.class, () -> Os.isFamily(null, "linux"));
        assertThrows(NullPointerException.class, () -> Os.isFamily("unix", null));
    }

    @Test
    void exposesCurrentSystemInformation() {
        assertEquals(System.getProperty("os.name").toLowerCase(Locale.ENGLISH), Os.OS_NAME);
        assertEquals(System.getProperty("os.arch").toLowerCase(Locale.ENGLISH), Os.OS_ARCH);
        assertEquals(System.getProperty("os.version").toLowerCase(Locale.ENGLISH), Os.OS_VERSION);
        assertEquals(Os.isFamily("windows", Os.OS_NAME), Os.IS_WINDOWS);
        for (String family : new String[] {"windows", "mac", "unix", "dos", "openvms", "custom"}) {
            assertEquals(Os.isFamily(family, Os.OS_NAME), Os.isFamily(family), family);
        }
        assertNotNull(Os.OS_FAMILY);
        if (!"unknown".equals(Os.OS_FAMILY)) {
            assertTrue(Os.isFamily(Os.OS_FAMILY));
        }
    }
}
