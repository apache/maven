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
package org.apache.maven.repository.internal;

import org.apache.maven.api.Version;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.model.version.ModelVersionParser;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionRangeTest {

    private ModelVersionParser versionParser = new DefaultModelVersionParser(new GenericVersionScheme());

    private Version newVersion(String version) {
        return versionParser.parseVersion(version);
    }

    private VersionRange parseValid(String range) {
        try {
            return versionParser.parseVersionRange(range);
        } catch (VersionParserException e) {
            throw new AssertionError(range + " should be valid but failed to parse due to: " + e.getMessage(), e);
        }
    }

    private void parseInvalid(String range) {
        try {
            versionParser.parseVersionRange(range);
            fail(range + " should be invalid");
        } catch (VersionParserException e) {
            assertTrue(true);
        }
    }

    private void assertContains(VersionRange range, String version) {
        assertTrue(range.contains(newVersion(version)), range + " should contain " + version);
    }

    private void assertNotContains(VersionRange range, String version) {
        assertFalse(range.contains(newVersion(version)), range + " should not contain " + version);
    }

    @Test
    void testLowerBoundInclusiveUpperBoundInclusive() {
        VersionRange range = parseValid("[1,2]");
        assertContains(range, "1");
        assertContains(range, "1.1-SNAPSHOT");
        assertContains(range, "2");
        assertEquals(range, parseValid(range.toString()));
    }

    @Test
    void testLowerBoundInclusiveUpperBoundExclusive() {
        VersionRange range = parseValid("[1.2.3.4.5,1.2.3.4.6)");
        assertContains(range, "1.2.3.4.5");
        assertNotContains(range, "1.2.3.4.6");
        assertEquals(range, parseValid(range.toString()));
    }

    @Test
    void testLowerBoundExclusiveUpperBoundInclusive() {
        VersionRange range = parseValid("(1a,1b]");
        assertNotContains(range, "1a");
        assertContains(range, "1b");
        assertEquals(range, parseValid(range.toString()));
    }

    @Test
    void testLowerBoundExclusiveUpperBoundExclusive() {
        VersionRange range = parseValid("(1,3)");
        assertNotContains(range, "1");
        assertContains(range, "2-SNAPSHOT");
        assertNotContains(range, "3");
        assertEquals(range, parseValid(range.toString()));
    }

    @Test
    void testSingleVersion() {
        VersionRange range = parseValid("[1]");
        assertContains(range, "1");
        assertEquals(range, parseValid(range.toString()));

        range = parseValid("[1,1]");
        assertContains(range, "1");
        assertEquals(range, parseValid(range.toString()));
    }

    @Test
    void testSingleWildcardVersion() {
        VersionRange range = parseValid("[1.2.*]");
        assertContains(range, "1.2-alpha-1");
        assertContains(range, "1.2-SNAPSHOT");
        assertContains(range, "1.2");
        assertContains(range, "1.2.9999999");
        assertNotContains(range, "1.3-rc-1");
        assertEquals(range, parseValid(range.toString()));
    }

    @Test
    void testMissingOpenCloseDelimiter() {
        parseInvalid("1.0");
    }

    @Test
    void testMissingOpenDelimiter() {
        parseInvalid("1.0]");
        parseInvalid("1.0)");
    }

    @Test
    void testMissingCloseDelimiter() {
        parseInvalid("[1.0");
        parseInvalid("(1.0");
    }

    @Test
    void testTooManyVersions() {
        parseInvalid("[1,2,3]");
        parseInvalid("(1,2,3)");
        parseInvalid("[1,2,3)");
    }
}
