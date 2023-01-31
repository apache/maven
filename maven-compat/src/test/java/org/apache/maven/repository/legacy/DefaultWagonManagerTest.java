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
package org.apache.maven.repository.legacy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.apache.maven.wagon.observers.Debug;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class DefaultWagonManagerTest extends PlexusTestCase {
    private DefaultWagonManager wagonManager;

    private TransferListener transferListener = new Debug();

    private ArtifactFactory artifactFactory;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    protected void setUp() throws Exception {
        super.setUp();
        wagonManager = (DefaultWagonManager) lookup(WagonManager.class);
        artifactFactory = lookup(ArtifactFactory.class);
        artifactRepositoryFactory = lookup(ArtifactRepositoryFactory.class);
    }

    @Override
    protected void tearDown() throws Exception {
        wagonManager = null;
        artifactFactory = null;
        super.tearDown();
    }

    public void testUnnecessaryRepositoryLookup() throws Exception {
        Artifact artifact = createTestPomArtifact("target/test-data/get-missing-pom");

        List<ArtifactRepository> repos = new ArrayList<>();
        repos.add(artifactRepositoryFactory.createArtifactRepository(
                "repo1", "string://url1", new ArtifactRepositoryLayoutStub(), null, null));
        repos.add(artifactRepositoryFactory.createArtifactRepository(
                "repo2", "string://url2", new ArtifactRepositoryLayoutStub(), null, null));

        StringWagon wagon = (StringWagon) wagonManager.getWagon("string");
        wagon.addExpectedContent(repos.get(0).getLayout().pathOf(artifact), "expected");
        wagon.addExpectedContent(repos.get(1).getLayout().pathOf(artifact), "expected");

        class TransferListener extends AbstractTransferListener {
            public List<TransferEvent> events = new ArrayList<>();

            @Override
            public void transferInitiated(TransferEvent transferEvent) {
                events.add(transferEvent);
            }
        }

        TransferListener listener = new TransferListener();
        wagonManager.getArtifact(artifact, repos, listener, false);
        assertEquals(1, listener.events.size());
    }

    public void testGetMissingJar() throws TransferFailedException, UnsupportedProtocolException, IOException {
        Artifact artifact = createTestArtifact("target/test-data/get-missing-jar", "jar");

        ArtifactRepository repo = createStringRepo();

        try {
            wagonManager.getArtifact(artifact, repo, null, false);

            fail();
        } catch (ResourceDoesNotExistException e) {
            assertTrue(true);
        }

        assertFalse(artifact.getFile().exists());
    }

    public void testGetMissingJarForced() throws TransferFailedException, UnsupportedProtocolException, IOException {
        Artifact artifact = createTestArtifact("target/test-data/get-missing-jar", "jar");

        ArtifactRepository repo = createStringRepo();

        try {
            wagonManager.getArtifact(artifact, repo, null, true);

            fail();
        } catch (ResourceDoesNotExistException e) {
            assertTrue(true);
        }

        assertFalse(artifact.getFile().exists());
    }

    public void testGetRemoteJar()
            throws TransferFailedException, ResourceDoesNotExistException, UnsupportedProtocolException, IOException,
                    AuthorizationException {
        Artifact artifact = createTestArtifact("target/test-data/get-remote-jar", "jar");

        ArtifactRepository repo = createStringRepo();

        StringWagon wagon = (StringWagon) wagonManager.getWagon("string");
        wagon.addExpectedContent(repo.getLayout().pathOf(artifact), "expected");

        wagonManager.getArtifact(artifact, repo, null, false);

        assertTrue(artifact.getFile().exists());
        assertEquals("expected", FileUtils.fileRead(artifact.getFile(), "UTF-8"));
    }

    private Artifact createTestPomArtifact(String directory) throws IOException {
        File testData = getTestFile(directory);
        FileUtils.deleteDirectory(testData);
        testData.mkdirs();

        Artifact artifact = artifactFactory.createProjectArtifact("test", "test", "1.0");
        artifact.setFile(new File(testData, "test-1.0.pom"));
        assertFalse(artifact.getFile().exists());
        return artifact;
    }

    private Artifact createTestArtifact(String directory, String type) throws IOException {
        return createTestArtifact(directory, "1.0", type);
    }

    private Artifact createTestArtifact(String directory, String version, String type) throws IOException {
        File testData = getTestFile(directory);
        FileUtils.deleteDirectory(testData);
        testData.mkdirs();

        Artifact artifact = artifactFactory.createBuildArtifact("test", "test", version, type);
        artifact.setFile(new File(
                testData,
                "test-" + version + "." + artifact.getArtifactHandler().getExtension()));
        assertFalse(artifact.getFile().exists());
        return artifact;
    }

    private ArtifactRepository createStringRepo() {
        return artifactRepositoryFactory.createArtifactRepository(
                "id", "string://url", new ArtifactRepositoryLayoutStub(), null, null);
    }

    /**
     * Build an ArtifactRepository object.
     *
     * @param id
     * @param url
     * @return
     */
    private ArtifactRepository getRepo(String id, String url) {
        return artifactRepositoryFactory.createArtifactRepository(id, url, new DefaultRepositoryLayout(), null, null);
    }

    /**
     * Build an ArtifactRepository object.
     *
     * @param id
     * @return
     */
    private ArtifactRepository getRepo(String id) {
        return getRepo(id, "http://something");
    }

    public void testDefaultWagonManager() throws Exception {
        assertWagon("a");

        assertWagon("b");

        assertWagon("c");

        assertWagon("string");

        try {
            assertWagon("d");

            fail("Expected :" + UnsupportedProtocolException.class.getName());
        } catch (UnsupportedProtocolException e) {
            // ok
            assertTrue(true);
        }
    }

    /**
     * Check that transfer listeners are properly removed after getArtifact and putArtifact
     */
    public void testWagonTransferListenerRemovedAfterGetArtifactAndPutArtifact() throws Exception {
        Artifact artifact = createTestArtifact("target/test-data/transfer-listener", "jar");
        ArtifactRepository repo = createStringRepo();
        StringWagon wagon = (StringWagon) wagonManager.getWagon("string");
        wagon.addExpectedContent(repo.getLayout().pathOf(artifact), "expected");

        /* getArtifact */
        assertFalse(
                "Transfer listener is registered before test",
                wagon.getTransferEventSupport().hasTransferListener(transferListener));
        wagonManager.getArtifact(artifact, repo, transferListener, false);
        assertFalse(
                "Transfer listener still registered after getArtifact",
                wagon.getTransferEventSupport().hasTransferListener(transferListener));

        /* putArtifact */
        File sampleFile = getTestFile("target/test-file");
        FileUtils.fileWrite(sampleFile.getAbsolutePath(), "sample file");

        assertFalse(
                "Transfer listener is registered before test",
                wagon.getTransferEventSupport().hasTransferListener(transferListener));
        wagonManager.putArtifact(sampleFile, artifact, repo, transferListener);
        assertFalse(
                "Transfer listener still registered after putArtifact",
                wagon.getTransferEventSupport().hasTransferListener(transferListener));
    }

    /**
     * Checks the verification of checksums.
     */
    public void xtestChecksumVerification() throws Exception {
        ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy(
                true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL);

        ArtifactRepository repo = artifactRepositoryFactory.createArtifactRepository(
                "id", "string://url", new ArtifactRepositoryLayoutStub(), policy, policy);

        Artifact artifact = new DefaultArtifact(
                "sample.group",
                "sample-art",
                VersionRange.createFromVersion("1.0"),
                "scope",
                "jar",
                "classifier",
                null);
        artifact.setFile(getTestFile("target/sample-art"));

        StringWagon wagon = (StringWagon) wagonManager.getWagon("string");

        wagon.clearExpectedContent();
        wagon.addExpectedContent("path", "lower-case-checksum");
        wagon.addExpectedContent("path.sha1", "2a25dc564a3b34f68237fc849066cbc7bb7a36a1");

        try {
            wagonManager.getArtifact(artifact, repo, null, false);
        } catch (ChecksumFailedException e) {
            fail("Checksum verification did not pass: " + e.getMessage());
        }

        wagon.clearExpectedContent();
        wagon.addExpectedContent("path", "upper-case-checksum");
        wagon.addExpectedContent("path.sha1", "B7BB97D7D0B9244398D9B47296907F73313663E6");

        try {
            wagonManager.getArtifact(artifact, repo, null, false);
        } catch (ChecksumFailedException e) {
            fail("Checksum verification did not pass: " + e.getMessage());
        }

        wagon.clearExpectedContent();
        wagon.addExpectedContent("path", "expected-failure");
        wagon.addExpectedContent("path.sha1", "b7bb97d7d0b9244398d9b47296907f73313663e6");

        try {
            wagonManager.getArtifact(artifact, repo, null, false);
            fail("Checksum verification did not fail");
        } catch (ChecksumFailedException e) {
            // expected
        }

        wagon.clearExpectedContent();
        wagon.addExpectedContent("path", "lower-case-checksum");
        wagon.addExpectedContent("path.md5", "50b2cf50a103a965efac62b983035cac");

        try {
            wagonManager.getArtifact(artifact, repo, null, false);
        } catch (ChecksumFailedException e) {
            fail("Checksum verification did not pass: " + e.getMessage());
        }

        wagon.clearExpectedContent();
        wagon.addExpectedContent("path", "upper-case-checksum");
        wagon.addExpectedContent("path.md5", "842F568FCCFEB7E534DC72133D42FFDC");

        try {
            wagonManager.getArtifact(artifact, repo, null, false);
        } catch (ChecksumFailedException e) {
            fail("Checksum verification did not pass: " + e.getMessage());
        }

        wagon.clearExpectedContent();
        wagon.addExpectedContent("path", "expected-failure");
        wagon.addExpectedContent("path.md5", "b7bb97d7d0b9244398d9b47296907f73313663e6");

        try {
            wagonManager.getArtifact(artifact, repo, null, false);
            fail("Checksum verification did not fail");
        } catch (ChecksumFailedException e) {
            // expected
        }
    }

    public void testPerLookupInstantiation() throws Exception {
        String protocol = "perlookup";

        Wagon one = wagonManager.getWagon(protocol);
        Wagon two = wagonManager.getWagon(protocol);

        assertNotSame(one, two);
    }

    private void assertWagon(String protocol) throws Exception {
        Wagon wagon = wagonManager.getWagon(protocol);

        assertNotNull("Check wagon, protocol=" + protocol, wagon);
    }

    private final class ArtifactRepositoryLayoutStub implements ArtifactRepositoryLayout {
        public String getId() {
            return "test";
        }

        public String pathOfRemoteRepositoryMetadata(ArtifactMetadata metadata) {
            return "path";
        }

        public String pathOfLocalRepositoryMetadata(ArtifactMetadata metadata, ArtifactRepository repository) {
            return "path";
        }

        public String pathOf(Artifact artifact) {
            return "path";
        }
    }
}
