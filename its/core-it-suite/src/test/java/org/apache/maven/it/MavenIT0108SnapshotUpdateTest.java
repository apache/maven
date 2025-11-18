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
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Downloads a snapshot dependency that was deployed with uniqueVersion = false, and checks it can be
 * updated. See <a href="https://issues.apache.org/jira/browse/MNG-1908">MNG-1908</a>.
 */
@Disabled("flaky test, see MNG-3137")
public class MavenIT0108SnapshotUpdateTest extends AbstractMavenIntegrationTestCase {

    private Verifier verifier;

    private Path artifact;

    private Path repository;

    private Path localRepoFile;

    private static final int TIME_OFFSET = 50000;

    @BeforeEach
    protected void setUp() throws Exception {
        Path testDir = extractResources("it0108");
        verifier = newVerifier(testDir);
        localRepoFile = getLocalRepoFile(verifier);
        deleteLocalArtifact(verifier, localRepoFile);

        repository = testDir.resolve("repository");
        recreateRemoteRepository(repository);

        // create artifact in repository (TODO: into verifier)
        artifact = repository.resolve(
                "org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-core-it-support-1.0-SNAPSHOT.jar");
        Files.createDirectories(artifact.getParent());
        Files.writeString(artifact, "originalArtifact");

        verifier.verifyArtifactNotPresent("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar");
    }

    @Test
    public void testSnapshotUpdated() throws Exception {
        verifier.addCliArgument("package");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifyArtifactContent("originalArtifact");

        // set in the past to ensure it is downloaded
        Files.setLastModifiedTime(localRepoFile, FileTime.fromMillis(System.currentTimeMillis() - TIME_OFFSET));

        Files.writeString(artifact, "updatedArtifact");

        verifier.addCliArgument("package");
        verifier.execute();

        verifyArtifactContent("updatedArtifact");

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testSnapshotUpdatedWithMetadata() throws Exception {
        Path metadata = repository.resolve("org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml");
        Files.writeString(
                metadata,
                constructMetadata("1", System.currentTimeMillis() - TIME_OFFSET, true));

        verifier.addCliArgument("package");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifyArtifactContent("originalArtifact");

        Files.writeString(artifact, "updatedArtifact");
        metadata = repository.resolve("org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml");
        Files.writeString(
                metadata, constructMetadata("2", System.currentTimeMillis(), true));

        verifier.addCliArgument("package");
        verifier.execute();

        verifyArtifactContent("updatedArtifact");

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testSnapshotUpdatedWithLocalMetadata() throws Exception {
        Path localMetadata = getMetadataFile("org/apache/maven", "maven-core-it-support", "1.0-SNAPSHOT");

        FileUtils.deleteDirectory(localMetadata.getParent().toFile());
        assertFalse(Files.exists(localMetadata.getParent()));
        Files.createDirectories(localMetadata.getParent());

        Path metadata = repository.resolve("org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml");
        Files.writeString(
                metadata,
                constructMetadata("1", System.currentTimeMillis() - TIME_OFFSET, true));

        verifier.addCliArgument("package");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifyArtifactContent("originalArtifact");
        assertFalse(Files.exists(localMetadata));

        Files.writeString(localRepoFile, "localArtifact");
        Files.writeString(
                localMetadata,
                constructLocalMetadata("org.apache.maven", "maven-core-it-support", System.currentTimeMillis(), true));
        // update the remote file, but we shouldn't be looking
        Files.setLastModifiedTime(artifact, FileTime.fromMillis(System.currentTimeMillis()));

        verifier.addCliArgument("package");
        verifier.execute();

        verifyArtifactContent("localArtifact");

        verifier.verifyErrorFreeLog();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        Files.writeString(
                localMetadata,
                constructLocalMetadata("org.apache.maven", "maven-core-it-support", cal.getTimeInMillis(), true));
        Files.writeString(
                metadata, constructMetadata("2", System.currentTimeMillis() - 2000, true));
        Files.setLastModifiedTime(artifact, FileTime.fromMillis(System.currentTimeMillis()));

        verifier.addCliArgument("package");
        verifier.execute();

        verifyArtifactContent("originalArtifact");

        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testSnapshotUpdatedWithMetadataUsingFileTimestamp() throws Exception {
        Path metadata = repository.resolve("org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml");
        Files.writeString(
                metadata,
                constructMetadata("1", System.currentTimeMillis() - TIME_OFFSET, false));
        Files.setLastModifiedTime(metadata, FileTime.fromMillis(System.currentTimeMillis() - TIME_OFFSET));

        verifier.addCliArgument("package");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        verifyArtifactContent("originalArtifact");

        Files.writeString(artifact, "updatedArtifact");
        metadata = repository.resolve("org/apache/maven/maven-core-it-support/1.0-SNAPSHOT/maven-metadata.xml");
        Files.writeString(
                metadata, constructMetadata("2", System.currentTimeMillis(), false));

        verifier.addCliArgument("package");
        verifier.execute();

        verifyArtifactContent("updatedArtifact");

        verifier.verifyErrorFreeLog();
    }

    private Path getMetadataFile(String groupId, String artifactId, String version) {
        return Paths.get(verifier.getArtifactMetadataPath(groupId, artifactId, version, "maven-metadata-local.xml"));
    }

    private void verifyArtifactContent(String s) throws IOException, VerificationException {
        verifier.verifyArtifactPresent("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar");
        verifier.verifyArtifactContent("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar", s);
    }

    private static Path deleteLocalArtifact(Verifier verifier, Path localRepoFile) throws IOException {
        verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar");
        // this is to delete metadata - TODO: incorporate into deleteArtifact in verifier
        FileUtils.deleteDirectory(localRepoFile.getParent().toFile());
        return localRepoFile;
    }

    private static Path getLocalRepoFile(Verifier verifier) {
        return Paths.get(verifier.getArtifactPath("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar"));
    }

    private static void recreateRemoteRepository(Path repository) throws IOException {
        // create a repository (TODO: into verifier)
        FileUtils.deleteDirectory(repository.toFile());
        assertFalse(Files.exists(repository));
        Files.createDirectories(repository);
    }

    private String constructMetadata(String buildNumber, long timestamp, boolean writeLastUpdated) {
        String ts = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date(timestamp));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata>\n" + "<groupId>org.apache.maven</groupId>\n"
                + "<artifactId>maven-core-it-support</artifactId>\n"
                + "<version>1.0-SNAPSHOT</version>\n" + "<versioning>\n"
                + "<snapshot>\n" + "<buildNumber>" + buildNumber + "</buildNumber>\n" + "</snapshot>\n"
                + (writeLastUpdated ? "<lastUpdated>" + ts + "</lastUpdated>\n" : "")
                + "</versioning>\n" + "</metadata>";
    }

    private String constructLocalMetadata(String groupId, String artifactId, long timestamp, boolean writeLastUpdated) {
        String ts = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date(timestamp));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata>\n" + "  <groupId>" + groupId + "</groupId>\n"
                + "  <artifactId>"
                + artifactId + "</artifactId>\n" + "  <version>1.0-SNAPSHOT</version>\n" + "  <versioning>\n"
                + "    <snapshot>\n" + "      <localCopy>true</localCopy>\n" + "    </snapshot>\n"
                + (writeLastUpdated ? "    <lastUpdated>" + ts + "</lastUpdated>\n" : "")
                + "  </versioning>\n" + "</metadata>";
    }
}
