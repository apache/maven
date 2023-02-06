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
package org.apache.maven.repository.legacy.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.CyclicDependencyException;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the default artifact collector.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultArtifactCollectorTest extends PlexusTestCase {
    private LegacyArtifactCollector artifactCollector;

    private ArtifactFactory artifactFactory;

    private ArtifactSpec projectArtifact;

    private Source source;

    private static final String GROUP_ID = "test";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        source = new Source();
        artifactFactory = (ArtifactFactory) lookup(ArtifactFactory.ROLE);
        artifactCollector = lookup(LegacyArtifactCollector.class);

        projectArtifact = createArtifactSpec("project", "1.0", null);
    }

    @Override
    protected void tearDown() throws Exception {
        artifactCollector = null;
        artifactFactory = null;
        super.tearDown();
    }

    // works, but we don't fail on cycles presently
    public void disabledtestCircularDependencyNotIncludingCurrentProject()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        b.addDependency("a", "1.0");
        try {
            collect(a);
            fail("Should have failed on cyclic dependency not involving project");
        } catch (CyclicDependencyException expected) {
            assertTrue(true);
        }
    }

    // works, but we don't fail on cycles presently
    public void disabledtestCircularDependencyIncludingCurrentProject()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        b.addDependency("project", "1.0");
        try {
            collect(a);
            fail("Should have failed on cyclic dependency involving project");
        } catch (CyclicDependencyException expected) {
            assertTrue(true);
        }
    }

    public void testResolveWithFilter() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        ArtifactSpec c = a.addDependency("c", "3.0");

        b.addDependency("c", "2.0");
        ArtifactSpec d = b.addDependency("d", "4.0");

        ArtifactResolutionResult res = collect(a);
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, c.artifact, d.artifact}),
                res.getArtifacts());

        ArtifactFilter filter = new ExclusionSetFilter(new String[] {"b"});
        res = collect(a, filter);
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, c.artifact}), res.getArtifacts());
    }

    public void testResolveCorrectDependenciesWhenDifferentDependenciesOnNearest()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        ArtifactSpec c2 = b.addDependency("c", "2.0");
        c2.addDependency("d", "1.0");

        ArtifactSpec e = createArtifactSpec("e", "1.0");
        ArtifactSpec c1 = e.addDependency("c", "1.0");
        ArtifactSpec f = c1.addDependency("f", "1.0");

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, e.artifact}));
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, e.artifact, c1.artifact, f.artifact}),
                res.getArtifacts());
        assertEquals(
                "Check version", "1.0", getArtifact("c", res.getArtifacts()).getVersion());
    }

    public void disabledtestResolveCorrectDependenciesWhenDifferentDependenciesOnNewest()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        // TODO use newest conflict resolver
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        ArtifactSpec c2 = b.addDependency("c", "2.0");
        ArtifactSpec d = c2.addDependency("d", "1.0");

        ArtifactSpec e = createArtifactSpec("e", "1.0");
        ArtifactSpec c1 = e.addDependency("c", "1.0");
        c1.addDependency("f", "1.0");

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, e.artifact}));
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, e.artifact, c2.artifact, d.artifact}),
                res.getArtifacts());
        assertEquals(
                "Check version", "2.0", getArtifact("c", res.getArtifacts()).getVersion());
    }

    public void disabledtestResolveCorrectDependenciesWhenDifferentDependenciesOnNewestVersionReplaced()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        // TODO use newest conflict resolver
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b1 = a.addDependency("b", "1.0");
        ArtifactSpec c = a.addDependency("c", "1.0");
        ArtifactSpec d2 = b1.addDependency("d", "2.0");
        d2.addDependency("h", "1.0");
        ArtifactSpec d1 = c.addDependency("d", "1.0");
        ArtifactSpec b2 = c.addDependency("b", "2.0");
        ArtifactSpec e = b2.addDependency("e", "1.0");
        ArtifactSpec g = d1.addDependency("g", "1.0");

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact}));
        Object[] artifacts = new Object[] {a.artifact, c.artifact, d1.artifact, b2.artifact, e.artifact, g.artifact};
        assertEquals("Check artifact list", createSet(artifacts), res.getArtifacts());
        assertEquals(
                "Check version", "1.0", getArtifact("d", res.getArtifacts()).getVersion());
        assertEquals(
                "Check version", "2.0", getArtifact("b", res.getArtifacts()).getVersion());
    }

    public void testResolveNearestNewestIsNearest()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        ArtifactSpec c = a.addDependency("c", "3.0");

        b.addDependency("c", "2.0");

        ArtifactResolutionResult res = collect(a);
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, c.artifact}),
                res.getArtifacts());
        assertEquals(
                "Check version", "3.0", getArtifact("c", res.getArtifacts()).getVersion());
    }

    public void testResolveNearestOldestIsNearest()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        ArtifactSpec c = a.addDependency("c", "2.0");

        b.addDependency("c", "3.0");

        ArtifactResolutionResult res = collect(a);
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, c.artifact}),
                res.getArtifacts());
        assertEquals(
                "Check version", "2.0", getArtifact("c", res.getArtifacts()).getVersion());
    }

    public void testResolveLocalNewestIsLocal()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        a.addDependency("b", "2.0");
        ArtifactSpec b = createArtifactSpec("b", "3.0");

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
        assertEquals(
                "Check version", "3.0", getArtifact("b", res.getArtifacts()).getVersion());
    }

    public void testResolveLocalOldestIsLocal()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        a.addDependency("b", "3.0");
        ArtifactSpec b = createArtifactSpec("b", "2.0");

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
        assertEquals(
                "Check version", "2.0", getArtifact("b", res.getArtifacts()).getVersion());
    }

    public void testResolveLocalWithNewerVersionButLesserScope()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("commons-logging", "1.0");
        a.addDependency("junit", "3.7");
        ArtifactSpec b = createArtifactSpec("junit", "3.8.1", Artifact.SCOPE_TEST);

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
        assertEquals(
                "Check version",
                "3.8.1",
                getArtifact("junit", res.getArtifacts()).getVersion());
        assertEquals(
                "Check artifactScope",
                Artifact.SCOPE_TEST,
                getArtifact("junit", res.getArtifacts()).getScope());
    }

    public void testResolveLocalWithNewerVersionButLesserScopeResolvedFirst()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec b = createArtifactSpec("junit", "3.8.1", Artifact.SCOPE_TEST);
        ArtifactSpec a = createArtifactSpec("commons-logging", "1.0");
        a.addDependency("junit", "3.7");

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
        assertEquals(
                "Check version",
                "3.8.1",
                getArtifact("junit", res.getArtifacts()).getVersion());
        assertEquals(
                "Check artifactScope",
                Artifact.SCOPE_TEST,
                getArtifact("junit", res.getArtifacts()).getScope());
    }

    public void testResolveNearestWithRanges()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        ArtifactSpec c = a.addDependency("c", "2.0");

        b.addDependency("c", "[1.0,3.0]");

        ArtifactResolutionResult res = collect(a);
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, c.artifact}),
                res.getArtifacts());
        assertEquals(
                "Check version", "2.0", getArtifact("c", res.getArtifacts()).getVersion());
    }

    public void testResolveRangeWithManagedVersion()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "[1.0,3.0]");

        ArtifactSpec managedB = createArtifactSpec("b", "5.0");

        ArtifactResolutionResult res = collect(a, managedB.artifact);
        assertEquals(
                "Check artifact list", createSet(new Object[] {a.artifact, managedB.artifact}), res.getArtifacts());
        assertEquals(
                "Check version", "5.0", getArtifact("b", res.getArtifacts()).getVersion());
    }

    public void testCompatibleRanges() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        a.addDependency("c", "[2.0,2.5]");
        b.addDependency("c", "[1.0,3.0]");
        ArtifactSpec c = createArtifactSpec("c", "2.5");

        ArtifactResolutionResult res = collect(a);

        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, c.artifact}),
                res.getArtifacts());
        assertEquals(
                "Check version", "2.5", getArtifact("c", res.getArtifacts()).getVersion());
    }

    public void testIncompatibleRanges() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        a.addDependency("c", "[2.4,3.0]");

        b.addDependency("c", "[1.0,2.0]");

        ArtifactResolutionResult res = collect(a);

        assertTrue(res.hasVersionRangeViolations());
    }

    public void testUnboundedRangeWhenVersionUnavailable()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = a.addDependency("b", "1.0");
        a.addDependency("c", "[2.0,]");
        b.addDependency("c", "[1.0,]");

        ArtifactResolutionResult res = collect(a);

        assertTrue(res.hasVersionRangeViolations());
    }

    public void testUnboundedRangeBelowLastRelease()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        createArtifactSpec("c", "1.5");
        ArtifactSpec c = createArtifactSpec("c", "2.0");
        createArtifactSpec("c", "1.1");
        a.addDependency("c", "[1.0,)");

        ArtifactResolutionResult res = collect(a);

        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, c.artifact}), res.getArtifacts());
        assertEquals(
                "Check version", "2.0", getArtifact("c", res.getArtifacts()).getVersion());
    }

    public void testUnboundedRangeAboveLastRelease()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        createArtifactSpec("c", "2.0");
        a.addDependency("c", "[10.0,)");

        ArtifactResolutionResult res = collect(a);

        assertTrue(res.hasVersionRangeViolations());
    }

    public void testResolveManagedVersion() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        a.addDependency("b", "3.0", Artifact.SCOPE_RUNTIME);

        Artifact managedVersion = createArtifactSpec("b", "5.0").artifact;
        Artifact modifiedB = createArtifactSpec("b", "5.0", Artifact.SCOPE_RUNTIME).artifact;

        ArtifactResolutionResult res = collect(a, managedVersion);
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, modifiedB}), res.getArtifacts());
    }

    public void testCollectChangesVersionOfOriginatingArtifactIfInDependencyManagementHasDifferentVersion()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");

        Artifact artifact = projectArtifact.artifact;
        Artifact managedVersion = createArtifactSpec(artifact.getArtifactId(), "2.0").artifact;

        ArtifactResolutionResult result = collect(a, managedVersion);

        assertEquals("collect has modified version in originating artifact", "1.0", artifact.getVersion());

        Artifact resolvedArtifact = result.getArtifacts().iterator().next();

        assertEquals("Resolved version don't match original artifact version", "1.0", resolvedArtifact.getVersion());
    }

    public void testResolveCompileScopeOverTestScope()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec c = createArtifactSpec("c", "3.0", Artifact.SCOPE_TEST);

        a.addDependency("c", "2.0", Artifact.SCOPE_COMPILE);

        Artifact modifiedC = createArtifactSpec("c", "3.0", Artifact.SCOPE_COMPILE).artifact;

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, c.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, modifiedC}), res.getArtifacts());
        Artifact artifact = getArtifact("c", res.getArtifacts());
        // local wins now, and irrelevant if not local as test/provided aren't transitive
        // assertEquals( "Check artifactScope", Artifact.SCOPE_COMPILE, artifact.getArtifactScope() );
        assertEquals("Check artifactScope", Artifact.SCOPE_TEST, artifact.getScope());
    }

    public void testResolveRuntimeScopeOverTestScope()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec c = createArtifactSpec("c", "3.0", Artifact.SCOPE_TEST);

        a.addDependency("c", "2.0", Artifact.SCOPE_RUNTIME);

        Artifact modifiedC = createArtifactSpec("c", "3.0", Artifact.SCOPE_RUNTIME).artifact;

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, c.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, modifiedC}), res.getArtifacts());
        Artifact artifact = getArtifact("c", res.getArtifacts());
        // local wins now, and irrelevant if not local as test/provided aren't transitive
        // assertEquals( "Check artifactScope", Artifact.SCOPE_RUNTIME, artifact.getArtifactScope() );
        assertEquals("Check artifactScope", Artifact.SCOPE_TEST, artifact.getScope());
    }

    public void testResolveCompileScopeOverRuntimeScope()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec root = createArtifactSpec("root", "1.0");
        ArtifactSpec a = root.addDependency("a", "1.0");
        root.addDependency("c", "3.0", Artifact.SCOPE_RUNTIME);

        a.addDependency("c", "2.0", Artifact.SCOPE_COMPILE);

        Artifact modifiedC = createArtifactSpec("c", "3.0", Artifact.SCOPE_COMPILE).artifact;

        ArtifactResolutionResult res = collect(createSet(new Object[] {root.artifact}));
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, root.artifact, modifiedC}),
                res.getArtifacts());
        Artifact artifact = getArtifact("c", res.getArtifacts());
        assertEquals("Check artifactScope", Artifact.SCOPE_COMPILE, artifact.getScope());
    }

    public void testResolveCompileScopeOverProvidedScope()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec c = createArtifactSpec("c", "3.0", Artifact.SCOPE_PROVIDED);

        a.addDependency("c", "2.0", Artifact.SCOPE_COMPILE);

        Artifact modifiedC = createArtifactSpec("c", "3.0", Artifact.SCOPE_COMPILE).artifact;

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, c.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, modifiedC}), res.getArtifacts());
        Artifact artifact = getArtifact("c", res.getArtifacts());
        // local wins now, and irrelevant if not local as test/provided aren't transitive
        // assertEquals( "Check artifactScope", Artifact.SCOPE_COMPILE, artifact.getArtifactScope() );
        assertEquals("Check artifactScope", Artifact.SCOPE_PROVIDED, artifact.getScope());
    }

    public void testResolveRuntimeScopeOverProvidedScope()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec c = createArtifactSpec("c", "3.0", Artifact.SCOPE_PROVIDED);

        a.addDependency("c", "2.0", Artifact.SCOPE_RUNTIME);

        Artifact modifiedC = createArtifactSpec("c", "3.0", Artifact.SCOPE_RUNTIME).artifact;

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, c.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, modifiedC}), res.getArtifacts());
        Artifact artifact = getArtifact("c", res.getArtifacts());
        // local wins now, and irrelevant if not local as test/provided aren't transitive
        // assertEquals( "Check artifactScope", Artifact.SCOPE_RUNTIME, artifact.getArtifactScope() );
        assertEquals("Check artifactScope", Artifact.SCOPE_PROVIDED, artifact.getScope());
    }

    public void testProvidedScopeNotTransitive()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0", Artifact.SCOPE_PROVIDED);
        ArtifactSpec b = createArtifactSpec("b", "1.0");
        b.addDependency("c", "3.0", Artifact.SCOPE_PROVIDED);

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
    }

    public void testOptionalNotTransitive() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = createArtifactSpec("b", "1.0");
        b.addDependency("c", "3.0", true);

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
    }

    public void testOptionalIncludedAtRoot() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");

        ArtifactSpec b = createArtifactSpec("b", "1.0", true);

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
    }

    public void testScopeUpdate() throws InvalidVersionSpecificationException, ArtifactResolutionException {
        /* farthest = compile */
        checkScopeUpdate(Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST, Artifact.SCOPE_COMPILE);

        /* farthest = provided */
        checkScopeUpdate(Artifact.SCOPE_PROVIDED, Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_PROVIDED, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_PROVIDED);
        checkScopeUpdate(Artifact.SCOPE_PROVIDED, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_RUNTIME);
        checkScopeUpdate(Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_SYSTEM);
        checkScopeUpdate(Artifact.SCOPE_PROVIDED, Artifact.SCOPE_TEST, Artifact.SCOPE_TEST);

        /* farthest = runtime */
        checkScopeUpdate(Artifact.SCOPE_RUNTIME, Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_RUNTIME, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_RUNTIME);
        checkScopeUpdate(Artifact.SCOPE_RUNTIME, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_RUNTIME);
        checkScopeUpdate(Artifact.SCOPE_RUNTIME, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_SYSTEM);
        checkScopeUpdate(Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST, Artifact.SCOPE_RUNTIME);

        /* farthest = system */
        checkScopeUpdate(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_PROVIDED);
        checkScopeUpdate(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_RUNTIME);
        checkScopeUpdate(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_SYSTEM);
        checkScopeUpdate(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_TEST, Artifact.SCOPE_TEST);

        /* farthest = test */
        checkScopeUpdate(Artifact.SCOPE_TEST, Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE);
        checkScopeUpdate(Artifact.SCOPE_TEST, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_PROVIDED);
        checkScopeUpdate(Artifact.SCOPE_TEST, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_RUNTIME);
        checkScopeUpdate(Artifact.SCOPE_TEST, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_SYSTEM);
        checkScopeUpdate(Artifact.SCOPE_TEST, Artifact.SCOPE_TEST, Artifact.SCOPE_TEST);
    }

    private void checkScopeUpdate(String farthestScope, String nearestScope, String expectedScope)
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        checkScopeUpdateDirect(farthestScope, nearestScope, expectedScope);
        checkScopeUpdateTransitively(farthestScope, nearestScope, expectedScope);
    }

    private void checkScopeUpdateTransitively(String farthestScope, String nearestScope, String expectedScope)
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = createArtifactSpec("b", "1.0", nearestScope);
        ArtifactSpec c = createArtifactSpec("c", "1.0");
        a.addDependency(c);
        ArtifactSpec dNearest = createArtifactSpec("d", "2.0");
        b.addDependency(dNearest);
        ArtifactSpec dFarthest = createArtifactSpec("d", "3.0", farthestScope);
        c.addDependency(dFarthest);

        /* system and provided dependencies are not transitive */
        if (!Artifact.SCOPE_SYSTEM.equals(nearestScope) && !Artifact.SCOPE_PROVIDED.equals(nearestScope)) {
            checkScopeUpdate(a, b, expectedScope, "2.0");
        }
    }

    private void checkScopeUpdateDirect(String farthestScope, String nearestScope, String expectedScope)
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = createArtifactSpec("b", "1.0");
        ArtifactSpec c = createArtifactSpec("c", "1.0");
        a.addDependency(c);
        ArtifactSpec dNearest = createArtifactSpec("d", "2.0", nearestScope);
        b.addDependency(dNearest);
        ArtifactSpec dFarthest = createArtifactSpec("d", "3.0", farthestScope);
        c.addDependency(dFarthest);

        checkScopeUpdate(a, b, expectedScope, "2.0");
    }

    private void checkScopeUpdate(ArtifactSpec a, ArtifactSpec b, String expectedScope, String expectedVersion)
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ScopeArtifactFilter filter;
        if (Artifact.SCOPE_PROVIDED.equals(expectedScope)) {
            filter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);
        } else if (Artifact.SCOPE_SYSTEM.equals(expectedScope)) {
            filter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);
        } else {
            filter = new ScopeArtifactFilter(expectedScope);
        }

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}), filter);
        Artifact artifact = getArtifact("d", res.getArtifacts());
        assertNotNull("MNG-1895 Dependency was not added to resolution", artifact);
        assertEquals("Check artifactScope", expectedScope, artifact.getScope());
        assertEquals("Check version", expectedVersion, artifact.getVersion());

        ArtifactSpec d = createArtifactSpec("d", "1.0");
        res = collect(createSet(new Object[] {a.artifact, b.artifact, d.artifact}), filter);
        artifact = getArtifact("d", res.getArtifacts());
        assertNotNull("MNG-1895 Dependency was not added to resolution", artifact);
        assertEquals("Check artifactScope", d.artifact.getScope(), artifact.getScope());
        assertEquals("Check version", "1.0", artifact.getVersion());
    }

    public void disabledtestOptionalNotTransitiveButVersionIsInfluential()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        ArtifactSpec b = createArtifactSpec("b", "1.0");
        b.addDependency("c", "3.0", true);
        ArtifactSpec d = a.addDependency("d", "1.0");
        ArtifactSpec e = d.addDependency("e", "1.0");
        e.addDependency("c", "2.0");

        ArtifactSpec c = createArtifactSpec("c", "3.0");

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals(
                "Check artifact list",
                createSet(new Object[] {a.artifact, b.artifact, c.artifact, d.artifact, e.artifact}),
                res.getArtifacts());
        Artifact artifact = getArtifact("c", res.getArtifacts());
        assertEquals("Check version", "3.0", artifact.getVersion());
    }

    public void testTestScopeNotTransitive() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0", Artifact.SCOPE_TEST);
        ArtifactSpec b = createArtifactSpec("b", "1.0");
        b.addDependency("c", "3.0", Artifact.SCOPE_TEST);

        ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact, b.artifact}));
        assertEquals("Check artifact list", createSet(new Object[] {a.artifact, b.artifact}), res.getArtifacts());
    }

    public void testSnapshotNotIncluded() throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        a.addDependency("b", "[1.0,)");
        createArtifactSpec("b", "1.0-SNAPSHOT");

        ArtifactResolutionResult res = collect(a);

        assertTrue(res.hasVersionRangeViolations());

        /*
         * try { ArtifactResolutionResult res = collect( a ); fail( "Expected b not to resolve: " + res ); } catch (
         * OverConstrainedVersionException e ) { assertTrue( e.getMessage().indexOf( "[1.0-SNAPSHOT]" ) <
         * e.getMessage().indexOf( "[1.0,)" ) ); }
         */
    }

    public void testOverConstrainedVersionException()
            throws ArtifactResolutionException, InvalidVersionSpecificationException {
        ArtifactSpec a = createArtifactSpec("a", "1.0");
        a.addDependency("b", "[1.0, 2.0)");
        a.addDependency("c", "[3.3.0,4.0.0)");

        ArtifactSpec b = createArtifactSpec("b", "1.0.0");
        b.addDependency("c", "3.3.0-v3346");

        ArtifactSpec c = createArtifactSpec("c", "3.2.1-v3235e");

        try {
            ArtifactResolutionResult res = collect(createSet(new Object[] {a.artifact}));
        } catch (OverConstrainedVersionException e) {
            assertTrue("Versions unordered", e.getMessage().contains("[3.2.1-v3235e, 3.3.0-v3346]"));
            assertTrue("DependencyTrail unresolved", e.getMessage().contains("Path to dependency:"));
        }
    }

    private Artifact getArtifact(String id, Set artifacts) {
        for (Object artifact : artifacts) {
            Artifact a = (Artifact) artifact;
            if (a.getArtifactId().equals(id) && a.getGroupId().equals(GROUP_ID)) {
                return a;
            }
        }
        return null;
    }

    private ArtifactResolutionResult collect(Set<Artifact> artifacts) throws ArtifactResolutionException {
        return collect(artifacts, null);
    }

    private ArtifactResolutionResult collect(Set<Artifact> artifacts, ArtifactFilter filter)
            throws ArtifactResolutionException {
        return artifactCollector.collect(
                artifacts,
                projectArtifact.artifact,
                null,
                null,
                null,
                source,
                filter,
                Collections.<ResolutionListener>emptyList(),
                null);
    }

    private ArtifactResolutionResult collect(ArtifactSpec a) throws ArtifactResolutionException {
        return artifactCollector.collect(
                Collections.singleton(a.artifact),
                projectArtifact.artifact,
                null,
                null,
                null,
                source,
                null,
                Collections.<ResolutionListener>emptyList(),
                null);
    }

    private ArtifactResolutionResult collect(ArtifactSpec a, ArtifactFilter filter) throws ArtifactResolutionException {
        return artifactCollector.collect(
                Collections.singleton(a.artifact),
                projectArtifact.artifact,
                null,
                null,
                null,
                source,
                filter,
                Collections.<ResolutionListener>emptyList(),
                null);
    }

    private ArtifactResolutionResult collect(ArtifactSpec a, Artifact managedVersion)
            throws ArtifactResolutionException {
        Map managedVersions = Collections.singletonMap(managedVersion.getDependencyConflictId(), managedVersion);
        return artifactCollector.collect(
                Collections.singleton(a.artifact),
                projectArtifact.artifact,
                managedVersions,
                null,
                null,
                source,
                null,
                Collections.<ResolutionListener>emptyList(),
                null);
    }

    private ArtifactSpec createArtifactSpec(String id, String version) throws InvalidVersionSpecificationException {
        return createArtifactSpec(id, version, Artifact.SCOPE_COMPILE);
    }

    private ArtifactSpec createArtifactSpec(String id, String version, boolean optional)
            throws InvalidVersionSpecificationException {
        return createArtifactSpec(id, version, Artifact.SCOPE_COMPILE, null, optional);
    }

    private ArtifactSpec createArtifactSpec(String id, String version, String scope)
            throws InvalidVersionSpecificationException {
        return createArtifactSpec(id, version, scope, null, false);
    }

    private ArtifactSpec createArtifactSpec(
            String id, String version, String scope, String inheritedScope, boolean optional)
            throws InvalidVersionSpecificationException {
        VersionRange versionRange = VersionRange.createFromVersionSpec(version);
        Artifact artifact = artifactFactory.createDependencyArtifact(
                GROUP_ID, id, versionRange, "jar", null, scope, inheritedScope, optional);
        ArtifactSpec spec = null;
        if (artifact != null) {
            spec = new ArtifactSpec();
            spec.artifact = artifact;
            source.addArtifact(spec);
        }
        return spec;
    }

    @SuppressWarnings("unchecked")
    private static Set<Artifact> createSet(Object[] x) {
        return new LinkedHashSet(Arrays.asList(x));
    }

    private class ArtifactSpec {
        private Artifact artifact;

        private Set<Artifact> dependencies = new HashSet<>();

        public ArtifactSpec addDependency(String id, String version) throws InvalidVersionSpecificationException {
            return addDependency(id, version, Artifact.SCOPE_COMPILE);
        }

        public ArtifactSpec addDependency(String id, String version, String scope)
                throws InvalidVersionSpecificationException {
            return addDependency(id, version, scope, false);
        }

        private ArtifactSpec addDependency(ArtifactSpec dep) throws InvalidVersionSpecificationException {
            if (dep != null) {
                dependencies.add(dep.artifact);
            }
            return dep;
        }

        private ArtifactSpec addDependency(String id, String version, String scope, boolean optional)
                throws InvalidVersionSpecificationException {
            ArtifactSpec dep = createArtifactSpec(id, version, scope, artifact.getScope(), optional);
            return addDependency(dep);
        }

        public ArtifactSpec addDependency(String id, String version, boolean optional)
                throws InvalidVersionSpecificationException {
            return addDependency(id, version, Artifact.SCOPE_COMPILE, optional);
        }
    }

    private class Source implements ArtifactMetadataSource {
        private Map<String, ArtifactSpec> artifacts = new HashMap<>();

        private Map<String, List<ArtifactVersion>> versions = new HashMap<>();

        public ResolutionGroup retrieve(
                Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
                throws ArtifactMetadataRetrievalException {
            String key = getKey(artifact);

            ArtifactSpec a = (ArtifactSpec) artifacts.get(key);
            try {
                return new ResolutionGroup(
                        artifact,
                        createArtifacts(
                                artifactFactory, a.dependencies, artifact.getScope(), artifact.getDependencyFilter()),
                        Collections.EMPTY_LIST);
            } catch (InvalidVersionSpecificationException e) {
                throw new ArtifactMetadataRetrievalException("Invalid version creating artifacts", e, artifact);
            }
        }

        private String getKey(Artifact artifact) {
            return artifact.getDependencyConflictId();
        }

        private Set<Artifact> createArtifacts(
                ArtifactFactory artifactFactory,
                Set<Artifact> dependencies,
                String inheritedScope,
                ArtifactFilter dependencyFilter)
                throws InvalidVersionSpecificationException {
            Set<Artifact> projectArtifacts = new HashSet<>();

            for (Artifact d : dependencies) {
                VersionRange versionRange;
                if (d.getVersionRange() != null) {
                    versionRange = d.getVersionRange();
                } else {
                    versionRange = VersionRange.createFromVersionSpec(d.getVersion());
                }
                Artifact artifact;
                if (d.getScope().equals(Artifact.SCOPE_TEST) || d.getScope().equals(Artifact.SCOPE_PROVIDED)) {
                    /* don't call createDependencyArtifact as it'll ignore test and provided scopes */
                    artifact = artifactFactory.createArtifact(
                            d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope(), d.getType());
                } else {
                    artifact = artifactFactory.createDependencyArtifact(
                            d.getGroupId(),
                            d.getArtifactId(),
                            versionRange,
                            d.getType(),
                            d.getClassifier(),
                            d.getScope(),
                            inheritedScope,
                            d.isOptional());
                }

                if (artifact != null && (dependencyFilter == null || dependencyFilter.include(artifact))) {
                    artifact.setDependencyFilter(dependencyFilter);

                    projectArtifacts.add(artifact);
                }
            }

            return projectArtifacts;
        }

        public void addArtifact(ArtifactSpec spec) {
            artifacts.put(getKey(spec.artifact), spec);

            String key = spec.artifact.getDependencyConflictId();
            List<ArtifactVersion> artifactVersions = versions.get(key);
            if (artifactVersions == null) {
                artifactVersions = new ArrayList<>();
                versions.put(key, artifactVersions);
            }
            if (spec.artifact.getVersion() != null) {
                artifactVersions.add(new DefaultArtifactVersion(spec.artifact.getVersion()));
            }
        }

        public List<ArtifactVersion> retrieveAvailableVersions(
                Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
                throws ArtifactMetadataRetrievalException {
            return retrieveAvailableVersions(artifact);
        }

        public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
                Artifact artifact, ArtifactRepository localRepository, ArtifactRepository remoteRepository)
                throws ArtifactMetadataRetrievalException {
            return retrieveAvailableVersions(artifact);
        }

        private List<ArtifactVersion> retrieveAvailableVersions(Artifact artifact) {
            List<ArtifactVersion> artifactVersions = versions.get(artifact.getDependencyConflictId());
            if (artifactVersions == null) {
                artifactVersions = Collections.emptyList();
            }
            return artifactVersions;
        }

        public ResolutionGroup retrieve(MetadataResolutionRequest request) throws ArtifactMetadataRetrievalException {
            return retrieve(request.getArtifact(), request.getLocalRepository(), request.getRemoteRepositories());
        }

        public List<ArtifactVersion> retrieveAvailableVersions(MetadataResolutionRequest request)
                throws ArtifactMetadataRetrievalException {
            return retrieveAvailableVersions(
                    request.getArtifact(), request.getLocalRepository(), request.getRemoteRepositories());
        }
    }
}
