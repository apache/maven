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

import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.model.version.ModelVersionParser;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class ModelVersionParserTest {

    private final ModelVersionParser versionParser = new DefaultModelVersionParser(new GenericVersionScheme());

    private VersionParserException parseInvalid(String constraint) {
        try {
            versionParser.parseVersionConstraint(constraint);
            fail("expected exception for constraint " + constraint);
            return null;
        } catch (VersionParserException e) {
            return e;
        }
    }

    @Test
    void testEnumeratedVersions() throws VersionParserException {
        VersionConstraint c = versionParser.parseVersionConstraint("1.0");
        assertEquals("1.0", c.getRecommendedVersion().toString());
        assertTrue(c.contains(versionParser.parseVersion("1.0")));

        c = versionParser.parseVersionConstraint("[1.0]");
        assertNull(c.getRecommendedVersion());
        assertTrue(c.contains(versionParser.parseVersion("1.0")));

        c = versionParser.parseVersionConstraint("[1.0],[2.0]");
        assertTrue(c.contains(versionParser.parseVersion("1.0")));
        assertTrue(c.contains(versionParser.parseVersion("2.0")));

        c = versionParser.parseVersionConstraint("[1.0],[2.0],[3.0]");
        assertContains(c, "1.0", "2.0", "3.0");
        assertNotContains(c, "1.5");

        c = versionParser.parseVersionConstraint("[1,3),(3,5)");
        assertContains(c, "1", "2", "4");
        assertNotContains(c, "3", "5");

        c = versionParser.parseVersionConstraint("[1,3),(3,)");
        assertContains(c, "1", "2", "4");
        assertNotContains(c, "3");
    }

    private void assertNotContains(VersionConstraint c, String... versions) {
        assertContains(String.format("%s: %%s should not be contained\n", c.toString()), c, false, versions);
    }

    private void assertContains(String msg, VersionConstraint c, boolean b, String... versions) {
        for (String v : versions) {
            assertEquals(b, c.contains(versionParser.parseVersion(v)), String.format(msg, v));
        }
    }

    private void assertContains(VersionConstraint c, String... versions) {
        assertContains(String.format("%s: %%s should be contained\n", c.toString()), c, true, versions);
    }

    @Test
    void testInvalid() {
        parseInvalid("[1,");
        parseInvalid("[1,2],(3,");
        parseInvalid("[1,2],3");
    }

    @Test
    void testSameUpperAndLowerBound() throws VersionParserException {
        VersionConstraint c = versionParser.parseVersionConstraint("[1.0]");
        assertEquals("[1.0,1.0]", c.toString());
        VersionConstraint c2 = versionParser.parseVersionConstraint(c.toString());
        assertEquals(c, c2);
        assertTrue(c.contains(versionParser.parseVersion("1.0")));
    }
}
