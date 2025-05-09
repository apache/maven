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
package org.apache.maven.toolchain.merge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.maven.toolchain.io.DefaultToolchainsReader;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.TrackableBase;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenToolchainMergerTest {
    private MavenToolchainMerger merger = new MavenToolchainMerger();

    private DefaultToolchainsReader reader = new DefaultToolchainsReader();

    @Test
    void testMergeNulls() {
        merger.merge(null, null, null);

        PersistedToolchains pt = new PersistedToolchains();
        merger.merge(pt, null, null);
        merger.merge(null, pt, null);
    }

    @Test
    void testMergeJdk() throws Exception {
        try (InputStream isDominant = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream isRecessive = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml")) {
            PersistedToolchains dominant = read(isDominant);
            PersistedToolchains recessive = read(isRecessive);
            assertEquals(2, dominant.getToolchains().size());

            merger.merge(dominant, recessive, TrackableBase.USER_LEVEL);
            assertEquals(2, dominant.getToolchains().size());
        }
    }

    @Test
    void testMergeJdkExtra() throws Exception {
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtraIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extra.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtra = read(jdksExtraIS);
            assertEquals(2, jdks.getToolchains().size());

            merger.merge(jdks, jdksExtra, TrackableBase.USER_LEVEL);
            assertEquals(4, jdks.getToolchains().size());
            assertEquals(2, jdksExtra.getToolchains().size());
        }
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtraIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extra.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtra = read(jdksExtraIS);
            assertEquals(2, jdks.getToolchains().size());

            // switch dominant with recessive
            merger.merge(jdksExtra, jdks, TrackableBase.USER_LEVEL);
            assertEquals(4, jdksExtra.getToolchains().size());
            assertEquals(2, jdks.getToolchains().size());
        }
    }

    @Test
    void testMergeJdkExtend() throws Exception {
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtendIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extend.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtend = read(jdksExtendIS);
            assertEquals(2, jdks.getToolchains().size());

            merger.merge(jdks, jdksExtend, TrackableBase.USER_LEVEL);
            assertEquals(2, jdks.getToolchains().size());
            Xpp3Dom config0 = (Xpp3Dom) jdks.getToolchains().get(0).getConfiguration();
            assertEquals("lib/tools.jar", config0.getChild("toolsJar").getValue());
            assertEquals(2, config0.getChildCount());
            Xpp3Dom config1 = (Xpp3Dom) jdks.getToolchains().get(1).getConfiguration();
            assertEquals(2, config1.getChildCount());
            assertEquals("lib/classes.jar", config1.getChild("toolsJar").getValue());
            assertEquals(2, jdksExtend.getToolchains().size());
        }
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtendIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extend.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtend = read(jdksExtendIS);
            assertEquals(2, jdks.getToolchains().size());

            // switch dominant with recessive
            merger.merge(jdksExtend, jdks, TrackableBase.USER_LEVEL);
            assertEquals(2, jdksExtend.getToolchains().size());
            Xpp3Dom config0 = (Xpp3Dom) jdksExtend.getToolchains().get(0).getConfiguration();
            assertEquals("lib/tools.jar", config0.getChild("toolsJar").getValue());
            assertEquals(2, config0.getChildCount());
            Xpp3Dom config1 = (Xpp3Dom) jdksExtend.getToolchains().get(1).getConfiguration();
            assertEquals(2, config1.getChildCount());
            assertEquals("lib/classes.jar", config1.getChild("toolsJar").getValue());
            assertEquals(2, jdks.getToolchains().size());
        }
    }

    private PersistedToolchains read(InputStream is) throws IOException {
        return reader.read(is, Collections.emptyMap());
    }
}
