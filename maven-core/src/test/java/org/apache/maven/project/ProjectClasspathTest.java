package org.apache.maven.project;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.MavenTestCase;
import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * @todo relocate to maven-artifact in entirety
 */
public class ProjectClasspathTest extends MavenTestCase
{

    private String dir = "src/test/resources/projects/scope/";

    public void testProjectClasspath()
        throws Exception
    {
        File f = getTestFile( dir + "project-with-scoped-dependencies.xml" );

        // XXX: Because this test fails, we resort to crude reflection hacks, see PLX-108 for the solution
//        assertEquals( ProjectClasspathArtifactResolver.class.getName(), getContainer().lookup( ArtifactResolver.ROLE ) );
        MavenProjectBuilder builder = (MavenProjectBuilder) getContainer().lookup( MavenProjectBuilder.ROLE );
        Field declaredField = builder.getClass().getDeclaredField( "artifactResolver" );
        boolean acc = declaredField.isAccessible();
        declaredField.setAccessible( true );
        declaredField.set( builder, new ProjectClasspathArtifactResolver() );
        declaredField.setAccessible( acc );
        // XXX: end hack

        MavenProject project = getProject( f, true );

        Artifact artifact;

        assertNotNull( "Test project can't be null!", project );

        checkArtifactIdScope( project, "test", "test" );
        checkArtifactIdScope( project, "compile", "compile" );
        checkArtifactIdScope( project, "runtime", "runtime" );
        checkArtifactIdScope( project, "default", "compile" );

        // check all transitive deps of a test dependency are skipped
        artifact = getArtifact( project, "maven-test-test", "scope-compile" );
        assertNull( "Check no test dependencies are transitive", artifact );
        artifact = getArtifact( project, "maven-test-test", "scope-test" );
        assertNull( "Check no test dependencies are transitive", artifact );
        artifact = getArtifact( project, "maven-test-test", "scope-default" );
        assertNull( "Check no test dependencies are transitive", artifact );
        artifact = getArtifact( project, "maven-test-test", "scope-runtime" );
        assertNull( "Check no test dependencies are transitive", artifact );

        // check all transitive deps of a runtime dependency are runtime scope, except for test
        checkGroupIdScope( project, "runtime", "runtime" );

        // check all transitive deps of a compile dependency are runtime scope, except for test
        checkGroupIdScope( project, "compile", "runtime" );

        // check all transitive deps of a default dependency are runtime scope, except for test
        checkGroupIdScope( project, "default", "runtime" );
    }

    private void checkGroupIdScope( MavenProject project, String scope, String scopeValue )
    {
        Artifact artifact;
        String groupId = "maven-test-" + scope;
        artifact = getArtifact( project, groupId, "scope-compile" );
        assertEquals( "Check scope", scopeValue, artifact.getScope() );
        artifact = getArtifact( project, groupId, "scope-test" );
        assertEquals( "Check scope", "test", artifact.getScope() );
        artifact = getArtifact( project, groupId, "scope-default" );
        assertEquals( "Check scope", scopeValue, artifact.getScope() );
        artifact = getArtifact( project, groupId, "scope-runtime" );
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
