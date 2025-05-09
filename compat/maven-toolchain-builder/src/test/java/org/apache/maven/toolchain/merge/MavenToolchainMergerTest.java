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

import static org.assertj.core.api.Assertions.assertThat;

class MavenToolchainMergerTest {
    private MavenToolchainMerger merger = new MavenToolchainMerger();

    private DefaultToolchainsReader reader = new DefaultToolchainsReader();

    @Test
    void mergeNulls() {
        merger.merge(null, null, null);

        PersistedToolchains pt = new PersistedToolchains();
        merger.merge(pt, null, null);
        merger.merge(null, pt, null);
    }

    @Test
    void mergeJdk() throws Exception {
        try (InputStream isDominant = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream isRecessive = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml")) {
            PersistedToolchains dominant = read(isDominant);
            PersistedToolchains recessive = read(isRecessive);
            assertThat(dominant.getToolchains().size()).isEqualTo(2);

            merger.merge(dominant, recessive, TrackableBase.USER_LEVEL);
            assertThat(dominant.getToolchains().size()).isEqualTo(2);
        }
    }

    @Test
    void mergeJdkExtra() throws Exception {
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtraIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extra.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtra = read(jdksExtraIS);
            assertThat(jdks.getToolchains().size()).isEqualTo(2);

            merger.merge(jdks, jdksExtra, TrackableBase.USER_LEVEL);
            assertThat(jdks.getToolchains().size()).isEqualTo(4);
            assertThat(jdksExtra.getToolchains().size()).isEqualTo(2);
        }
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtraIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extra.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtra = read(jdksExtraIS);
            assertThat(jdks.getToolchains().size()).isEqualTo(2);

            // switch dominant with recessive
            merger.merge(jdksExtra, jdks, TrackableBase.USER_LEVEL);
            assertThat(jdksExtra.getToolchains().size()).isEqualTo(4);
            assertThat(jdks.getToolchains().size()).isEqualTo(2);
        }
    }

    @Test
    void mergeJdkExtend() throws Exception {
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtendIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extend.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtend = read(jdksExtendIS);
            assertThat(jdks.getToolchains().size()).isEqualTo(2);

            merger.merge(jdks, jdksExtend, TrackableBase.USER_LEVEL);
            assertThat(jdks.getToolchains().size()).isEqualTo(2);
            Xpp3Dom config0 = (Xpp3Dom) jdks.getToolchains().get(0).getConfiguration();
            assertThat(config0.getChild("toolsJar").getValue()).isEqualTo("lib/tools.jar");
            assertThat(config0.getChildCount()).isEqualTo(2);
            Xpp3Dom config1 = (Xpp3Dom) jdks.getToolchains().get(1).getConfiguration();
            assertThat(config1.getChildCount()).isEqualTo(2);
            assertThat(config1.getChild("toolsJar").getValue()).isEqualTo("lib/classes.jar");
            assertThat(jdksExtend.getToolchains().size()).isEqualTo(2);
        }
        try (InputStream jdksIS = MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks.xml");
                InputStream jdksExtendIS =
                        MavenToolchainMergerTest.class.getResourceAsStream("toolchains-jdks-extend.xml")) {
            PersistedToolchains jdks = read(jdksIS);
            PersistedToolchains jdksExtend = read(jdksExtendIS);
            assertThat(jdks.getToolchains().size()).isEqualTo(2);

            // switch dominant with recessive
            merger.merge(jdksExtend, jdks, TrackableBase.USER_LEVEL);
            assertThat(jdksExtend.getToolchains().size()).isEqualTo(2);
            Xpp3Dom config0 = (Xpp3Dom) jdksExtend.getToolchains().get(0).getConfiguration();
            assertThat(config0.getChild("toolsJar").getValue()).isEqualTo("lib/tools.jar");
            assertThat(config0.getChildCount()).isEqualTo(2);
            Xpp3Dom config1 = (Xpp3Dom) jdksExtend.getToolchains().get(1).getConfiguration();
            assertThat(config1.getChildCount()).isEqualTo(2);
            assertThat(config1.getChild("toolsJar").getValue()).isEqualTo("lib/classes.jar");
            assertThat(jdks.getToolchains().size()).isEqualTo(2);
        }
    }

    private PersistedToolchains read(InputStream is) throws IOException {
        return reader.read(is, Collections.emptyMap());
    }
}
