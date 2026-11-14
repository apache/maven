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
import org.apache.maven.api.model.Inclusion;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
                .exclusions(List.of(Exclusion.newBuilder()
                        .groupId("org.junit.vintage")
                        .artifactId("junit-vintage-engine")
                        .build()))
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
        assertEquals(1, merged.getExclusions().size());
        assertEquals("org.junit.vintage", merged.getExclusions().get(0).getGroupId());
        assertEquals("junit-vintage-engine", merged.getExclusions().get(0).getArtifactId());
    }

    @Test
    void testImportManagementWithEquivalentInclusionsUsingIdAndChildElements() {
        Inclusion inc1 =
                Inclusion.newBuilder().id("org.junit.jupiter:junit-jupiter-api").build();
        Inclusion inc2 = Inclusion.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .build();

        Dependency dep1 = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version("5.10.0")
                .includes(List.of(inc1))
                .build();

        Dependency dep2 = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version("5.10.0")
                .includes(List.of(inc2))
                .build();

        DependencyManagement bom1 =
                DependencyManagement.newBuilder().dependencies(List.of(dep1)).build();
        DependencyManagement bom2 =
                DependencyManagement.newBuilder().dependencies(List.of(dep2)).build();

        Model target = Model.newBuilder().build();
        DefaultDependencyManagementImporter importer = new DefaultDependencyManagementImporter();
        SimpleProblemCollector problems = new SimpleProblemCollector();

        Model result =
                importer.importManagement(target, List.of(bom1, bom2), mock(ModelBuilderRequest.class), problems);

        assertEquals(1, result.getDependencyManagement().getDependencies().size());
        assertTrue(problems.getWarnings().isEmpty(), "Expected no warnings for semantically identical inclusions");
    }

    @Test
    void testImportManagementWithConflictingInclusionsUsingDifferentIds() {
        Inclusion inc1 = Inclusion.newBuilder().id("org.junit.jupiter:foo").build();
        Inclusion inc2 = Inclusion.newBuilder().id("org.junit.jupiter:bar").build();

        Dependency dep1 = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version("5.10.0")
                .includes(List.of(inc1))
                .build();

        Dependency dep2 = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version("5.10.0")
                .includes(List.of(inc2))
                .build();

        DependencyManagement bom1 =
                DependencyManagement.newBuilder().dependencies(List.of(dep1)).build();
        DependencyManagement bom2 =
                DependencyManagement.newBuilder().dependencies(List.of(dep2)).build();

        Model target = Model.newBuilder().build();
        DefaultDependencyManagementImporter importer = new DefaultDependencyManagementImporter();
        SimpleProblemCollector problems = new SimpleProblemCollector();

        Model result =
                importer.importManagement(target, List.of(bom1, bom2), mock(ModelBuilderRequest.class), problems);

        assertEquals(1, result.getDependencyManagement().getDependencies().size());
        assertEquals(1, problems.getWarnings().size());
        assertTrue(problems.getWarnings().get(0).contains("Ignored POM import"));
    }
}
