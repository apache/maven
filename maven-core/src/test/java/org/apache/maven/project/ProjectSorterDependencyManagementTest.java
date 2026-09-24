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
package org.apache.maven.project;

import java.util.Arrays;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectSorterDependencyManagementTest {

    /**
     * Verifies that a BOM module imported via {@code <dependencyManagement>} is treated as a
     * reactor graph edge: the BOM must be sorted before its consumer (-am direction).
     */
    @Test
    void importedReactorBomPrecedesConsumer() throws Exception {
        MavenProject bom = createProject("org.example", "test-bom", "1-SNAPSHOT");
        MavenProject consumer = createProject("org.example", "test-consumer", "1-SNAPSHOT");

        addBomImport(consumer, "org.example", "test-bom", "${project.version}");

        ProjectSorter sorter = new ProjectSorter(Arrays.asList(consumer, bom));

        assertEquals(Arrays.asList(ProjectSorter.getId(bom)), sorter.getDependencies(ProjectSorter.getId(consumer)));
        assertEquals(Arrays.asList(bom, consumer), sorter.getSortedProjects());
    }

    /**
     * Verifies that a BOM version expressed as an arbitrary project property (not {@code ${project.version}})
     * is resolved correctly. The second branch of {@code resolveImportVersion} looks up the property from
     * {@code project.getProperties()}; if it resolves to the BOM's concrete version the edge must be created.
     */
    @Test
    void importedReactorBomWithPropertyVersion() throws Exception {
        MavenProject bom = createProject("org.example", "test-bom", "1.0");
        MavenProject consumer = createProject("org.example", "test-consumer", "1.0");

        // version expressed as a project property — exercises the second branch of resolveImportVersion
        consumer.getOriginalModel().getProperties().setProperty("bom.version", "1.0");
        addBomImport(consumer, "org.example", "test-bom", "${bom.version}");

        ProjectSorter sorter = new ProjectSorter(Arrays.asList(consumer, bom));

        assertEquals(Arrays.asList(ProjectSorter.getId(bom)), sorter.getDependencies(ProjectSorter.getId(consumer)));
        assertEquals(Arrays.asList(bom, consumer), sorter.getSortedProjects());
    }

    /**
     * Verifies the reverse direction (-amd): the consumer is a downstream dependent of the BOM,
     * so the BOM knows about the consumer as one of its dependents.
     */
    @Test
    void importedReactorBomKnowsItsDependents() throws Exception {
        MavenProject bom = createProject("org.example", "test-bom", "1-SNAPSHOT");
        MavenProject consumer = createProject("org.example", "test-consumer", "1-SNAPSHOT");

        addBomImport(consumer, "org.example", "test-bom", "${project.version}");

        ProjectSorter sorter = new ProjectSorter(Arrays.asList(consumer, bom));

        assertTrue(
                sorter.getDependents(ProjectSorter.getId(bom)).contains(ProjectSorter.getId(consumer)),
                "BOM should list the importing consumer as a dependent");
    }

    // -------------------------------------------------------------------------

    private static void addBomImport(MavenProject project, String groupId, String artifactId, String version) {
        Dependency importedBom = new Dependency();
        importedBom.setGroupId(groupId);
        importedBom.setArtifactId(artifactId);
        importedBom.setVersion(version);
        importedBom.setType("pom");
        importedBom.setScope("import");

        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.addDependency(importedBom);
        project.getOriginalModel().setDependencyManagement(dependencyManagement);
    }

    private static MavenProject createProject(String groupId, String artifactId, String version) {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        MavenProject project = new MavenProject(model);
        project.setOriginalModel(model);
        return project;
    }
}
