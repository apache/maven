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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectSorterDependencyManagementTest {
    @Test
    void importedReactorBomPrecedesConsumer() throws Exception {
        MavenProject bom = createProject("org.example", "test-bom", "1-SNAPSHOT");
        MavenProject consumer = createProject("org.example", "test-consumer", "1-SNAPSHOT");

        Dependency importedBom = new Dependency();
        importedBom.setGroupId("org.example");
        importedBom.setArtifactId("test-bom");
        importedBom.setVersion("${project.version}");
        importedBom.setType("pom");
        importedBom.setScope("import");

        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.addDependency(importedBom);
        consumer.getOriginalModel().setDependencyManagement(dependencyManagement);

        ProjectSorter sorter = new ProjectSorter(List.of(consumer, bom));

        assertEquals(List.of(ProjectSorter.getId(bom)), sorter.getDependencies(ProjectSorter.getId(consumer)));
        assertEquals(List.of(bom, consumer), sorter.getSortedProjects());
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
