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
package org.apache.maven.rtinfo.internal;

import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.PlexusTestCase;

public class DefaultRuntimeInformationTest extends PlexusTestCase {

    public void testGetMavenVersion() throws Exception {
        RuntimeInformation rtInfo = lookup(RuntimeInformation.class);

        String mavenVersion = rtInfo.getMavenVersion();
        assertNotNull(mavenVersion);
        assertTrue(mavenVersion.length() > 0);
    }

    public void testIsMavenVersion() throws Exception {
        RuntimeInformation rtInfo = lookup(RuntimeInformation.class);

        assertTrue(rtInfo.isMavenVersion("2.0"));
        assertFalse(rtInfo.isMavenVersion("9.9"));

        assertTrue(rtInfo.isMavenVersion("[2.0.11,2.1.0),[3.0,)"));
        assertFalse(rtInfo.isMavenVersion("[9.0,)"));

        try {
            rtInfo.isMavenVersion("[3.0,");
            fail("Bad version range wasn't rejected");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            rtInfo.isMavenVersion("");
            fail("Bad version range wasn't rejected");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            rtInfo.isMavenVersion(null);
            fail("Bad version range wasn't rejected");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }
}
