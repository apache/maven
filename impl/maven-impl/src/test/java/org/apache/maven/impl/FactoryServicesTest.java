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

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.DownloadedDependency;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.services.ArtifactFactory;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void dependencyFactoryAcceptsLegacyGetterOnlyImplementations() {
        InternalSession session = newSession();
        Type type = mock(Type.class);
        when(type.id()).thenReturn("jar");
        when(session.requireType("jar")).thenReturn(type);
        when(session.requireDependencyScope("runtime")).thenReturn(DependencyScope.RUNTIME);
        DownloadedDependency dependency = new LegacyDependency(session, type);

        assertEquals("org.example", dependency.groupId());
        assertEquals("library", dependency.artifactId());
        assertEquals("1.0-SNAPSHOT", dependency.version().toString());
        assertEquals("1.0-SNAPSHOT", dependency.baseVersion().toString());
        assertEquals("tests", dependency.classifier());
        assertEquals("jar", dependency.extension());
        assertTrue(dependency.snapshot());
        assertSame(type, dependency.type());
        assertEquals(DependencyScope.RUNTIME, dependency.scope());
        assertEquals(Path.of("library.jar"), dependency.path());
        assertEquals("org.example:library:jar:tests:1.0-SNAPSHOT", dependency.key());

        DependencyCoordinates coordinates = new DefaultDependencyCoordinatesFactory().create(session, dependency);
        assertEquals("org.example", coordinates.groupId());
        assertEquals("library", coordinates.artifactId());
        assertEquals("tests", coordinates.classifier());
        assertEquals(DependencyScope.RUNTIME, coordinates.scope());
        assertTrue(coordinates.optional());
    }

    @Test
    @SuppressWarnings("removal")
    void nounOnlyImplementationsRetainLegacyGetters() {
        InternalSession session = newSession();
        Type type = mock(Type.class);
        when(session.requireType("jar")).thenReturn(type);
        when(session.requireDependencyScope("runtime")).thenReturn(DependencyScope.RUNTIME);
        var resolverArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
                        "org.example:library:jar:tests:1.0-SNAPSHOT")
                .setPath(Path.of("library.jar"));
        Dependency dependency = new DefaultDependency(
                session, new org.eclipse.aether.graph.Dependency(resolverArtifact, "runtime", true));

        assertEquals(dependency.groupId(), dependency.getGroupId());
        assertEquals(dependency.artifactId(), dependency.getArtifactId());
        assertEquals(dependency.version(), dependency.getVersion());
        assertEquals(dependency.baseVersion(), dependency.getBaseVersion());
        assertEquals(dependency.classifier(), dependency.getClassifier());
        assertEquals(dependency.extension(), dependency.getExtension());
        assertEquals(dependency.snapshot(), dependency.isSnapshot());
        assertSame(type, dependency.getType());
        assertEquals(DependencyScope.RUNTIME, dependency.getScope());
        DownloadedArtifact downloaded = new DefaultDownloadedArtifact(session, resolverArtifact);
        assertEquals(downloaded.path(), downloaded.getPath());
    }

    @Test
    @SuppressWarnings("removal")
    void missingAccessorImplementationsFailWithoutRecursion() {
        DownloadedDependency incomplete = new DownloadedDependency() {
            @Override
            public boolean isOptional() {
                return false;
            }

            @Override
            public DependencyCoordinates toCoordinates() {
                throw new UnsupportedOperationException();
            }
        };
        assertThrows(UnsupportedOperationException.class, incomplete::groupId);
        assertThrows(UnsupportedOperationException.class, incomplete::getGroupId);
        assertThrows(UnsupportedOperationException.class, incomplete::type);
        assertThrows(UnsupportedOperationException.class, incomplete::getType);
        assertThrows(UnsupportedOperationException.class, incomplete::scope);
        assertThrows(UnsupportedOperationException.class, incomplete::getScope);
        assertThrows(UnsupportedOperationException.class, incomplete::path);
        assertThrows(UnsupportedOperationException.class, incomplete::getPath);
    }

    @Test
    @SuppressWarnings("removal")
    void exclusionBridgesSupportBothStylesAndPreserveWildcards() {
        Exclusion legacy = new Exclusion() {
            @Override
            public String getGroupId() {
                return null;
            }

            @Override
            public String getArtifactId() {
                return "unwanted";
            }
        };
        Exclusion modern = new Exclusion() {
            @Override
            public String groupId() {
                return "org.excluded";
            }

            @Override
            public String artifactId() {
                return null;
            }
        };

        assertNull(legacy.groupId());
        assertEquals("unwanted", legacy.artifactId());
        assertEquals("org.excluded", modern.getGroupId());
        assertNull(modern.getArtifactId());

        Exclusion incomplete = new Exclusion() {};
        assertThrows(UnsupportedOperationException.class, incomplete::groupId);
        assertThrows(UnsupportedOperationException.class, incomplete::getGroupId);
        assertThrows(UnsupportedOperationException.class, incomplete::artifactId);
        assertThrows(UnsupportedOperationException.class, incomplete::getArtifactId);
    }

    @SuppressWarnings("removal")
    private record LegacyDependency(InternalSession legacySession, Type legacyType) implements DownloadedDependency {
        @Override
        public String getGroupId() {
            return "org.example";
        }

        @Override
        public String getArtifactId() {
            return "library";
        }

        @Override
        public Version getVersion() {
            return legacySession.parseVersion("1.0-SNAPSHOT");
        }

        @Override
        public Version getBaseVersion() {
            return getVersion();
        }

        @Override
        public String getClassifier() {
            return "tests";
        }

        @Override
        public String getExtension() {
            return "jar";
        }

        @Override
        public boolean isSnapshot() {
            return true;
        }

        @Override
        public Type getType() {
            return legacyType;
        }

        @Override
        public DependencyScope getScope() {
            return DependencyScope.RUNTIME;
        }

        @Override
        public boolean isOptional() {
            return true;
        }

        @Override
        public Path getPath() {
            return Path.of("library.jar");
        }

        @Override
        public DependencyCoordinates toCoordinates() {
            return new DefaultDependencyCoordinatesFactory().create(legacySession, this);
        }
    }
}
