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

class DefaultDependencyManagementImporterTest {
    // TODO Need to rewrite tests as soon as the SUT is a bit more stable
    /*
    @Test
    public void testUpdateDependencyHierarchy_SameSource() {
        InputSource source = new InputSource();
        source.setModelId("SINGLE_SOURCE");
        Dependency dependency = new Dependency();
        dependency.setLocation("", new InputLocation(1, 1, source));
        DependencyManagement bom = new DependencyManagement();
        bom.setLocation("", new InputLocation(2, 2, source));

        DefaultDependencyManagementImporter.updateDependencyImports(dependency, bom);

        assertSame(source, dependency.getLocation("").getSource());
        assertSame(source, bom.getLocation("").getSource());
        assertNull(source.getImportedBy());
    }

    @Test
    public void testUpdateDependencyHierarchy_SingleLevel() {
        InputSource dependencySource = new InputSource();
        dependencySource.setModelId("DEPENDENCY");
        InputSource bomSource = new InputSource();
        bomSource.setModelId("BOM");
        Dependency dependency = new Dependency();
        dependency.setLocation("", new InputLocation(1, 1, dependencySource));
        DependencyManagement bom = new DependencyManagement();
        bom.setLocation("", new InputLocation(2, 2, bomSource));

        DefaultDependencyManagementImporter.updateDependencyImports(dependency, bom);

        assertSame(dependencySource, dependency.getLocation("").getSource());
        assertSame(bomSource, bom.getLocation("").getSource());
        assertEquals(bomSource, dependencySource.getImportedBy());
    }

    @Test
    public void testUpdateDependencyHierarchy_MultiLevel() {
        InputSource intermediateSource = new InputSource();
        intermediateSource.setModelId("INTERMEDIATE");
        InputSource dependencySource = new InputSource();
        dependencySource.setModelId("DEPENDENCY");
        dependencySource.setImportedBy(intermediateSource);
        InputSource bomSource = new InputSource();
        bomSource.setModelId("BOM");
        Dependency dependency = new Dependency();
        dependency.setLocation("", new InputLocation(1, 1, dependencySource));
        DependencyManagement bom = new DependencyManagement();
        bom.setLocation("", new InputLocation(2, 2, bomSource));

        DefaultDependencyManagementImporter.updateDependencyImports(dependency, bom);

        assertSame(dependencySource, dependency.getLocation("").getSource());
        assertSame(bomSource, bom.getLocation("").getSource());
        assertEquals(intermediateSource, dependencySource.getImportedBy());
        assertEquals(bomSource, intermediateSource.getImportedBy());
    }

    @Test
    public void testUpdateDependencyHierarchy_PresentSource() {
        InputSource bomSource = new InputSource();
        bomSource.setModelId("BOM");
        InputSource intermediateSource = new InputSource();
        intermediateSource.setModelId("INTERMEDIATE");
        intermediateSource.setImportedBy(bomSource);
        InputSource dependencySource = new InputSource();
        dependencySource.setModelId("DEPENDENCY");
        dependencySource.setImportedBy(intermediateSource);
        Dependency dependency = new Dependency();
        dependency.setLocation("", new InputLocation(1, 1, dependencySource));
        DependencyManagement bom = new DependencyManagement();
        bom.setLocation("", new InputLocation(2, 2, bomSource));

        DefaultDependencyManagementImporter.updateDependencyImports(dependency, bom);

        assertSame(dependencySource, dependency.getLocation("").getSource());
        assertSame(bomSource, bom.getLocation("").getSource());
        assertEquals(intermediateSource, dependencySource.getImportedBy());
        assertEquals(bomSource, intermediateSource.getImportedBy());
        assertNull(bomSource.getImportedBy());
    }
    */
}
