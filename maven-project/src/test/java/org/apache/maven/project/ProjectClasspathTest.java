package org.apache.maven.project;

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

import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.Iterator;

/**
 * @todo relocate to maven-artifact in entirety
 */
public class ProjectClasspathTest
    extends AbstractMavenProjectTestCase
{
    private String dir = "projects/scope/";

    public void testProjectClasspath()
        throws Exception
    {
        File f = getFileForClasspathResource( dir + "project-with-scoped-dependencies.xml" );

//        assertEquals( TestArtifactResolver.class, getContainer().lookup( ArtifactResolver.ROLE ).getClass() );
        TestProjectBuilder builder = (TestProjectBuilder) getContainer().lookup( MavenProjectBuilder.ROLE, "test" );
        
        TestArtifactResolver testArtifactResolver = (TestArtifactResolver) getContainer().lookup( TestArtifactResolver.class.getName() );
        
        builder.setArtifactResolver( testArtifactResolver );
        builder.setArtifactMetadataSource( testArtifactResolver.source() );
        
        MavenProject project = getProjectWithDependencies( f );

        Artifact artifact;

        assertNotNull( "Test project can't be null!", project );

        checkArtifactIdScope( project, "provided", "provided" );
        checkArtifactIdScope( project, "test", "test" );
        checkArtifactIdScope( project, "compile", "compile" );
        checkArtifactIdScope( project, "runtime", "runtime" );
        checkArtifactIdScope( project, "default", "compile" );

        // check all transitive deps of a test dependency are test, except test and provided which is skipped
        artifact = getArtifact( project, "maven-test-test", "scope-provided" );
        assertNull( "Check no provided dependencies are transitive", artifact );
        artifact = getArtifact( project, "maven-test-test", "scope-test" );
        assertNull( "Check no test dependencies are transitive", artifact );
        artifact = getArtifact( project, "maven-test-test", "scope-compile" );
        assertEquals( "Check scope", "test", artifact.getScope() );
        artifact = getArtifact( project, "maven-test-test", "scope-default" );
        assertEquals( "Check scope", "test", artifact.getScope() );
        artifact = getArtifact( project, "maven-test-test", "scope-runtime" );
        assertEquals( "Check scope", "test", artifact.getScope() );

        // check all transitive deps of a provided dependency are provided scope, except for test
        checkGroupIdScope( project, "provided", "maven-test-provided" );
        artifact = getArtifact( project, "maven-test-provided", "scope-runtime" );
        assertEquals( "Check scope", "provided", artifact.getScope() );

        // check all transitive deps of a runtime dependency are runtime scope, except for test
        checkGroupIdScope( project, "runtime", "maven-test-runtime" );
        artifact = getArtifact( project, "maven-test-runtime", "scope-runtime" );
        assertEquals( "Check scope", "runtime", artifact.getScope() );

        // check all transitive deps of a compile dependency are compile scope, except for runtime and test
        checkGroupIdScope( project, "compile", "maven-test-compile" );
        artifact = getArtifact( project, "maven-test-compile", "scope-runtime" );
        assertEquals( "Check scope", "runtime", artifact.getScope() );

        // check all transitive deps of a default dependency are compile scope, except for runtime and test
        checkGroupIdScope( project, "compile", "maven-test-default" );
        artifact = getArtifact( project, "maven-test-default", "scope-runtime" );
        assertEquals( "Check scope", "runtime", artifact.getScope() );
    }

    private void checkGroupIdScope( MavenProject project, String scopeValue, String groupId )
    {
        Artifact artifact;
        artifact = getArtifact( project, groupId, "scope-compile" );
        assertEquals( "Check scope", scopeValue, artifact.getScope() );
        artifact = getArtifact( project, groupId, "scope-test" );
        assertNull( "Check test dependency is not transitive", artifact );
        artifact = getArtifact( project, groupId, "scope-provided" );
        assertNull( "Check provided dependency is not transitive", artifact );
        artifact = getArtifact( project, groupId, "scope-default" );
        assertEquals( "Check scope", scopeValue, artifact.getScope() );
    }

    private void checkArtifactIdScope( MavenProject project, String scope, String scopeValue )
    {
        String artifactId = "scope-" + scope;
        Artifact artifact = getArtifact( project, "maven-test", artifactId );
        assertEquals( "Check scope", scopeValue, artifact.getScope() );
    }

    private Artifact getArtifact( MavenProject project, String groupId, String artifactId )
    {
        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            if ( artifactId.equals( a.getArtifactId() ) && a.getGroupId().equals( groupId ) )
            {
                return a;
            }
        }
        return null;
    }
}
