package org.apache.maven.it;

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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Test case for <a href="https://issues.apache.org/jira/browse/MNG-6173">MNG-6173</a>.
 */
public class MavenITmng6173GetProjectsAndDependencyGraphTest
        extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6173GetProjectsAndDependencyGraphTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Verifies that {@code MavenSession#getProjects()} returns the projects being built and that
     * {@code MavenSession#getDependencyGraph()} returns the dependency graph.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitShouldReturnProjectsAndProjectDependencyGraph()
            throws Exception
    {

        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/mng-6173-get-projects-and-dependency-graph" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "module-1/target" );
        verifier.deleteDirectory( "module-2/target" );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "module-1" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties properties = verifier.loadProperties( "module-1/target/session.properties" );
        assertEquals( "1", properties.getProperty( "session.projects.size" ) );
        assertEquals( "module-1", properties.getProperty( "session.projects.0.artifactId" ) );
        assertEquals( "1", properties.getProperty("session.projectDependencyGraph.sortedProjects.size" ) );
        assertEquals( "module-1", properties.getProperty(
                "session.projectDependencyGraph.sortedProjects.0.artifactId" ) );
    }

}
