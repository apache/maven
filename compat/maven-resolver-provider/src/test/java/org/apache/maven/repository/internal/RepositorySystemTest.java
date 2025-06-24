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
package org.apache.maven.repository.internal;

import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySystemTest extends AbstractRepositoryTestCase {
    @Test
    void testResolveVersionRange() throws Exception {
        // VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
        //                throws VersionRangeResolutionException;

    }

    @Test
    void testResolveVersion() throws Exception {
        // VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
        //                throws VersionResolutionException;
    }

    @Test
    void testReadArtifactDescriptor() throws Exception {
        Artifact artifact = new DefaultArtifact("ut.simple:artifact:extension:classifier:1.0");

        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact(artifact);
        request.addRepository(newTestRepository());

        ArtifactDescriptorResult result = system.readArtifactDescriptor(session, request);

        List<Dependency> deps = result.getDependencies();
        assertEquals(2, deps.size());
        checkUtSimpleArtifactDependencies(deps.get(0), deps.get(1));
    }

    /**
     * check ut.simple:artifact:1.0 dependencies
     */
    private void checkUtSimpleArtifactDependencies(Dependency dep1, Dependency dep2) {
        assertEquals("compile", dep1.getScope());
        assertFalse(dep1.isOptional());
        assertEquals(0, dep1.getExclusions().size());
        Artifact depArtifact = dep1.getArtifact();
        assertEquals("ut.simple", depArtifact.getGroupId());
        assertEquals("dependency", depArtifact.getArtifactId());
        assertEquals("1.0", depArtifact.getVersion());
        assertEquals("1.0", depArtifact.getBaseVersion());
        assertNull(depArtifact.getFile());
        assertFalse(depArtifact.isSnapshot());
        assertEquals("", depArtifact.getClassifier());
        assertEquals("jar", depArtifact.getExtension());
        assertEquals("java", depArtifact.getProperty("language", null));
        assertEquals("jar", depArtifact.getProperty("type", null));
        assertEquals("true", depArtifact.getProperty("constitutesBuildPath", null));
        assertEquals("false", depArtifact.getProperty("includesDependencies", null));
        assertEquals(4, depArtifact.getProperties().size());

        assertEquals("compile", dep2.getScope());
        assertFalse(dep2.isOptional());
        assertEquals(0, dep2.getExclusions().size());
        depArtifact = dep2.getArtifact();
        assertEquals("ut.simple", depArtifact.getGroupId());
        assertEquals("dependency", depArtifact.getArtifactId());
        assertEquals("1.0", depArtifact.getVersion());
        assertEquals("1.0", depArtifact.getBaseVersion());
        assertNull(depArtifact.getFile());
        assertFalse(depArtifact.isSnapshot());
        assertEquals("sources", depArtifact.getClassifier());
        assertEquals("jar", depArtifact.getExtension());
        assertEquals("java", depArtifact.getProperty("language", null));
        assertEquals(
                "jar", depArtifact.getProperty("type", null)); // shouldn't it be java-sources given the classifier?
        assertEquals(
                "true",
                depArtifact.getProperty("constitutesBuildPath", null)); // shouldn't it be false given the classifier?
        assertEquals("false", depArtifact.getProperty("includesDependencies", null));
        assertEquals(4, depArtifact.getProperties().size());
    }

    @Test
    void testCollectDependencies() throws Exception {
        Artifact artifact = new DefaultArtifact("ut.simple:artifact:extension:classifier:1.0");
        // notice: extension and classifier not really used in this test...

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, null));
        collectRequest.addRepository(newTestRepository());

        CollectResult collectResult = system.collectDependencies(session, collectRequest);

        List<DependencyNode> nodes = collectResult.getRoot().getChildren();
        assertEquals(2, nodes.size());
        checkUtSimpleArtifactDependencies(
                nodes.get(0).getDependency(), nodes.get(1).getDependency());
    }

    @Test
    void testResolveArtifact() throws Exception {
        Artifact artifact = new DefaultArtifact("ut.simple:artifact:1.0");

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.addRepository(newTestRepository());

        ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
        checkArtifactResult(artifactResult, "artifact-1.0.jar");

        artifact = new DefaultArtifact("ut.simple:artifact:zip:1.0");
        artifactRequest.setArtifact(artifact);
        artifactResult = system.resolveArtifact(session, artifactRequest);
        checkArtifactResult(artifactResult, "artifact-1.0.zip");

        artifact = new DefaultArtifact("ut.simple:artifact:zip:classifier:1.0");
        artifactRequest.setArtifact(artifact);
        artifactResult = system.resolveArtifact(session, artifactRequest);
        checkArtifactResult(artifactResult, "artifact-1.0-classifier.zip");
    }

    private void checkArtifactResult(ArtifactResult result, String filename) {
        assertFalse(result.isMissing());
        assertTrue(result.isResolved());
        Artifact artifact = result.getArtifact();
        assertNotNull(artifact.getFile());
        assertEquals(filename, artifact.getFile().getName());
    }

    @Test
    void testResolveArtifacts() throws Exception {
        ArtifactRequest req1 = new ArtifactRequest();
        req1.setArtifact(new DefaultArtifact("ut.simple:artifact:1.0"));
        req1.addRepository(newTestRepository());

        ArtifactRequest req2 = new ArtifactRequest();
        req2.setArtifact(new DefaultArtifact("ut.simple:artifact:zip:1.0"));
        req2.addRepository(newTestRepository());

        ArtifactRequest req3 = new ArtifactRequest();
        req3.setArtifact(new DefaultArtifact("ut.simple:artifact:zip:classifier:1.0"));
        req3.addRepository(newTestRepository());

        List<ArtifactRequest> requests = Arrays.asList(req1, req2, req3);

        List<ArtifactResult> results = system.resolveArtifacts(session, requests);

        assertEquals(3, results.size());
        checkArtifactResult(results.get(0), "artifact-1.0.jar");
        checkArtifactResult(results.get(1), "artifact-1.0.zip");
        checkArtifactResult(results.get(2), "artifact-1.0-classifier.zip");
    }

    @Test
    void testResolveMetadata() throws Exception {
        // List<MetadataResult> resolveMetadata( RepositorySystemSession session,
        //                                      Collection<? extends MetadataRequest> requests );
    }

    @Test
    void testInstall() throws Exception {
        // InstallResult install( RepositorySystemSession session, InstallRequest request )
        //                throws InstallationException;
        // release, snapshot unique ou non unique, attachment
    }

    @Test
    void testDeploy() throws Exception {
        // DeployResult deploy( RepositorySystemSession session, DeployRequest request )
        //                throws DeploymentException;
    }

    @Test
    void testNewLocalRepositoryManager() throws Exception {
        // LocalRepositoryManager newLocalRepositoryManager( LocalRepository localRepository );
    }

    @Test
    void testNewSyncContext() throws Exception {
        // SyncContext newSyncContext( RepositorySystemSession session, boolean shared );
    }
}
