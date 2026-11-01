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
package org.apache.maven.model.composition;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultDependencyManagementImporterTest {

    @Test
    void testImportManagementInheritsVersionFromImportedBomWhenLocalEntryHasNoVersion() {
        Dependency localDep = new Dependency();
        localDep.setGroupId("org.springframework.boot");
        localDep.setArtifactId("spring-boot-starter-web");
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("org.springframework.boot");
        exclusion.setArtifactId("spring-boot-starter-tomcat");
        localDep.addExclusion(exclusion);

        DependencyManagement localDepMgmt = new DependencyManagement();
        localDepMgmt.addDependency(localDep);

        Model target = new Model();
        target.setDependencyManagement(localDepMgmt);

        Dependency importedDep = new Dependency();
        importedDep.setGroupId("org.springframework.boot");
        importedDep.setArtifactId("spring-boot-starter-web");
        importedDep.setVersion("3.0.0");

        DependencyManagement importedBom = new DependencyManagement();
        importedBom.addDependency(importedDep);

        DefaultDependencyManagementImporter importer = new DefaultDependencyManagementImporter();
        importer.importManagement(target, List.of(importedBom), null, null);

        List<Dependency> resultDeps = target.getDependencyManagement().getDependencies();
        assertEquals(1, resultDeps.size());

        Dependency merged = resultDeps.get(0);
        assertEquals("org.springframework.boot", merged.getGroupId());
        assertEquals("spring-boot-starter-web", merged.getArtifactId());
        assertEquals("3.0.0", merged.getVersion());
        assertEquals(1, merged.getExclusions().size());
        assertEquals("spring-boot-starter-tomcat", merged.getExclusions().get(0).getArtifactId());
    }
}
