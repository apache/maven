package org.apache.maven.project.inheritance.t02;

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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test which demonstrates maven's recursive inheritance where
 * a distinct value is taken from each parent contributing to the
 * the final model of the project being assembled. There is no
 * overriding going on amongst the models being used in this test:
 * each model in the lineage is providing a value that is not present
 * anywhere else in the lineage. We are just making sure that values
 * down in the lineage are bubbling up where they should.
 *
 * @author Jason van Zyl
 */
public class ProjectInheritanceTest
    extends AbstractProjectInheritanceTestCase
{
    // ----------------------------------------------------------------------
    //
    // p4 inherits from p3
    // p3 inherits from p2
    // p2 inherits from p1
    // p1 inherits from p0
    // p0 inherits from super model
    //
    // or we can show it graphically as:
    //
    // p4 ---> p3 ---> p2 ---> p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    @Test
    public void testProjectInheritance()
        throws Exception
    {
        File localRepo = getLocalRepositoryPath();

        System.out.println( "Local repository is at: " + localRepo.getAbsolutePath() );

        File pom0 = new File( localRepo, "p0/pom.xml" );
        File pom1 = new File( pom0.getParentFile(), "p1/pom.xml" );
        File pom2 = new File( pom1.getParentFile(), "p2/pom.xml" );
        File pom3 = new File( pom2.getParentFile(), "p3/pom.xml" );
        File pom4 = new File( pom3.getParentFile(), "p4/pom.xml" );
        File pom5 = new File( pom4.getParentFile(), "p5/pom.xml" );

        System.out.println( "Location of project-4's POM: " + pom4.getPath() );

        // load everything...
        MavenProject project0 = getProject( pom0 );
        MavenProject project1 = getProject( pom1 );
        MavenProject project2 = getProject( pom2 );
        MavenProject project3 = getProject( pom3 );
        MavenProject project4 = getProject( pom4 );
        MavenProject project5 = getProject( pom5 );

        assertEquals( "p4", project4.getName() );

        // ----------------------------------------------------------------------
        // Value inherited from p3
        // ----------------------------------------------------------------------

        assertEquals( "2000", project4.getInceptionYear() );

        // ----------------------------------------------------------------------
        // Value taken from p2
        // ----------------------------------------------------------------------

        assertEquals( "mailing-list", project4.getMailingLists().get( 0 ).getName() );

        // ----------------------------------------------------------------------
        // Value taken from p1
        // ----------------------------------------------------------------------

        assertEquals( "scm-url/p2/p3/p4", project4.getScm().getUrl() );

        // ----------------------------------------------------------------------
        // Value taken from p4
        // ----------------------------------------------------------------------

        assertEquals( "Codehaus", project4.getOrganization().getName() );

        // ----------------------------------------------------------------------
        // Value taken from super model
        // ----------------------------------------------------------------------

        assertEquals( "4.0.0", project4.getModelVersion() );

        Build build = project4.getBuild();
        List<Plugin> plugins = build.getPlugins();

        Map<String, Integer> validPluginCounts = new HashMap<>();

        String testPluginArtifactId = "maven-compiler-plugin";

        // this is the plugin we're looking for.
        validPluginCounts.put( testPluginArtifactId, 0 );

        // these are injected if -DperformRelease=true
        validPluginCounts.put( "maven-deploy-plugin", 0 );
        validPluginCounts.put( "maven-javadoc-plugin", 0 );
        validPluginCounts.put( "maven-source-plugin", 0 );

        Plugin testPlugin = null;

        for ( Plugin plugin : plugins )
        {
            String pluginArtifactId = plugin.getArtifactId();

            assertTrue( validPluginCounts.containsKey( pluginArtifactId ), "Illegal plugin found: " + pluginArtifactId );

            if ( pluginArtifactId.equals( testPluginArtifactId ) )
            {
                testPlugin = plugin;
            }

            Integer count = validPluginCounts.get( pluginArtifactId );

            assertEquals( 0, (int) count, "Multiple copies of plugin: " + pluginArtifactId + " found in POM." );

            count = count + 1;

            validPluginCounts.put( pluginArtifactId, count );
        }

        assertNotNull( testPlugin );

        List<PluginExecution> executions = testPlugin.getExecutions();

        assertEquals( 1, executions.size() );
    }
}
