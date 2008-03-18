package org.apache.maven.project.inheritance.t12;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;

import java.io.File;
import java.util.Map;

/**
 * Verifies that plugin execution sections in the parent POM that have
 * inherit == false are not inherited to the child POM.
 */
public class ProjectInheritanceTest extends AbstractProjectInheritanceTestCase
{
    // ----------------------------------------------------------------------
    //
    // p1 inherits from p0
    // p0 inherits from super model
    //
    // or we can show it graphically as:
    //
    // p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    public void testFalsePluginExecutionInheritValue() throws Exception
    {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File( localRepo, "p0/pom.xml" );
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File( pom0Basedir, "p1/pom.xml" );

        getProjectWithDependencies( pom0 );
        MavenProject project1 = getProjectWithDependencies( pom1 );

        Map pluginMap = project1.getBuild().getPluginsAsMap();
        Plugin compilerPlugin = (Plugin) pluginMap.get( "org.apache.maven.plugins:maven-compiler-plugin" );

        assertNotNull( compilerPlugin );

        Map executionMap = compilerPlugin.getExecutionsAsMap();
        assertNull( "Plugin execution: \'test\' should NOT exist in the compiler plugin specification for the child project!", executionMap.get( "test" ) );
    }
}