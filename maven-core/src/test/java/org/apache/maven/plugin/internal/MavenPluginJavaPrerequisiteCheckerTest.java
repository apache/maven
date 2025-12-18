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
package org.apache.maven.plugin.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(Parameterized.class)
public class MavenPluginJavaPrerequisiteCheckerTest {

    private static final String JAVA_8_VERSION = "1.8";
    private static final String JAVA_9_VERSION = "9.0.1+11";

    @Parameterized.Parameters
    public static Object[][] parameters() {
        // requiredVersion, currentVersion, expected, throwable
        return new Object[][] {
            {"8", JAVA_8_VERSION, true},
            {"[8,)", JAVA_8_VERSION, true},
            {"(,8]", JAVA_8_VERSION, true},
            {"(,8)", JAVA_8_VERSION, false},
            {"1.8", JAVA_8_VERSION, true},
            {"[1.8,)", JAVA_8_VERSION, true},
            {"(,1.8]", JAVA_8_VERSION, true},
            {"(,1.8)", JAVA_8_VERSION, false},
            {"8", JAVA_9_VERSION, true},
            {"[8,)", JAVA_9_VERSION, true},
            {"(,8]", JAVA_9_VERSION, false},
            {"(,8)", JAVA_9_VERSION, false},
            {"1.8", JAVA_9_VERSION, true},
            {"[1.8,)", JAVA_9_VERSION, true},
            {"(,1.8]", JAVA_9_VERSION, false},
            {"(,1.8)", JAVA_9_VERSION, false},

            // various
            {"1.0", JAVA_8_VERSION, true},
            {"1.8", JAVA_9_VERSION, true},
            {"[1.0,2],[3,4]", "2.1", false},
            {"[1.0,2],[3,4]", "3.1", true},
            {"(1.0,0)", "11", null},
        };
    }

    private final MavenPluginJavaPrerequisiteChecker subject = new MavenPluginJavaPrerequisiteChecker();

    @Parameterized.Parameter(0)
    public String requiredVersion;

    @Parameterized.Parameter(1)
    public String currentVersion;

    @Parameterized.Parameter(2)
    public Boolean expected;

    @Test
    public void doTest() {
        if (expected != null) {
            assertEquals(
                    requiredVersion + ":" + currentVersion + " -> " + expected,
                    expected,
                    subject.matchesVersion(requiredVersion, currentVersion));
        } else {
            assertThrows(IllegalArgumentException.class, () -> subject.matchesVersion(requiredVersion, currentVersion));
        }
    }
}
