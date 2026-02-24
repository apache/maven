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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1021">MNG-1021</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng1021EqualAttachmentBuildNumberTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that source attachments have the same build number and timestamp as the main
     * artifact when deployed.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG1021() throws Exception {
        Path testDir = extractResources("mng-1021");
        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("repo");
        verifier.deleteArtifacts("org.apache.maven.its.mng1021");
        verifier.addCliArgument("-Dmaven.consumer.pom=false");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng1021", "test", "1-SNAPSHOT", "pom");
        verifier.verifyArtifactPresent("org.apache.maven.its.mng1021", "test", "1-SNAPSHOT", "jar");

        String dir = "repo/org/apache/maven/its/mng1021/test/";
        String snapshot = getSnapshotVersion(testDir.resolve(dir + "1-SNAPSHOT"));
        assertTrue(snapshot.endsWith("-1"), snapshot);

        verifier.verifyFilePresent(dir + "maven-metadata.xml");
        verifier.verifyFilePresent(dir + "maven-metadata.xml.md5");
        verifier.verifyFilePresent(dir + "maven-metadata.xml.sha1");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/maven-metadata.xml");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/maven-metadata.xml.md5");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/maven-metadata.xml.sha1");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + ".pom");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + ".pom.md5");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + ".pom.sha1");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + ".jar");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + ".jar.md5");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + ".jar.sha1");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + "-it.jar");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + "-it.jar.md5");
        verifier.verifyFilePresent(dir + "1-SNAPSHOT/test-" + snapshot + "-it.jar.sha1");
    }

    private String getSnapshotVersion(Path artifactDir) throws IOException {
        try (var stream = Files.list(artifactDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".pom"))
                    .findFirst()
                    .map(pomName -> pomName.substring("test-".length(), pomName.length() - ".pom".length()))
                    .orElseThrow(() -> new IllegalStateException("POM not found in " + artifactDir));
        }
    }}
