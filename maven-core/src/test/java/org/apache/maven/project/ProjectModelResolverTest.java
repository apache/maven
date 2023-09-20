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
package org.apache.maven.project;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Test cases for the project {@code ModelResolver} implementation.
 *
 * @author Christian Schulte
 * @since 3.5.0
 */
public class ProjectModelResolverTest extends AbstractMavenProjectTestCase {

    /**
     * Creates a new {@code ProjectModelResolverTest} instance.
     */
    public ProjectModelResolverTest() {
        super();
    }

    public void testResolveParentThrowsUnresolvableModelExceptionWhenNotFound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("org.apache");
        parent.setArtifactId("apache");
        parent.setVersion("0");

        try {
            this.newModelResolver().resolveModel(parent);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertNotNull(e.getMessage());
            assertThat(e.getMessage(), containsString("Could not find artifact org.apache:apache:pom:0 in central"));
        }
    }

    public void testResolveParentThrowsUnresolvableModelExceptionWhenNoMatchingVersionFound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("org.apache");
        parent.setArtifactId("apache");
        parent.setVersion("[2.0,2.1)");

        try {
            this.newModelResolver().resolveModel(parent);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals("No versions matched the requested parent version range '[2.0,2.1)'", e.getMessage());
        }
    }

    public void testResolveParentThrowsUnresolvableModelExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("org.apache");
        parent.setArtifactId("apache");
        parent.setVersion("[1,)");

        try {
            this.newModelResolver().resolveModel(parent);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals("The requested parent version range '[1,)' does not specify an upper bound", e.getMessage());
        }
    }

    public void testResolveParentSuccessfullyResolvesExistingParentWithoutRange() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("org.apache");
        parent.setArtifactId("apache");
        parent.setVersion("1");

        assertNotNull(this.newModelResolver().resolveModel(parent));
        assertEquals("1", parent.getVersion());
    }

    public void testResolveParentSuccessfullyResolvesExistingParentUsingHighestVersion() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("org.apache");
        parent.setArtifactId("apache");
        parent.setVersion("(,2.0)");

        assertNotNull(this.newModelResolver().resolveModel(parent));
        assertEquals("1", parent.getVersion());
    }

    public void testResolveDependencyThrowsUnresolvableModelExceptionWhenNotFound() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache");
        dependency.setArtifactId("apache");
        dependency.setVersion("0");

        try {
            this.newModelResolver().resolveModel(dependency);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertNotNull(e.getMessage());
            assertThat(e.getMessage(), containsString("Could not find artifact org.apache:apache:pom:0 in central"));
        }
    }

    public void testResolveDependencyThrowsUnresolvableModelExceptionWhenNoMatchingVersionFound() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache");
        dependency.setArtifactId("apache");
        dependency.setVersion("[2.0,2.1)");

        try {
            this.newModelResolver().resolveModel(dependency);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals("No versions matched the requested dependency version range '[2.0,2.1)'", e.getMessage());
        }
    }

    public void testResolveDependencyThrowsUnresolvableModelExceptionWhenUsingRangesWithoutUpperBound()
            throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache");
        dependency.setArtifactId("apache");
        dependency.setVersion("[1,)");

        try {
            this.newModelResolver().resolveModel(dependency);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals(
                    "The requested dependency version range '[1,)' does not specify an upper bound", e.getMessage());
        }
    }

    public void testResolveDependencySuccessfullyResolvesExistingDependencyWithoutRange() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache");
        dependency.setArtifactId("apache");
        dependency.setVersion("1");

        assertNotNull(this.newModelResolver().resolveModel(dependency));
        assertEquals("1", dependency.getVersion());
    }

    public void testResolveDependencySuccessfullyResolvesExistingDependencyUsingHighestVersion() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache");
        dependency.setArtifactId("apache");
        dependency.setVersion("(,2.0)");

        assertNotNull(this.newModelResolver().resolveModel(dependency));
        assertEquals("1", dependency.getVersion());
    }

    private ModelResolver newModelResolver() throws Exception {
        final File localRepo = new File(this.getLocalRepository().getBasedir());
        final DefaultRepositorySystemSession repoSession = MavenRepositorySystemUtils.newSession();
        repoSession.setLocalRepositoryManager(new LegacyLocalRepositoryManager(localRepo));

        return new ProjectModelResolver(
                repoSession,
                null,
                lookup(RepositorySystem.class),
                lookup(RemoteRepositoryManager.class),
                this.getRemoteRepositories(),
                ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT,
                null);
    }

    private List<RemoteRepository> getRemoteRepositories() throws InvalidRepositoryException {
        final File repoDir = new File(getBasedir(), "src/test/remote-repo").getAbsoluteFile();
        final RemoteRepository remoteRepository = new RemoteRepository.Builder(
                        org.apache.maven.repository.RepositorySystem.DEFAULT_REMOTE_REPO_ID,
                        "default",
                        repoDir.toURI().toASCIIString())
                .build();

        return Collections.singletonList(remoteRepository);
    }
}
