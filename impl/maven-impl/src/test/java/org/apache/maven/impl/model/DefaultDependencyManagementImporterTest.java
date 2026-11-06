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
package org.apache.maven.impl.model;

import java.util.List;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultDependencyManagementImporterTest {
    @Test
    void testUpdateWithImportedFromDependencyLocationAndBomLocationAreNullDependencyReturned() {
        final Dependency dependency = Dependency.newBuilder().build();
        final DependencyManagement depMgmt = DependencyManagement.newBuilder().build();
        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, depMgmt);

        assertEquals(result, dependency);
    }

    @Test
    void testUpdateWithImportedFromDependencyManagementAndDependencyHaveSameSourceDependencyImportedFromSameSource() {
        final InputSource source = InputSource.of("SINGLE_SOURCE", "");
        final Dependency dependency = Dependency.newBuilder()
                .location("", InputLocation.of(1, 1, source))
                .build();
        final DependencyManagement bom = DependencyManagement.newBuilder()
                .location("", InputLocation.of(1, 1, source))
                .build();

        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, bom);

        assertNotNull(result);
        String actualImportedFrom = result.getImportedFrom().toString();
        String expectedImportedFrom = bom.getLocation("").toString();
        assertEquals(
                expectedImportedFrom,
                actualImportedFrom,
                "Expected importedFrom to be " + expectedImportedFrom + " but was " + actualImportedFrom);
    }

    @Test
    public void testUpdateWithImportedFromSingleLevelImportedFromSet() {
        // Arrange
        final InputSource dependencySource = InputSource.of("DEPENDENCY", "DEPENDENCY");
        final InputSource bomSource = InputSource.of("BOM", "BOM");
        final Dependency dependency = Dependency.newBuilder()
                .location("", InputLocation.of(1, 1, dependencySource))
                .build();
        final DependencyManagement bom = DependencyManagement.newBuilder()
                .location("", InputLocation.of(2, 2, bomSource))
                .build();

        // Act
        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, bom);

        // Assert
        assertNotNull(result);
        String actualImportedFrom = result.getImportedFrom().toString();
        String expectedImportedFrom = bom.getLocation("").toString();
        assertEquals(
                expectedImportedFrom,
                actualImportedFrom,
                "Expected importedFrom to be " + expectedImportedFrom + " but was " + actualImportedFrom);
    }

    @Test
    public void testUpdateWithImportedFromMultiLevelImportedFromSetChanged() {
        // Arrange
        final InputSource bomSource = InputSource.of("BOM", "BOM");
        final InputSource intermediateSource =
                InputSource.of("INTERMEDIATE", "INTERMEDIATE", InputLocation.of(bomSource));
        final InputSource dependencySource =
                InputSource.of("DEPENDENCY", "DEPENDENCY", InputLocation.of(intermediateSource));
        final InputLocation bomLocation = InputLocation.of(2, 2, bomSource);
        final Dependency dependency = Dependency.newBuilder()
                .location("", InputLocation.of(1, 1, dependencySource))
                .importedFrom(bomLocation)
                .build();
        final DependencyManagement bom =
                DependencyManagement.newBuilder().location("", bomLocation).build();

        // Act
        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, bom);

        // Assert
        String actualImportedFrom = result.getImportedFrom().toString();
        String expectedImportedFrom = bom.getLocation("").toString();
        assertEquals(
                expectedImportedFrom,
                actualImportedFrom,
                "Expected importedFrom to be " + expectedImportedFrom + " but was " + actualImportedFrom);
    }

    @Test
    public void testUpdateWithImportedFromMultiLevelAlreadyFoundInDifferentSourceImportedFromSetMaintained() {
        // Arrange
        final InputSource bomSource = InputSource.of("BOM", "BOM");
        final InputSource intermediateSource =
                InputSource.of("INTERMEDIATE", "INTERMEDIATE", InputLocation.of(bomSource));
        final InputSource dependencySource =
                InputSource.of("DEPENDENCY", "DEPENDENCY", InputLocation.of(intermediateSource));
        final Dependency dependency = Dependency.newBuilder()
                .location("", InputLocation.of(1, 1, dependencySource))
                .build();
        final DependencyManagement differentSource = DependencyManagement.newBuilder()
                .location("", InputLocation.of(2, 2, InputSource.of("BOM2", "BOM2")))
                .build();

        // Act
        final Dependency result =
                DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, differentSource);

        // Assert
        String actualImportedFrom = result.getImportedFrom().toString();
        String expectedImportedFrom = differentSource.getLocation("").toString();
        assertEquals(
                expectedImportedFrom,
                actualImportedFrom,
                "Expected importedFrom to be " + expectedImportedFrom + " but was " + actualImportedFrom);
    }

    @Test
    void testImportManagementInheritsVersionFromImportedBomWhenLocalEntryHasNoVersion() {
        // A locally-declared dep mgmt entry with scope=provided but no version
        Dependency localDep = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .scope("provided")
                .build();

        Model target = Model.newBuilder()
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(localDep))
                        .build())
                .build();

        // An imported BOM providing the same dependency with a version
        Dependency importedDep = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version("5.10.0")
                .build();

        DependencyManagement importedBom = DependencyManagement.newBuilder()
                .dependencies(List.of(importedDep))
                .build();

        DefaultDependencyManagementImporter importer = new DefaultDependencyManagementImporter();
        Model result = importer.importManagement(
                target, List.of(importedBom), mock(ModelBuilderRequest.class), mock(ModelProblemCollector.class));

        List<Dependency> resultDeps = result.getDependencyManagement().getDependencies();
        assertEquals(1, resultDeps.size());

        Dependency merged = resultDeps.get(0);
        assertEquals("org.junit.jupiter", merged.getGroupId());
        assertEquals("junit-jupiter-api", merged.getArtifactId());
        assertEquals("5.10.0", merged.getVersion());
        assertEquals("provided", merged.getScope());
    }

    @Test
    void testImportManagementDoesNotWarnOnIdenticalEntries() {
        assertEquals(List.of(), importWarnings(managed().build(), managed().build()));
    }

    @Test
    void testImportManagementDoesNotWarnOnExclusionsDeclaredInDifferentOrder() {
        Dependency first = managed()
                .exclusions(List.of(exclusion("x", "a"), exclusion("x", "b")))
                .build();
        Dependency second = managed()
                .exclusions(List.of(exclusion("x", "b"), exclusion("x", "a")))
                .build();

        assertEquals(List.of(), importWarnings(first, second));
    }

    @Test
    void testImportManagementWarningNamesDifferingExclusions() {
        Dependency first = managed().exclusions(List.of(exclusion("x", "a"))).build();
        Dependency second = managed().exclusions(List.of(exclusion("x", "b"))).build();

        List<String> warnings = importWarnings(first, second);

        assertEquals(1, warnings.size());
        assertTrue(
                warnings.get(0)
                        .startsWith("Ignored POM import for: g:a:jar:1[exclusions: x:b] as already imported "
                                + "g:a:jar:1[exclusions: x:a]."),
                warnings.get(0));
    }

    @Test
    void testImportManagementWarningDistinguishesUnsetFromExplicitCompileScope() {
        // an unset scope is not managed, unlike an explicit compile scope, so both must not render as "@compile"
        List<String> warnings =
                importWarnings(managed().build(), managed().scope("compile").build());

        assertEquals(1, warnings.size());
        assertTrue(
                warnings.get(0).startsWith("Ignored POM import for: g:a:jar:1@compile as already imported g:a:jar:1."),
                warnings.get(0));
    }

    @Test
    void testImportManagementWarningDistinguishesUnsetFromExplicitNonOptional() {
        // an unset optional flag is not managed, unlike an explicit false
        List<String> warnings =
                importWarnings(managed().build(), managed().optional("false").build());

        assertEquals(1, warnings.size());
        assertTrue(
                warnings.get(0)
                        .startsWith("Ignored POM import for: g:a:jar:1[optional=false] as already imported g:a:jar:1."),
                warnings.get(0));
    }

    private static Dependency.Builder managed() {
        return Dependency.newBuilder().groupId("g").artifactId("a").type("jar").version("1");
    }

    private static Exclusion exclusion(String groupId, String artifactId) {
        return Exclusion.newBuilder().groupId(groupId).artifactId(artifactId).build();
    }

    /**
     * Imports two BOMs managing the given dependencies, in order, and returns the reported warnings.
     */
    private static List<String> importWarnings(Dependency first, Dependency second) {
        ModelProblemCollector problems = mock(ModelProblemCollector.class);
        new DefaultDependencyManagementImporter()
                .importManagement(
                        Model.newBuilder().build(),
                        List.of(
                                DependencyManagement.newBuilder()
                                        .dependencies(List.of(first))
                                        .build(),
                                DependencyManagement.newBuilder()
                                        .dependencies(List.of(second))
                                        .build()),
                        mock(ModelBuilderRequest.class),
                        problems);
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(problems, atLeast(0)).add(eq(Severity.WARNING), any(), messages.capture(), (InputLocation) any());
        return messages.getAllValues();
    }
}
