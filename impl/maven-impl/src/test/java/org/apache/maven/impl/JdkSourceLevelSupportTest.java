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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkSourceLevelSupportTest {

    @Test
    void minimumSupportedSourceLevelJdk8AndEarlier() {
        assertEquals(1, JdkSourceLevelSupport.minimumSupportedSourceLevel(7));
        assertEquals(1, JdkSourceLevelSupport.minimumSupportedSourceLevel(8));
    }

    @Test
    void minimumSupportedSourceLevelJdk9To11() {
        assertEquals(6, JdkSourceLevelSupport.minimumSupportedSourceLevel(9));
        assertEquals(6, JdkSourceLevelSupport.minimumSupportedSourceLevel(10));
        assertEquals(6, JdkSourceLevelSupport.minimumSupportedSourceLevel(11));
    }

    @Test
    void minimumSupportedSourceLevelJdk12To20() {
        assertEquals(7, JdkSourceLevelSupport.minimumSupportedSourceLevel(12));
        assertEquals(7, JdkSourceLevelSupport.minimumSupportedSourceLevel(15));
        assertEquals(7, JdkSourceLevelSupport.minimumSupportedSourceLevel(17));
        assertEquals(7, JdkSourceLevelSupport.minimumSupportedSourceLevel(20));
    }

    @Test
    void minimumSupportedSourceLevelJdk21AndLater() {
        assertEquals(8, JdkSourceLevelSupport.minimumSupportedSourceLevel(21));
        assertEquals(8, JdkSourceLevelSupport.minimumSupportedSourceLevel(22));
        assertEquals(8, JdkSourceLevelSupport.minimumSupportedSourceLevel(25));
    }

    @Test
    void supportsSourceLevelJdk8() {
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(8, 1));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(8, 5));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(8, 6));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(8, 8));
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(8, 9));
    }

    @Test
    void supportsSourceLevelJdk11() {
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(11, 5));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(11, 6));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(11, 8));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(11, 11));
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(11, 12));
    }

    @Test
    void supportsSourceLevelJdk17() {
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(17, 5));
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(17, 6));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(17, 7));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(17, 8));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(17, 11));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(17, 17));
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(17, 18));
    }

    @Test
    void supportsSourceLevelJdk21() {
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(21, 6));
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(21, 7));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(21, 8));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(21, 11));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(21, 17));
        assertTrue(JdkSourceLevelSupport.supportsSourceLevel(21, 21));
        assertFalse(JdkSourceLevelSupport.supportsSourceLevel(21, 22));
    }

    @Test
    void normalizeSourceLevelLegacyFormat() {
        assertEquals(5, JdkSourceLevelSupport.normalizeSourceLevel("1.5"));
        assertEquals(6, JdkSourceLevelSupport.normalizeSourceLevel("1.6"));
        assertEquals(7, JdkSourceLevelSupport.normalizeSourceLevel("1.7"));
        assertEquals(8, JdkSourceLevelSupport.normalizeSourceLevel("1.8"));
    }

    @Test
    void normalizeSourceLevelModernFormat() {
        assertEquals(5, JdkSourceLevelSupport.normalizeSourceLevel("5"));
        assertEquals(6, JdkSourceLevelSupport.normalizeSourceLevel("6"));
        assertEquals(8, JdkSourceLevelSupport.normalizeSourceLevel("8"));
        assertEquals(9, JdkSourceLevelSupport.normalizeSourceLevel("9"));
        assertEquals(11, JdkSourceLevelSupport.normalizeSourceLevel("11"));
        assertEquals(17, JdkSourceLevelSupport.normalizeSourceLevel("17"));
        assertEquals(21, JdkSourceLevelSupport.normalizeSourceLevel("21"));
    }

    @Test
    void normalizeSourceLevelDottedVersion() {
        assertEquals(21, JdkSourceLevelSupport.normalizeSourceLevel("21.0.1"));
        assertEquals(17, JdkSourceLevelSupport.normalizeSourceLevel("17.0.2"));
        assertEquals(11, JdkSourceLevelSupport.normalizeSourceLevel("11.0.3"));
    }

    @Test
    void normalizeSourceLevelInvalid() {
        assertEquals(-1, JdkSourceLevelSupport.normalizeSourceLevel(null));
        assertEquals(-1, JdkSourceLevelSupport.normalizeSourceLevel(""));
        assertEquals(-1, JdkSourceLevelSupport.normalizeSourceLevel("abc"));
        assertEquals(-1, JdkSourceLevelSupport.normalizeSourceLevel("${java.version}"));
    }

    @Test
    void normalizeSourceLevelWithWhitespace() {
        assertEquals(11, JdkSourceLevelSupport.normalizeSourceLevel(" 11 "));
        assertEquals(8, JdkSourceLevelSupport.normalizeSourceLevel("  1.8  "));
    }

    @Test
    void getRunningJdkMajorReturnsPositive() {
        assertTrue(JdkSourceLevelSupport.getRunningJdkMajor() > 0);
    }
}
