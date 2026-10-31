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
package org.apache.maven.toolchain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author mkleint
 */
public class RequirementMatcherFactoryTest {

    /**
     * Test of createExactMatcher method, of class RequirementMatcherFactory.
     */
    @Test
    public void testCreateExactMatcher() {
        RequirementMatcher matcher;
        matcher = RequirementMatcherFactory.createExactMatcher("foo");
        assertFalse(matcher.matches("bar"));
        assertFalse(matcher.matches("foobar"));
        assertFalse(matcher.matches("foob"));
        assertTrue(matcher.matches("foo"));
    }

    /**
     * Test of createVersionMatcher method, of class RequirementMatcherFactory.
     */
    @Test
    public void testCreateVersionMatcher() {
        RequirementMatcher matcher;
        matcher = RequirementMatcherFactory.createVersionMatcher("1.5.2");
        assertTrue(matcher.matches("1")); // Major matches
        assertTrue(matcher.matches("1.5")); // Major.Minor matches
        assertTrue(matcher.matches("1.5.2")); // Full match
        assertFalse(matcher.matches("1.6")); // Wrong minor
        assertFalse(matcher.matches("2")); // Wrong major
        assertFalse(matcher.matches("2.5")); // Wrong major, right minor
        assertFalse(matcher.matches("[1.4,1.5)"));
        assertFalse(matcher.matches("[1.5,1.5.2)"));
        assertTrue(matcher.matches("[1.5,1.5.3)"));
        assertTrue(matcher.matches("(1.5.1,1.6)"));
        assertFalse(matcher.matches("(1.5.2,1.6)"));
        assertTrue(matcher.matches("[1.5.2,1.6)"));
        assertTrue(matcher.matches("(1.4,1.5.2]"));
        assertTrue(matcher.matches("(1.5,)"));
        assertEquals("1.5.2", matcher.toString());

        // Ensure it is not printed as 1.5.0
        matcher = RequirementMatcherFactory.createVersionMatcher("1.5");
        assertEquals("1.5", matcher.toString());
    }

    @Test
    public void testCreateVersionMatcherMultiDigit() {
        RequirementMatcher matcher;
        matcher = RequirementMatcherFactory.createVersionMatcher("11.55.22");
        assertTrue(matcher.matches("11")); // Major matches
        assertTrue(matcher.matches("11.55")); // Major.Minor matches
        assertTrue(matcher.matches("11.55.22")); // Full match
        assertFalse(matcher.matches("11.66")); // Wrong minor
        assertFalse(matcher.matches("22")); // Wrong major
        assertFalse(matcher.matches("22.55")); // Wrong major, right minor
        assertFalse(matcher.matches("[11.54,11.55)"));
        assertFalse(matcher.matches("[11.55,11.55.22)"));
        assertTrue(matcher.matches("[11.55,11.55.33)"));
        assertTrue(matcher.matches("(11.55.11,11.56)"));
        assertFalse(matcher.matches("(11.55.22,11.56)"));
        assertTrue(matcher.matches("[11.55.22,11.56)"));
        assertTrue(matcher.matches("(11.54,11.55.22]"));
        assertTrue(matcher.matches("(11.55,)"));
        assertEquals("11.55.22", matcher.toString());

        // Ensure it is not printed as 1.5.0
        matcher = RequirementMatcherFactory.createVersionMatcher("11.55");
        assertEquals("11.55", matcher.toString());
    }
}
