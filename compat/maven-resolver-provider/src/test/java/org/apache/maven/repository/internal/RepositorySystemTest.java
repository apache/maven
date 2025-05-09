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

import static org.assertj.core.api.Assertions.assertThat;

class RepositorySystemTest extends AbstractRepositoryTestCase {
    @Test
    void resolveVersionRange() throws Exception {
        // VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
        //                throws VersionRangeResolutionException;

    }

    @Test
    void resolveVersion() throws Exception {
        // VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
        //                throws VersionResolutionException;
    }

    @Test
    void readArtifactDescriptor() throws Exception {
        Artifact artifact = new DefaultArtifact("ut.simple:artifact:extension:classifier:1.0");

        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact(artifact);
        request.addRepository(newTestRepository());

        ArtifactDescriptorResult result = system.readArtifactDescriptor(session, request);

        List<Dependency> deps = result.getDependencies();
        assertThat(deps.size()).isEqualTo(2);
        checkUtSimpleArtifactDependencies(deps.get(0), deps.get(1));
    }

    /**
     * check ut.simple:artifact:1.0 dependencies
     */
    private void checkUtSimpleArtifactDependencies(Dependency dep1, Dependency dep2) {
        assertThat(dep1.getScope()).isEqualTo("compile");
        assertThat(dep1.isOptional()).isFalse();
        assertThat(dep1.getExclusions().size()).isEqualTo(0);
        Artifact depArtifact = dep1.getArtifact();
        assertThat(depArtifact.getGroupId()).isEqualTo("ut.simple");
        assertThat(depArtifact.getArtifactId()).isEqualTo("dependency");
        assertThat(depArtifact.getVersion()).isEqualTo("1.0");
        assertThat(depArtifact.getBaseVersion()).isEqualTo("1.0");
        assertThat(depArtifact.getFile()).isNull();
        assertThat(depArtifact.isSnapshot()).isFalse();
        assertThat(depArtifact.getClassifier()).isEqualTo("");
        assertThat(depArtifact.getExtension()).isEqualTo("jar");
        assertThat(depArtifact.getProperty("language", null)).isEqualTo("java");
        assertThat(depArtifact.getProperty("type", null)).isEqualTo("jar");
        assertThat(depArtifact.getProperty("constitutesBuildPath", null)).isEqualTo("true");
        assertThat(depArtifact.getProperty("includesDependencies", null)).isEqualTo("false");
        assertThat(depArtifact.getProperties().size()).isEqualTo(4);

        assertThat(dep2.getScope()).isEqualTo("compile");
        assertThat(dep2.isOptional()).isFalse();
        assertThat(dep2.getExclusions().size()).isEqualTo(0);
        depArtifact = dep2.getArtifact();
        assertThat(depArtifact.getGroupId()).isEqualTo("ut.simple");
        assertThat(depArtifact.getArtifactId()).isEqualTo("dependency");
        assertThat(depArtifact.getVersion()).isEqualTo("1.0");
        assertThat(depArtifact.getBaseVersion()).isEqualTo("1.0");
        assertThat(depArtifact.getFile()).isNull();
        assertThat(depArtifact.isSnapshot()).isFalse();
        assertThat(depArtifact.getClassifier()).isEqualTo("sources");
        assertThat(depArtifact.getExtension()).isEqualTo("jar");
        assertThat(depArtifact.getProperty("language", null)).isEqualTo("java");
        assertThat(depArtifact.getProperty("type", null)).isEqualTo("jar"); // shouldn't it be java-sources given the classifier?
        assertThat(depArtifact.getProperty("constitutesBuildPath", null)).isEqualTo("true"); // shouldn't it be false given the classifier?
        assertThat(depArtifact.getProperty("includesDependencies", null)).isEqualTo("false");
        assertThat(depArtifact.getProperties().size()).isEqualTo(4);
    }

    @Test
    void collectDependencies() throws Exception {
        Artifact artifact = new DefaultArtifact("ut.simple:artifact:extension:classifier:1.0");
        // notice: extension and classifier not really used in this test...

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, null));
        collectRequest.addRepository(newTestRepository());

        CollectResult collectResult = system.collectDependencies(session, collectRequest);

        List<DependencyNode> nodes = collectResult.getRoot().getChildren();
        assertThat(nodes.size()).isEqualTo(2);
        checkUtSimpleArtifactDependencies(
                nodes.get(0).getDependency(), nodes.get(1).getDependency());
    }

    @Test
    void resolveArtifact() throws Exception {
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
        assertThat(result.isMissing()).isFalse();
        assertThat(result.isResolved()).isTrue();
        Artifact artifact = result.getArtifact();
        assertThat(artifact.getFile()).isNotNull();
        assertThat(artifact.getFile().getName()).isEqualTo(filename);
    }

    @Test
    void resolveArtifacts() throws Exception {
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

        assertThat(results.size()).isEqualTo(3);
        checkArtifactResult(results.get(0), "artifact-1.0.jar");
        checkArtifactResult(results.get(1), "artifact-1.0.zip");
        checkArtifactResult(results.get(2), "artifact-1.0-classifier.zip");
    }

    @Test
    void resolveMetadata() throws Exception {
        // List<MetadataResult> resolveMetadata( RepositorySystemSession session,
        //                                      Collection<? extends MetadataRequest> requests );
    }

    @Test
    void install() throws Exception {
        // InstallResult install( RepositorySystemSession session, InstallRequest request )
        //                throws InstallationException;
        // release, snapshot unique ou non unique, attachment
    }

    @Test
    void deploy() throws Exception {
        // DeployResult deploy( RepositorySystemSession session, DeployRequest request )
        //                throws DeploymentException;
    }

    @Test
    void newLocalRepositoryManager() throws Exception {
        // LocalRepositoryManager newLocalRepositoryManager( LocalRepository localRepository );
    }

    @Test
    void newSyncContext() throws Exception {
        // SyncContext newSyncContext( RepositorySystemSession session, boolean shared );
    }
}
