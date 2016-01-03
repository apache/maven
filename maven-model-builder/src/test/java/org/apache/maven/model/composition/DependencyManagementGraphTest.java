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

import static org.junit.Assert.*;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

/**
 * Test case for {@link DependencyManagementGraph}
 * 
 * @author Michal Kowalcze
 * @since 3.4.0
 */
public class DependencyManagementGraphTest
{

    private DependencyManagement depMgmt_1_1;

    private DependencyManagement depMgmt_1_0;

    private DependencyManagement import_1_0;

    private Dependency dependency_1_1;

    private Dependency dependency_1_0;

    private DependencyManagementGraph dependencyManagementGraph;

    @Before
    public void prepare()
    {
        dependencyManagementGraph = new DependencyManagementGraph();
        dependency_1_0 =
            DefaultDependencyManagementImporterTest.createDependency( "DefaultDependencyManagementImporterTest", "dep",
                                                                      "1.0" );
        dependency_1_1 =
            DefaultDependencyManagementImporterTest.createDependency( "DefaultDependencyManagementImporterTest", "dep",
                                                                      "1.1" );

        depMgmt_1_0 = new DependencyManagement();
        depMgmt_1_0.addDependency( dependency_1_0 );
        dependencyManagementGraph.addDeclaredDependency( depMgmt_1_0, dependency_1_0 );

        depMgmt_1_1 = new DependencyManagement();
        depMgmt_1_1.addDependency( dependency_1_1 );
        dependencyManagementGraph.addDeclaredDependency( depMgmt_1_1, dependency_1_1 );

        import_1_0 = new DependencyManagement();
        dependencyManagementGraph.addImportedDependencyManagement( import_1_0, depMgmt_1_0 );
        import_1_0.addDependency( dependency_1_0 );

    }

    @Test
    public void testDepthOfADirectDependency()
    {
        Optional<Integer> depth = dependencyManagementGraph.findDeclaredDependencyDepth( depMgmt_1_1, dependency_1_1 );
        assertTrue( depth.isPresent() );
        assertEquals( 0, (int) depth.get() );
    }

    @Test
    public void testDepthOfAIndirectDependency()
    {
        Optional<Integer> depth = dependencyManagementGraph.findDeclaredDependencyDepth( import_1_0, dependency_1_0 );
        assertTrue( depth.isPresent() );
        assertEquals( 1, (int) depth.get() );
    }

    @Test
    public void testDepthOfAMissingDependency()
    {
        Optional<Integer> depth = dependencyManagementGraph.findDeclaredDependencyDepth( import_1_0, dependency_1_1 );
        assertFalse( depth.isPresent() );
    }

}
