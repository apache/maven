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

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDependencyManagementImporterTest {
    @Test
    void updateWithImportedFromDependencyLocationAndBomLocationAreNullDependencyReturned() {
        final Dependency dependency = Dependency.newBuilder().build();
        final DependencyManagement depMgmt = DependencyManagement.newBuilder().build();
        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, depMgmt);

        assertThat(dependency).isEqualTo(result);
    }

    @Test
    void updateWithImportedFromDependencyManagementAndDependencyHaveSameSourceDependencyImportedFromSameSource() {
        final InputSource source = new InputSource("SINGLE_SOURCE", "");
        final Dependency dependency = Dependency.newBuilder()
                .location("", new InputLocation(1, 1, source))
                .build();
        final DependencyManagement bom = DependencyManagement.newBuilder()
                .location("", new InputLocation(1, 1, source))
                .build();

        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, bom);

        assertThat(result).isNotNull();
        assertThat(result.getImportedFrom().toString())
                .isEqualTo(bom.getLocation("").toString());
    }

    @Test
    void updateWithImportedFromSingleLevelImportedFromSet() {
        // Arrange
        final InputSource dependencySource = new InputSource("DEPENDENCY", "DEPENDENCY");
        final InputSource bomSource = new InputSource("BOM", "BOM");
        final Dependency dependency = Dependency.newBuilder()
                .location("", new InputLocation(1, 1, dependencySource))
                .build();
        final DependencyManagement bom = DependencyManagement.newBuilder()
                .location("", new InputLocation(2, 2, bomSource))
                .build();

        // Act
        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, bom);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImportedFrom().toString())
                .isEqualTo(bom.getLocation("").toString());
    }

    @Test
    void updateWithImportedFromMultiLevelImportedFromSetChanged() {
        // Arrange
        final InputSource bomSource = new InputSource("BOM", "BOM");
        final InputSource intermediateSource =
                new InputSource("INTERMEDIATE", "INTERMEDIATE", new InputLocation(bomSource));
        final InputSource dependencySource =
                new InputSource("DEPENDENCY", "DEPENDENCY", new InputLocation(intermediateSource));
        final InputLocation bomLocation = new InputLocation(2, 2, bomSource);
        final Dependency dependency = Dependency.newBuilder()
                .location("", new InputLocation(1, 1, dependencySource))
                .importedFrom(bomLocation)
                .build();
        final DependencyManagement bom =
                DependencyManagement.newBuilder().location("", bomLocation).build();

        // Act
        final Dependency result = DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, bom);

        // Assert
        assertThat(result.getImportedFrom().toString())
                .isEqualTo(bom.getLocation("").toString());
    }

    @Test
    void updateWithImportedFromMultiLevelAlreadyFoundInDifferentSourceImportedFromSetMaintained() {
        // Arrange
        final InputSource bomSource = new InputSource("BOM", "BOM");
        final InputSource intermediateSource =
                new InputSource("INTERMEDIATE", "INTERMEDIATE", new InputLocation(bomSource));
        final InputSource dependencySource =
                new InputSource("DEPENDENCY", "DEPENDENCY", new InputLocation(intermediateSource));
        final Dependency dependency = Dependency.newBuilder()
                .location("", new InputLocation(1, 1, dependencySource))
                .build();
        final DependencyManagement differentSource = DependencyManagement.newBuilder()
                .location("", new InputLocation(2, 2, new InputSource("BOM2", "BOM2")))
                .build();

        // Act
        final Dependency result =
                DefaultDependencyManagementImporter.updateWithImportedFrom(dependency, differentSource);

        // Assert
        assertThat(result.getImportedFrom().toString())
                .isEqualTo(differentSource.getLocation("").toString());
    }
}
