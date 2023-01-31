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

import java.io.InputStream;

import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.apache.maven.toolchain.model.TrackableBase;
import org.apache.maven.toolchain.model.io.xpp3.MavenToolchainsXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MavenToolchainMergerTest {
    private MavenToolchainMerger merger = new MavenToolchainMerger();

    private MavenToolchainsXpp3Reader reader = new MavenToolchainsXpp3Reader();

    @Test
    public void testMergeNulls() {
        merger.merge(null, null, null);

        PersistedToolchains pt = new PersistedToolchains();
        merger.merge(pt, null, null);
        merger.merge(null, pt, null);
    }

    @Test
    public void testMergeJdk() throws Exception {
        try (InputStream isDominant = ToolchainModel.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream isRecessive = ToolchainModel.class.getResourceAsStream("toolchains-jdks.xml")) {
            PersistedToolchains dominant = reader.read(isDominant);
            PersistedToolchains recessive = reader.read(isRecessive);
            assertEquals(2, dominant.getToolchains().size());

            merger.merge(dominant, recessive, TrackableBase.USER_LEVEL);
            assertEquals(2, dominant.getToolchains().size());
        }
    }

    @Test
    public void testMergeJdkExtra() throws Exception {
        try (InputStream jdksIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtraIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks-extra.xml")) {
            PersistedToolchains jdks = reader.read(jdksIS);
            PersistedToolchains jdksExtra = reader.read(jdksExtraIS);
            assertEquals(2, jdks.getToolchains().size());

            merger.merge(jdks, jdksExtra, TrackableBase.USER_LEVEL);
            assertEquals(4, jdks.getToolchains().size());
            assertEquals(2, jdksExtra.getToolchains().size());
        }
        try (InputStream jdksIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtraIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks-extra.xml")) {
            PersistedToolchains jdks = reader.read(jdksIS);
            PersistedToolchains jdksExtra = reader.read(jdksExtraIS);
            assertEquals(2, jdks.getToolchains().size());

            // switch dominant with recessive
            merger.merge(jdksExtra, jdks, TrackableBase.USER_LEVEL);
            assertEquals(4, jdksExtra.getToolchains().size());
            assertEquals(2, jdks.getToolchains().size());
        }
    }

    @Test
    public void testMergeJdkExtend() throws Exception {
        try (InputStream jdksIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtendIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks-extend.xml")) {
            PersistedToolchains jdks = reader.read(jdksIS);
            PersistedToolchains jdksExtend = reader.read(jdksExtendIS);
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
        try (InputStream jdksIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtendIS = ToolchainModel.class.getResourceAsStream("toolchains-jdks-extend.xml")) {
            PersistedToolchains jdks = reader.read(jdksIS);
            PersistedToolchains jdksExtend = reader.read(jdksExtendIS);
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
}
