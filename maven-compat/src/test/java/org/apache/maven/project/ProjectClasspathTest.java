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

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ProjectClasspathTest
    extends AbstractMavenProjectTestCase
{
    static final String dir = "projects/scope/";

    @Override
    @BeforeEach
    public void setUp()
            throws Exception
    {
        super.setUp();

        ArtifactResolver resolver = getContainer().lookup( ArtifactResolver.class, "classpath" );
        DefaultArtifactDescriptorReader pomReader = (DefaultArtifactDescriptorReader)getContainer().lookup(ArtifactDescriptorReader.class);
        pomReader.setArtifactResolver( resolver );

        projectBuilder = getContainer().lookup( ProjectBuilder.class, "classpath" );
    }

    @Test
    public void testProjectClasspath()
        throws Exception
    {
        File f = getFileForClasspathResource( dir + "project-with-scoped-dependencies.xml" );

        MavenProject project = getProjectWithDependencies( f );

        Artifact artifact;

        assertNotNull( project, "Test project can't be null!" );

        checkArtifactIdScope( project, "provided", "provided" );
        checkArtifactIdScope( project, "test", "test" );
        checkArtifactIdScope( project, "compile", "compile" );
        checkArtifactIdScope( project, "runtime", "runtime" );
        checkArtifactIdScope( project, "default", "compile" );

        // check all transitive deps of a test dependency are test, except test and provided which is skipped
        artifact = getArtifact( project, "maven-test-test", "scope-provided" );
        assertNull( artifact, "Check no provided dependencies are transitive" );
        artifact = getArtifact( project, "maven-test-test", "scope-test" );
        assertNull( artifact, "Check no test dependencies are transitive" );

        artifact = getArtifact( project, "maven-test-test", "scope-compile" );
        assertNotNull( artifact );

        System.out.println( "a = " + artifact );
        System.out.println( "b = " + artifact.getScope() );
        assertEquals( "test", artifact.getScope(), "Check scope" );
        artifact = getArtifact( project, "maven-test-test", "scope-default" );
        assertEquals( "test", artifact.getScope(), "Check scope" );
        artifact = getArtifact( project, "maven-test-test", "scope-runtime" );
        assertEquals( "test", artifact.getScope(), "Check scope" );

        // check all transitive deps of a provided dependency are provided scope, except for test
        checkGroupIdScope( project, "provided", "maven-test-provided" );
        artifact = getArtifact( project, "maven-test-provided", "scope-runtime" );
        assertEquals( "provided", artifact.getScope(), "Check scope" );

        // check all transitive deps of a runtime dependency are runtime scope, except for test
        checkGroupIdScope( project, "runtime", "maven-test-runtime" );
        artifact = getArtifact( project, "maven-test-runtime", "scope-runtime" );
        assertEquals( "runtime", artifact.getScope(), "Check scope" );

        // check all transitive deps of a compile dependency are compile scope, except for runtime and test
        checkGroupIdScope( project, "compile", "maven-test-compile" );
        artifact = getArtifact( project, "maven-test-compile", "scope-runtime" );
        assertEquals( "runtime", artifact.getScope(), "Check scope" );

        // check all transitive deps of a default dependency are compile scope, except for runtime and test
        checkGroupIdScope( project, "compile", "maven-test-default" );
        artifact = getArtifact( project, "maven-test-default", "scope-runtime" );
        assertEquals( "runtime", artifact.getScope(), "Check scope" );
    }

    private void checkGroupIdScope( MavenProject project, String scopeValue, String groupId )
    {
        Artifact artifact;
        artifact = getArtifact( project, groupId, "scope-compile" );
        assertEquals( scopeValue, artifact.getScope(), "Check scope" );
        artifact = getArtifact( project, groupId, "scope-test" );
        assertNull( artifact, "Check test dependency is not transitive" );
        artifact = getArtifact( project, groupId, "scope-provided" );
        assertNull( artifact, "Check provided dependency is not transitive" );
        artifact = getArtifact( project, groupId, "scope-default" );
        assertEquals( scopeValue, artifact.getScope(), "Check scope" );
    }

    private void checkArtifactIdScope( MavenProject project, String scope, String scopeValue )
    {
        String artifactId = "scope-" + scope;
        Artifact artifact = getArtifact( project, "maven-test", artifactId );
        assertNotNull( artifact );
        assertEquals( scopeValue, artifact.getScope(), "Check scope" );
    }

    private Artifact getArtifact( MavenProject project, String groupId, String artifactId )
    {
        System.out.println( "[ Looking for " + groupId + ":" + artifactId + " ]" );
        for ( Artifact a : project.getArtifacts() )
        {
            System.out.println( a.toString() );
            if ( artifactId.equals( a.getArtifactId() ) && a.getGroupId().equals( groupId ) )
            {
                System.out.println( "RETURN" );
                return a;
            }
        }
        System.out.println( "Return null" );
        return null;
    }

}
