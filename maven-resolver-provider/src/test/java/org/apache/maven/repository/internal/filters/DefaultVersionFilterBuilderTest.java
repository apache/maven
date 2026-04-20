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
package org.apache.maven.repository.internal.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.VersionFilterBuilder;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.internal.impl.collect.DefaultVersionFilterContext;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.graph.version.ContextPredicateDelegatingVersionFilter;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultVersionFilterBuilderTest {
    final GenericVersionScheme versionScheme = new GenericVersionScheme();
    RepositorySystemSession session;
    DefaultVersionFilterBuilder factory;
    Map<String, Object> sessionConfigProperties;

    @BeforeEach
    public void prepare() {
        session = mock(RepositorySystemSession.class);
        sessionConfigProperties = new HashMap<>();
        when(session.getConfigProperties()).thenReturn(sessionConfigProperties);
        factory = new DefaultVersionFilterBuilder();
    }

    private Version version(String spec) {
        try {
            return versionScheme.parseVersion(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e); // never happens
        }
    }

    private VersionConstraint versionConstraint(String spec) {
        try {
            return versionScheme.parseVersionConstraint(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void versionFilterUnsupportedExpression() {
        // null and empty string are OK
        assertThrows(IllegalArgumentException.class, () -> factory.buildVersionFilter("[*", this::versionConstraint));
        assertThrows(
                IllegalArgumentException.class, () -> factory.buildVersionFilter("foobar", this::versionConstraint));
    }

    /**
     * Simple assertions for {@code h} filter,
     */
    @Test
    public void versionFilterHighest() {
        VersionFilter vf;
        vf = factory.buildVersionFilter("h", this::versionConstraint).orElse(null);
        assertNotNull(vf);
        assertInstanceOf(ContextPredicateDelegatingVersionFilter.class, vf);

        vf = factory.buildVersionFilter("h(5)", this::versionConstraint).orElse(null);
        assertNotNull(vf);
        assertInstanceOf(ContextPredicateDelegatingVersionFilter.class, vf);

        vf = factory.buildVersionFilter("h(1)@group", this::versionConstraint).orElse(null);
        assertNotNull(vf);
        assertInstanceOf(ContextPredicateDelegatingVersionFilter.class, vf); // this is wrapped instance
    }

    /**
     * Creating {@code h(1@group)} and incoming artifact G does not match => not applied.
     */
    @Test
    public void versionFilterSuppressed() throws RepositoryException {
        VersionFilter vf;

        vf = factory.buildVersionFilter("h(2)@group", this::versionConstraint).orElse(null);
        assertNotNull(vf);

        List<Version> versions = Arrays.asList(version("1.0"), version("1.1"), version("1.2"));

        DefaultVersionFilterContext context = new DefaultVersionFilterContext(session);
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(new ArrayList<>(versions));
        context.set(new Dependency(new DefaultArtifact("group:a:[1,)"), ""), result);

        vf.filterVersions(context);

        // hit
        assertEquals(2, context.get().size());
        assertEquals(version("1.1"), context.get().get(0));
        assertEquals(version("1.2"), context.get().get(1));

        sessionConfigProperties.put(VersionFilterBuilder.MAVEN_VERSION_FILTER_SUPPRESSED, Boolean.TRUE);

        context = new DefaultVersionFilterContext(session);
        context.set(new Dependency(new DefaultArtifact("group:a:[1,)"), ""), result);

        // suppressed
        assertEquals(versions, context.get());
    }

    /**
     * Creating {@code h(1@group)} and incoming artifact G does not match => not applied.
     */
    @Test
    public void versionFilterHLFuncMiss() throws RepositoryException {
        VersionFilter vf;

        vf = factory.buildVersionFilter("h(2)@group", this::versionConstraint).orElse(null);
        assertNotNull(vf);

        List<Version> versions = Arrays.asList(version("1.0"), version("1.1"), version("1.2"));

        DefaultVersionFilterContext context = new DefaultVersionFilterContext(session);
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(new ArrayList<>(versions));
        context.set(new Dependency(new DefaultArtifact("g:a:[1,)"), ""), result);

        vf.filterVersions(context);

        vf.filterVersions(context);
    }

    /**
     * Creating {@code h(1@group)} and incoming artifact G does match => applied.
     */
    @Test
    public void versionFilterHLFuncHit() throws RepositoryException {
        VersionFilter vf;

        vf = factory.buildVersionFilter("h(2)@group", this::versionConstraint).orElse(null);
        assertNotNull(vf);

        List<Version> versions = Arrays.asList(version("1.0"), version("1.1"), version("1.2"));

        DefaultVersionFilterContext context = new DefaultVersionFilterContext(session);
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(new ArrayList<>(versions));
        context.set(new Dependency(new DefaultArtifact("group:a:[1,)"), ""), result);

        vf.filterVersions(context);

        assertEquals(2, context.get().size());
        assertEquals(version("1.1"), context.get().get(0));
        assertEquals(version("1.2"), context.get().get(1));
    }

    /**
     * Creating {@code l(1@group)} and incoming artifact G does not match => not applied.
     */
    @Test
    public void versionFilterLowestFuncMiss() throws RepositoryException {
        VersionFilter vf;

        vf = factory.buildVersionFilter("l(2)@group", this::versionConstraint).orElse(null);
        assertNotNull(vf);

        List<Version> versions = Arrays.asList(version("1.0"), version("1.1"), version("1.2"));

        DefaultVersionFilterContext context = new DefaultVersionFilterContext(session);
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(new ArrayList<>(versions));
        context.set(new Dependency(new DefaultArtifact("g:a:[1,)"), ""), result);

        vf.filterVersions(context);

        assertEquals(versions, context.get());
    }

    /**
     * Creating {@code l(1@group)} and incoming artifact G does match => applied.
     */
    @Test
    public void versionFilterLowestFuncHit() throws RepositoryException {
        VersionFilter vf;

        vf = factory.buildVersionFilter("l(2)@group", this::versionConstraint).orElse(null);
        assertNotNull(vf);

        List<Version> versions = Arrays.asList(version("1.0"), version("1.1"), version("1.2"));

        DefaultVersionFilterContext context = new DefaultVersionFilterContext(session);
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(new ArrayList<>(versions));
        context.set(new Dependency(new DefaultArtifact("group:a:[1,)"), ""), result);

        vf.filterVersions(context);

        assertEquals(2, context.get().size());
        assertEquals(version("1.0"), context.get().get(0));
        assertEquals(version("1.1"), context.get().get(1));
    }

    @Test
    public void versionFilterExcludeFuncHit() throws RepositoryException {
        VersionFilter vf;

        vf = factory.buildVersionFilter("e([1.1,2.0))@group:a", this::versionConstraint)
                .orElse(null);
        assertNotNull(vf);

        List<Version> versions = Arrays.asList(version("1.0"), version("1.1"), version("1.2"), version("2.0"));

        DefaultVersionFilterContext context = new DefaultVersionFilterContext(session);
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(new ArrayList<>(versions));
        context.set(new Dependency(new DefaultArtifact("group:a:[1,)"), ""), result);

        vf.filterVersions(context);

        assertEquals(2, context.get().size());
        assertEquals(version("1.0"), context.get().get(0));
        assertEquals(version("2.0"), context.get().get(1));
    }

    @Test
    public void versionFilterIncludeFuncHit() throws RepositoryException {
        VersionFilter vf;

        vf = factory.buildVersionFilter("i([1.1,),[2.0,))@group:a", this::versionConstraint)
                .orElse(null);
        assertNotNull(vf);

        List<Version> versions = Arrays.asList(version("1.0"), version("1.1"), version("1.2"), version("2.0"));

        DefaultVersionFilterContext context = new DefaultVersionFilterContext(session);
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(new ArrayList<>(versions));
        context.set(new Dependency(new DefaultArtifact("group:a:[1,)"), ""), result);

        vf.filterVersions(context);

        assertEquals(3, context.get().size());
        assertEquals(version("1.1"), context.get().get(0));
        assertEquals(version("1.2"), context.get().get(1));
        assertEquals(version("2.0"), context.get().get(2));
    }
}
