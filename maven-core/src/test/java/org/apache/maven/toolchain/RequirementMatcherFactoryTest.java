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

import junit.framework.TestCase;

/**
 *
 * @author mkleint
 */
public class RequirementMatcherFactoryTest extends TestCase {

    public RequirementMatcherFactoryTest(String testName) {
        super(testName);
    }

    /**
     * Test of createExactMatcher method, of class RequirementMatcherFactory.
     */
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
    public void testCreateVersionMatcher() {
        RequirementMatcher matcher;
        matcher = RequirementMatcherFactory.createVersionMatcher("1.5.2");
        assertFalse(matcher.matches("1.5"));
        assertTrue(matcher.matches("1.5.2"));
        assertFalse(matcher.matches("[1.4,1.5)"));
        assertFalse(matcher.matches("[1.5,1.5.2)"));
        assertFalse(matcher.matches("(1.5.2,1.6)"));
        assertTrue(matcher.matches("(1.4,1.5.2]"));
        assertTrue(matcher.matches("(1.5,)"));
        assertEquals("1.5.2", matcher.toString());

        // Ensure it is not printed as 1.5.0
        matcher = RequirementMatcherFactory.createVersionMatcher("1.5");
        assertEquals("1.5", matcher.toString());
    }
}
