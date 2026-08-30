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

import javax.inject.Inject;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test cases for the default {@code ModelResolver} implementation.
 *
 * @author Christian Schulte
 * @since 3.5.0
 */
@PlexusTest
public final class DefaultModelResolverTest extends AbstractRepositoryTest {

    @Test
    public void testResolveParentThrowsUnresolvableModelExceptionWhenNotFound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("0");

        try {
            this.newModelResolver().resolveModel(parent);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Could not find artifact ut.simple:artifact:pom:0 in repo"));
        }
    }

    @Test
    public void testResolveParentThrowsUnresolvableModelExceptionWhenNoMatchingVersionFound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("[2.0,2.1)");

        try {
            this.newModelResolver().resolveModel(parent);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals("No versions matched the requested parent version range '[2.0,2.1)'", e.getMessage());
        }
    }

    @Test
    public void testResolveParentThrowsUnresolvableModelExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("[1.0,)");

        try {
            this.newModelResolver().resolveModel(parent);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals("The requested parent version range '[1.0,)' does not specify an upper bound", e.getMessage());
        }
    }

    @Test
    public void testResolveParentSuccessfullyResolvesExistingParentWithoutRange() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("1.0");

        assertNotNull(this.newModelResolver().resolveModel(parent));
        assertEquals("1.0", parent.getVersion());
    }

    @Test
    public void testResolveParentSuccessfullyResolvesExistingParentUsingHighestVersion() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("(,2.0)");

        assertNotNull(this.newModelResolver().resolveModel(parent));
        assertEquals("1.0", parent.getVersion());
    }

    @Test
    public void testResolveDependencyThrowsUnresolvableModelExceptionWhenNotFound() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("0");

        try {
            this.newModelResolver().resolveModel(dependency);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Could not find artifact ut.simple:artifact:pom:0 in repo"));
        }
    }

    @Test
    public void testResolveDependencyThrowsUnresolvableModelExceptionWhenNoMatchingVersionFound() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("[2.0,2.1)");

        try {
            this.newModelResolver().resolveModel(dependency);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals("No versions matched the requested dependency version range '[2.0,2.1)'", e.getMessage());
        }
    }

    @Test
    public void testResolveDependencyThrowsUnresolvableModelExceptionWhenUsingRangesWithoutUpperBound()
            throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("[1.0,)");

        try {
            this.newModelResolver().resolveModel(dependency);
            fail("Expected 'UnresolvableModelException' not thrown.");
        } catch (final UnresolvableModelException e) {
            assertEquals(
                    "The requested dependency version range '[1.0,)' does not specify an upper bound", e.getMessage());
        }
    }

    @Test
    public void testResolveDependencySuccessfullyResolvesExistingDependencyWithoutRange() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("1.0");

        assertNotNull(this.newModelResolver().resolveModel(dependency));
        assertEquals("1.0", dependency.getVersion());
    }

    @Test
    public void testResolveDependencySuccessfullyResolvesExistingDependencyUsingHighestVersion() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("(,2.0)");

        assertNotNull(this.newModelResolver().resolveModel(dependency));
        assertEquals("1.0", dependency.getVersion());
    }

    @Inject
    private ArtifactResolver artifactResolver;

    @Inject
    private VersionRangeResolver versionRangeResolver;

    @Inject
    private RemoteRepositoryManager remoteRepositoryManager;

    @Test
    public void testConstructionSuppliedRepositoryKeepsPrecedence(@TempDir Path localRepository) throws Exception {
        // An empty local repository, so resolution has to consult the remote repository list
        // rather than a copy cached by another test in this class.
        final DefaultRepositorySystemSession isolatedSession = MavenRepositorySystemUtils.newSession();
        isolatedSession.setLocalRepositoryManager(
                system.newLocalRepositoryManager(isolatedSession, new LocalRepository(localRepository.toFile())));

        final ModelResolver resolver = new DefaultModelResolver(
                isolatedSession,
                null,
                this.getClass().getName(),
                artifactResolver,
                versionRangeResolver,
                remoteRepositoryManager,
                Arrays.asList(newTestRepository()));

        // A model-declared repository that reuses the external repository's id; the external
        // repository must keep its slot.
        final Repository repository = new Repository();
        repository.setId("repo");
        repository.setUrl(new File("target/no-such-repository").toURI().toURL().toString());

        resolver.addRepository(repository);
        resolver.addRepository(repository, true);

        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("1.0");

        // The external repository kept its slot, so the artifact still resolves.
        assertNotNull(resolver.resolveModel(parent));
    }

    private ModelResolver newModelResolver() throws ComponentLookupException, MalformedURLException {
        return new DefaultModelResolver(
                this.session,
                null,
                this.getClass().getName(),
                artifactResolver,
                versionRangeResolver,
                remoteRepositoryManager,
                Arrays.asList(newTestRepository()));
    }
}
