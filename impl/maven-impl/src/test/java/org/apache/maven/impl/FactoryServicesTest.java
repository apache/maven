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
package org.apache.maven.impl;

import java.util.List;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Type;
import org.apache.maven.api.services.ArtifactFactory;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FactoryServicesTest {
    private InternalSession newSession() {
        InternalSession session = mock(InternalSession.class);
        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);
        ArtifactTypeRegistry types = mock(ArtifactTypeRegistry.class);
        when(session.getSession()).thenReturn(repositorySession);
        when(repositorySession.getArtifactTypeRegistry()).thenReturn(types);
        when(types.get("test-jar")).thenReturn(new DefaultArtifactType("test-jar", "jar", "tests", "java"));
        DefaultModelVersionParser versions = new DefaultModelVersionParser(new GenericVersionScheme());
        when(session.parseVersion(anyString())).thenAnswer(call -> versions.parseVersion(call.getArgument(0)));
        when(session.parseVersionConstraint(anyString()))
                .thenAnswer(call -> versions.parseVersionConstraint(call.getArgument(0)));
        return session;
    }

    @Test
    void artifactFactoriesPreserveTypeDefaults() {
        InternalSession session = newSession();
        ArtifactFactory factory = new DefaultArtifactFactory();
        var artifact = factory.create(session, "org.example", "library", "1.0", null, null, "test-jar");
        var produced = factory.createProduced(session, "org.example", "library", "1.0", null, null, "test-jar");
        var coordinates = new DefaultArtifactCoordinatesFactory()
                .create(session, "org.example", "library", "1.0", null, null, "test-jar");
        assertEquals("tests", artifact.classifier());
        assertEquals("jar", artifact.extension());
        assertEquals("1.0", artifact.version().toString());
        assertEquals("tests", produced.classifier());
        assertEquals("jar", produced.extension());
        assertEquals("tests", coordinates.classifier());
        assertEquals("jar", coordinates.extension());
    }

    @Test
    void explicitClassifierAndExtensionOverrideTypeDefaults() {
        InternalSession session = newSession();
        var artifact = new DefaultArtifactFactory()
                .create(session, "org.example", "library", "1.0", "sources", "zip", "test-jar");
        var coordinates = new DefaultArtifactCoordinatesFactory()
                .create(session, "org.example", "library", "1.0", "sources", "zip", "test-jar");
        assertEquals("sources", artifact.classifier());
        assertEquals("zip", artifact.extension());
        assertEquals("sources", coordinates.classifier());
        assertEquals("zip", coordinates.extension());
    }

    @Test
    void coordinateStringsAndConvenienceMethodsPreserveVersionConstraints() {
        InternalSession session = newSession();
        DefaultArtifactCoordinatesFactory factory = new DefaultArtifactCoordinatesFactory();
        ArtifactCoordinates parsed = factory.create(session, "org.example:library:jar:tests:[1,2)");
        ArtifactCoordinates components =
                factory.create(session, "org.example", "library", "[1,2)", "tests", "jar", null);
        assertEquals(components, parsed);
        assertEquals("[1,2)", parsed.versionConstraint().toString());
        DependencyCoordinates dependency = new DefaultDependencyCoordinatesFactory().create(session, parsed);
        assertEquals("[1,2)", dependency.versionConstraint().toString());
        assertEquals("tests", dependency.classifier());
        assertEquals("jar", dependency.extension());
    }

    @Test
    void dependencyConversionPreservesExclusionsWithoutRecursion() {
        InternalSession session = newSession();
        Type type = mock(Type.class);
        when(type.id()).thenReturn("jar");
        when(session.requireType("jar")).thenReturn(type);
        when(session.requireDependencyScope("runtime")).thenReturn(DependencyScope.RUNTIME);
        org.eclipse.aether.graph.Dependency resolverDependency = new org.eclipse.aether.graph.Dependency(
                new org.eclipse.aether.artifact.DefaultArtifact("org.example:library:jar:tests:1.0"),
                "runtime",
                true,
                List.of(new org.eclipse.aether.graph.Exclusion("org.excluded", "unwanted", "*", "*")));
        Dependency dependency = new DefaultDependency(session, resolverDependency);
        DefaultDependencyCoordinatesFactory factory = new DefaultDependencyCoordinatesFactory();
        when(session.createDependencyCoordinates(any(Dependency.class)))
                .thenAnswer(call -> factory.create(session, (Dependency) call.getArgument(0)));
        assertEquals("org.excluded", dependency.exclusions().iterator().next().groupId());
        for (DependencyCoordinates coordinates :
                List.of(factory.create(session, dependency), dependency.toCoordinates())) {
            assertEquals("tests", coordinates.classifier());
            assertEquals("1.0", coordinates.versionConstraint().toString());
            assertEquals(DependencyScope.RUNTIME, coordinates.scope());
            assertTrue(coordinates.optional());
            assertEquals(1, coordinates.exclusions().size());
            Exclusion exclusion = coordinates.exclusions().iterator().next();
            assertEquals("org.excluded", exclusion.groupId());
            assertEquals("unwanted", exclusion.artifactId());
        }
    }

    @Test
    void dependencyFactoryPreservesScopeOptionalityAndExclusions() {
        InternalSession session = newSession();
        Exclusion exclusion = mock(Exclusion.class);
        when(exclusion.getGroupId()).thenReturn("org.excluded");
        when(exclusion.getArtifactId()).thenReturn("unwanted");
        DefaultDependencyCoordinatesFactory factory = new DefaultDependencyCoordinatesFactory();
        DependencyCoordinates dependency = factory.create(
                session, "org.example", "library", "1.0", "tests", "jar", "test-jar", "test", true, List.of(exclusion));
        DependencyCoordinates parsed =
                factory.create(session, "org.example:library:jar:tests:1.0", "test", true, List.of(exclusion));
        for (DependencyCoordinates coordinates : List.of(dependency, parsed)) {
            assertTrue(coordinates.optional());
            assertEquals("test", ((DefaultDependencyCoordinates) coordinates).dependency.getScope());
            assertEquals(
                    "org.excluded", coordinates.exclusions().iterator().next().groupId());
            assertEquals("unwanted", coordinates.exclusions().iterator().next().artifactId());
        }
    }
}
