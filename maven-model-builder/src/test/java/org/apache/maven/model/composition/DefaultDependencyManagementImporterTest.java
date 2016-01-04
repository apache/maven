package org.apache.maven.model.composition;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Test case for {@link DefaultDependencyManagementImporter}
 * 
 * @author Michal Kowalcze
 * @since 3.4.0
 */
public class DefaultDependencyManagementImporterTest
{

    private DependencyManagement depMgmt_1_1;

    private DependencyManagement depMgmt_1_0;

    private DependencyManagement import_1_0;

    private Dependency dependency_1_1;

    private Dependency dependency_1_0;

    private DefaultDependencyManagementImporter defaultDependencyManagementImporter;

    private DependencyManagementGraph dependencyManagementGraph;

    @Before
    public void prepare()
    {
        dependencyManagementGraph = new DependencyManagementGraph();
        dependency_1_0 = createDependency( "DefaultDependencyManagementImporterTest", "dep", "1.0" );
        dependency_1_1 = createDependency( "DefaultDependencyManagementImporterTest", "dep", "1.1" );

        depMgmt_1_0 = new DependencyManagement();
        depMgmt_1_0.addDependency( dependency_1_0 );
        dependencyManagementGraph.addDeclaredDependency( depMgmt_1_0, dependency_1_0 );

        depMgmt_1_1 = new DependencyManagement();
        depMgmt_1_1.addDependency( dependency_1_1 );
        dependencyManagementGraph.addDeclaredDependency( depMgmt_1_1, dependency_1_1 );

        import_1_0 = new DependencyManagement();
        dependencyManagementGraph.addImportedDependencyManagement( import_1_0, depMgmt_1_0 );
        import_1_0.addDependency( dependency_1_0 );

        defaultDependencyManagementImporter = new DefaultDependencyManagementImporter();
    }

    @Test
    public void testDependencyManagementFromCloserImport()
    {
        Model target = createModelWithNearestMatchEnabled();
        ModelBuildingRequest request = null;
        List<DependencyManagement> sources = ImmutableList.of( import_1_0, depMgmt_1_1 );
        SimpleProblemCollector problems = new SimpleProblemCollector();

        defaultDependencyManagementImporter.importManagement( target, sources, request, problems,
                                                              dependencyManagementGraph );

        assertDependencyVersion( target, dependency_1_1 );
        assertNoProblems( problems );
    }

    @Test
    public void testDependencyManagementFromFirstMatchImport()
    {
        Model target = createStandardModel();
        ModelBuildingRequest request = null;
        List<DependencyManagement> sources = ImmutableList.of( import_1_0, depMgmt_1_1 );
        SimpleProblemCollector problems = new SimpleProblemCollector();

        defaultDependencyManagementImporter.importManagement( target, sources, request, problems,
                                                              dependencyManagementGraph );

        assertDependencyVersion( target, dependency_1_0 );
        assertNoProblems( problems );
    }

    @Test
    public void testDependencyManagementFromCloserImportWithoutDepthCheck()
    {
        Model target = createModelWithNearestMatchEnabled();
        ModelBuildingRequest request = null;
        List<DependencyManagement> sources = ImmutableList.of( depMgmt_1_1, import_1_0 );
        SimpleProblemCollector problems = new SimpleProblemCollector();

        defaultDependencyManagementImporter.importManagement( target, sources, request, problems,
                                                              dependencyManagementGraph );

        assertDependencyVersion( target, dependency_1_1 );
        assertNoProblems( problems );
    }

    @Test
    public void testDirectDependencyManagement()
    {
        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.addDependency( dependency_1_0 );
        Model target = createModelWithNearestMatchEnabled();
        target.setDependencyManagement( dependencyManagement );
        ModelBuildingRequest request = null;
        List<DependencyManagement> sources = ImmutableList.of( import_1_0, depMgmt_1_1 );
        SimpleProblemCollector problems = new SimpleProblemCollector();

        defaultDependencyManagementImporter.importManagement( target, sources, request, problems,
                                                              dependencyManagementGraph );

        assertDependencyVersion( target, dependency_1_0 );
        assertNoProblems( problems );
    }

    static Dependency createDependency( String groupId, String artifactId, String version )
    {
        Dependency dependency = new Dependency();

        dependency.setGroupId( groupId );
        dependency.setArtifactId( artifactId );
        dependency.setVersion( version );

        return dependency;
    }

    private void assertDependencyVersion( Model target, Dependency dependency )
    {
        List<Dependency> dependencies = target.getDependencyManagement().getDependencies();
        assertEquals( 1, dependencies.size() );
        assertEquals( dependency, dependencies.get( 0 ) );
    }

    private void assertNoProblems( SimpleProblemCollector problems )
    {
        assertEquals( 0, problems.getWarnings().size() );
        assertEquals( 0, problems.getFatals().size() );
        assertEquals( 0, problems.getErrors().size() );
    }

    private Model createModelWithNearestMatchEnabled()
    {

        Model model = createStandardModel();

        Properties properties = new Properties();
        properties.setProperty( "org.apache.maven.model.composition.DependencyManagementImporter.nearestMatchEnabled",
                                "true" );
        model.setProperties( properties );

        return model;
    }

    private Model createStandardModel()
    {

        Model model = new Model();

        return model;
    }
}
