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

import java.net.MalformedURLException;
import java.util.Arrays;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;

/**
 * Test cases for the default {@code ModelResolver} implementation.
 *
 * @author Christian Schulte
 * @since 3.5.0
 */
public final class DefaultModelResolverTest extends AbstractRepositoryTestCase {

    /**
     * Creates a new {@code DefaultModelResolverTest} instance.
     */
    public DefaultModelResolverTest() {
        super();
    }

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

    public void testResolveParentSuccessfullyResolvesExistingParentWithoutRange() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("1.0");

        assertNotNull(this.newModelResolver().resolveModel(parent));
        assertEquals("1.0", parent.getVersion());
    }

    public void testResolveParentSuccessfullyResolvesExistingParentUsingHighestVersion() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("(,2.0)");

        assertNotNull(this.newModelResolver().resolveModel(parent));
        assertEquals("1.0", parent.getVersion());
    }

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

    public void testResolveDependencySuccessfullyResolvesExistingDependencyWithoutRange() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("1.0");

        assertNotNull(this.newModelResolver().resolveModel(dependency));
        assertEquals("1.0", dependency.getVersion());
    }

    public void testResolveDependencySuccessfullyResolvesExistingDependencyUsingHighestVersion() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("(,2.0)");

        assertNotNull(this.newModelResolver().resolveModel(dependency));
        assertEquals("1.0", dependency.getVersion());
    }

    private ModelResolver newModelResolver() throws ComponentLookupException, MalformedURLException {
        return new DefaultModelResolver(
                this.session,
                null,
                this.getClass().getName(),
                lookup(ArtifactResolver.class),
                lookup(VersionRangeResolver.class),
                lookup(RemoteRepositoryManager.class),
                Arrays.asList(newTestRepository()));
    }
}
