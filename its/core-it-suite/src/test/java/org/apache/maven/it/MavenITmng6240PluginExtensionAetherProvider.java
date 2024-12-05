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
package org.apache.maven.it;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6240">MNG-6240</a>.
 *
 * @author gboue
 */
public class MavenITmng6240PluginExtensionAetherProvider extends AbstractMavenIntegrationTestCase {
    public MavenITmng6240PluginExtensionAetherProvider() {
        super("[3.5.1,)");
    }

    /**
     * <p>
     * Checks that there are no duplicate classes from maven-aether-provider on the classpath, when the build has a
     * plugin extension that depends on the former maven-aether-provider, renamed to maven-resolver-provider.
     * <p>
     * One way to do this is to look at MetadataGeneratorFactory, which is a class of this module: there should only be
     * two instances (one of SnapshotMetadataGeneratorFactory and VersionsMetadataGeneratorFactory). When there are
     * more, the Deploy Plugin will perform the download / upload of artifact metadata multiple times. This can be
     * verified easily from the logs.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testPluginExtensionDependingOnMavenAetherProvider() throws Exception {
        File testDir = extractResources("/mng-6240-plugin-extension-aether-provider");
        File pluginDir = new File(testDir, "plugin-extension");
        File projectDir = new File(testDir, "project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.setEnvironmentVariable("CI", "false");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.setEnvironmentVariable("CI", "false");
        verifier.addCliArgument("deploy");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLogLines();
        long count = lines.stream()
                .filter(line ->
                        line.endsWith("/repo/org/apache/maven/its/mng6240/project/1.0-SNAPSHOT/maven-metadata.xml"))
                .count();
        assertEquals(2L, count); // 1 from download, 1 from upload
    }
}
