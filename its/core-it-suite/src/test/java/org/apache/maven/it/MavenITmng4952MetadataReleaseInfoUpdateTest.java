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
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4952">MNG-4952</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4952MetadataReleaseInfoUpdateTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4952MetadataReleaseInfoUpdateTest() {
        super("[2.0.3,3.0-alpha-1),[3.0.2,)");
    }

    /**
     * Verify that the metadata's RELEASE field gets updated upon deployment of a new version.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4952");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4952");

        Map<String, String> props = verifier.newDefaultFilterMap();

        props.put("@version@", "1.0");
        verifier.filterFile("pom-template.xml", "pom.xml", props);
        verifier.setLogFileName("log-1.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        props.put("@version@", "2.0");
        verifier.filterFile("pom-template.xml", "pom.xml", props);
        verifier.setLogFileName("log-2.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File metadataFile = new File(testDir, "target/repo/org/apache/maven/its/mng4952/test/maven-metadata.xml");
        String xml = Files.readString(metadataFile.toPath());
        assertTrue(xml.matches("(?s).*<release>2\\.0</release>.*"), xml);
    }
}
