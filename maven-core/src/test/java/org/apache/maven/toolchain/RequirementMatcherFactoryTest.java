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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
        assertTrue(matcher.matches("1"));
        assertTrue(matcher.matches("1.5"));
        assertTrue(matcher.matches("1.5.2"));
        assertFalse(matcher.matches("[1.4,1.5)"));
        assertFalse(matcher.matches("[1.5,1.5.2)"));
        assertFalse(matcher.matches("(1.5.2,1.6)"));
        assertTrue(matcher.matches("(1.4,1.5.2]"));
        assertTrue(matcher.matches("(1.5,)"));

        assertTrue(matcher.matches("1.5+"));
        assertFalse(matcher.matches("1.5-"));

        assertFalse(matcher.matches("1.6+"));
        assertTrue(matcher.matches("1.6-"));

        assertEquals("1.5.2", matcher.toString());

        // Ensure it is not printed as 1.5.0
        matcher = RequirementMatcherFactory.createVersionMatcher("1.5");
        assertEquals("1.5", matcher.toString());
    }

    @Test
    public void testCreateVersionMatcherWithJavaVersions() {
        RequirementMatcher java25 = RequirementMatcherFactory.createVersionMatcher("25.0.2");
        RequirementMatcher java21 = RequirementMatcherFactory.createVersionMatcher("21.0.10");
        RequirementMatcher java17 = RequirementMatcherFactory.createVersionMatcher("17.0.18");
        RequirementMatcher java11 = RequirementMatcherFactory.createVersionMatcher("11.0.30");
        RequirementMatcher java8 = RequirementMatcherFactory.createVersionMatcher("1.8.0_482");
        List<RequirementMatcher> matchers = Arrays.asList(java25, java21, java17, java11, java8);

        testMatch("11", matchers, java11);
        testMatch("11+", matchers, java25, java21, java17, java11);
        testMatch("11-", matchers, java8);
        testMatch("[11,21)", matchers, java17, java11);
        testMatch("1.8", matchers, java8);
        testMatch("1.8+", matchers, java25, java21, java17, java11, java8);
        testMatch("8", matchers);
        testMatch("8+", matchers, java25, java21, java17, java11);
    }

    private static void testMatch(
            String requirement, Collection<RequirementMatcher> allMatchers, RequirementMatcher... requiredMatchers) {
        int matches = 0;
        for (RequirementMatcher matcher : allMatchers) {
            if (matcher.matches(requirement)) {
                matches++;
                assertTrue(Arrays.asList(requiredMatchers).contains(matcher));
            }
        }
        assertEquals(matches, requiredMatchers.length);
    }
}
