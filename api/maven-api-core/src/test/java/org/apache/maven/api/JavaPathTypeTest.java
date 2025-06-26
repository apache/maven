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
package org.apache.maven.api;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaPathTypeTest {
    /**
     * {@return dummy paths to use in tests}.
     */
    private static List<Path> paths() {
        return List.of(Path.of("src", "foo.java"), Path.of("src", "bar.java"));
    }

    /**
     * Converts paths from Unix style to platform-dependent style.
     *
     * @param expected the option value expected by the test
     * @return the expected value with separators of the host
     */
    private static String toPlatformSpecific(String expected) {
        return expected.replace("/", File.separator).replace(":", File.pathSeparator);
    }

    /**
     * Tests the formatting of an option without module name.
     */
    @Test
    public void testOption() {
        String[] formatted = JavaPathType.MODULES.option(paths());
        assertEquals(2, formatted.length);
        assertEquals("--module-path", formatted[0]);
        assertEquals(toPlatformSpecific("\"src/foo.java:src/bar.java\""), formatted[1]);
    }

    /**
     * Tests the formatting of an option with a module name.
     */
    @Test
    public void testModularOption() {
        String[] formatted = JavaPathType.patchModule("my.module").option(paths());
        assertEquals(2, formatted.length);
        assertEquals("--patch-module", formatted[0]);
        assertEquals(toPlatformSpecific("my.module=\"src/foo.java:src/bar.java\""), formatted[1]);
    }
}
