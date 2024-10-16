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
package org.apache.maven.repository.legacy.resolver.conflict;

import javax.inject.Inject;

import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Provides a basis for testing conflict resolvers.
 *
 */
@PlexusTest
@Deprecated
public abstract class AbstractConflictResolverTest {
    // constants --------------------------------------------------------------

    private static final String GROUP_ID = "test";

    // fields -----------------------------------------------------------------

    protected Artifact a1;

    protected Artifact a2;

    protected Artifact b1;

    private final String roleHint;

    @Inject
    private ArtifactFactory artifactFactory;

    private ConflictResolver conflictResolver;

    @Inject
    protected PlexusContainer container;

    // constructors -----------------------------------------------------------

    public AbstractConflictResolverTest(String roleHint) throws Exception {
        this.roleHint = roleHint;
    }

    // TestCase methods -------------------------------------------------------

    /*
     * @see junit.framework.TestCase#setUp()
     */
    @BeforeEach
    public void setUp() throws Exception {
        conflictResolver = (ConflictResolver) container.lookup(ConflictResolver.ROLE, roleHint);

        a1 = createArtifact("a", "1.0");
        a2 = createArtifact("a", "2.0");
        b1 = createArtifact("b", "1.0");
    }

    // protected methods ------------------------------------------------------

    protected ConflictResolver getConflictResolver() {
        return conflictResolver;
    }

    protected void assertResolveConflict(
            ResolutionNode expectedNode, ResolutionNode actualNode1, ResolutionNode actualNode2) {
        ResolutionNode resolvedNode = getConflictResolver().resolveConflict(actualNode1, actualNode2);

        assertNotNull(resolvedNode, "Expected resolvable");
        assertEquals(expectedNode, resolvedNode, "Resolution node");
    }

    protected Artifact createArtifact(String id, String version) throws InvalidVersionSpecificationException {
        return createArtifact(id, version, Artifact.SCOPE_COMPILE);
    }

    protected Artifact createArtifact(String id, String version, String scope)
            throws InvalidVersionSpecificationException {
        return createArtifact(id, version, scope, null, false);
    }

    protected Artifact createArtifact(String id, String version, String scope, String inheritedScope, boolean optional)
            throws InvalidVersionSpecificationException {
        VersionRange versionRange = VersionRange.createFromVersionSpec(version);

        return artifactFactory.createDependencyArtifact(
                GROUP_ID, id, versionRange, "jar", null, scope, inheritedScope, optional);
    }

    protected ResolutionNode createResolutionNode(Artifact Artifact) {
        return new ResolutionNode(Artifact, Collections.<ArtifactRepository>emptyList());
    }

    protected ResolutionNode createResolutionNode(Artifact Artifact, ResolutionNode parent) {
        return new ResolutionNode(Artifact, Collections.<ArtifactRepository>emptyList(), parent);
    }
}
