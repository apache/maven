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

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
