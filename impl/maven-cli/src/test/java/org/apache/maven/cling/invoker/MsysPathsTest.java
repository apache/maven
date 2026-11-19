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
package org.apache.maven.cling.invoker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MsysPathsTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "/c/Users/alice|C:\\Users\\alice",
                "/D/projects/foo/.m2/repository|D:\\projects\\foo\\.m2\\repository",
                "/c/|C:\\",
                "/c/x|C:\\x"
            })
    void convertsDrivePaths(String path, String expected) {
        assertEquals(expected, MsysPaths.toNativePath(path, true));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "/tmp/repo",
                "/usr/local",
                "/c",
                "/cc/x",
                "C:\\Users\\alice",
                "relative/path",
                "c:/Users/alice",
                "//server/share"
            })
    void keepsAnythingElse(String path) {
        assertEquals(path, MsysPaths.toNativePath(path, true));
    }

    @Test
    void keepsEmptyAndNull() {
        assertEquals("", MsysPaths.toNativePath("", true));
        assertNull(MsysPaths.toNativePath(null, true));
        assertNull(MsysPaths.toNativePath(null, false));
    }

    @Test
    void keepsPathsOutsideWindows() {
        assertEquals("/c/Users/alice", MsysPaths.toNativePath("/c/Users/alice", false));
    }
}
