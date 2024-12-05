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
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4353">MNG-4353</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4353PluginDependencyResolutionFromPomRepoTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4353PluginDependencyResolutionFromPomRepoTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that repos given in a plugin's POM are considered while resolving the plugin dependencies.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4353").getCanonicalFile();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4353");
        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
        verifier.filterFile(
                "repo-1/org/apache/maven/its/mng4353/maven-mng4353-plugin/0.1/template.pom",
                "repo-1/org/apache/maven/its/mng4353/maven-mng4353-plugin/0.1/maven-mng4353-plugin-0.1" + ".pom",
                "UTF-8",
                filterProps);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/touch.properties");
        assertEquals("passed", props.getProperty("test"));
    }
}
