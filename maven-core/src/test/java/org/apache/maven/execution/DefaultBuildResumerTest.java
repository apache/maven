package org.apache.maven.execution;

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

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

@RunWith( MockitoJUnitRunner.class )
public class DefaultBuildResumerTest
{
    private final DefaultBuildResumer buildResumer = new DefaultBuildResumer();

    private MavenExecutionResult result;

    @Before
    public void before() {
        result = new DefaultMavenExecutionResult();
    }

    @Test
    public void resumeFromGetsDetermined()
    {
        MavenProject projectA = createSucceededMavenProject( "A" );
        MavenProject projectB = createFailedMavenProject( "B" );
        result.setTopologicallySortedProjects( asList( projectA, projectB ) );

        Properties properties = buildResumer.determineResumptionProperties( result );

        assertThat( properties.get( "resumeFrom" ), is( "test:B" ) );
    }

    @Test
    public void resumeFromIsIgnoredWhenFirstProjectFails()
    {
        MavenProject projectA = createFailedMavenProject( "A" );
        MavenProject projectB = createMavenProject( "B" );
        result.setTopologicallySortedProjects( asList( projectA, projectB ) );

        Properties properties = buildResumer.determineResumptionProperties( result );

        assertThat( properties.containsKey( "resumeFrom" ), is(false) );
    }

    @Test
    public void projectsSucceedingAfterFailedProjectsAreExcluded()
    {
        MavenProject projectA = createSucceededMavenProject( "A" );
        MavenProject projectB = createFailedMavenProject( "B" );
        MavenProject projectC = createSucceededMavenProject( "C" );
        result.setTopologicallySortedProjects( asList( projectA, projectB, projectC ) );

        Properties properties = buildResumer.determineResumptionProperties( result );

        assertThat( properties.get( "excludedProjects" ), is("test:C") );
    }

    @Test
    public void projectsDependingOnFailedProjectsAreNotExcluded()
    {
        MavenProject projectA = createSucceededMavenProject( "A" );
        MavenProject projectB = createFailedMavenProject( "B" );
        MavenProject projectC = createSucceededMavenProject( "C" );
        projectC.setDependencies( singletonList( toDependency( projectB ) ) );
        result.setTopologicallySortedProjects( asList( projectA, projectB, projectC ) );

        Properties properties = buildResumer.determineResumptionProperties( result );

        assertThat( properties.containsKey( "excludedProjects" ), is(false) );
    }

    @Test
    public void projectsFailingAfterAnotherFailedProjectAreNotExcluded()
    {
        MavenProject projectA = createSucceededMavenProject( "A" );
        MavenProject projectB = createFailedMavenProject( "B" );
        MavenProject projectC = createSucceededMavenProject( "C" );
        MavenProject projectD = createFailedMavenProject( "D" );
        result.setTopologicallySortedProjects( asList( projectA, projectB, projectC, projectD ) );

        Properties properties = buildResumer.determineResumptionProperties( result );

        assertThat( properties.get( "resumeFrom" ), is("test:B") );
        assertThat( properties.get( "excludedProjects" ), is("test:C") );
    }

    @Test
    public void multipleExcludedProjectsAreCommaSeparated()
    {
        MavenProject projectA = createFailedMavenProject( "A" );
        MavenProject projectB = createSucceededMavenProject( "B" );
        MavenProject projectC = createSucceededMavenProject( "C" );
        result.setTopologicallySortedProjects( asList( projectA, projectB, projectC ) );

        Properties properties = buildResumer.determineResumptionProperties( result );

        assertThat( properties.get( "excludedProjects" ), is( "test:B, test:C" ) );
    }

    @Test
    public void resumeFromPropertyGetsApplied()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        Properties properties = new Properties();
        properties.setProperty( "resumeFrom", ":module-a" );

        buildResumer.applyResumptionProperties( request, properties );

        assertThat( request.getResumeFrom(), is( ":module-a" ) );
    }

    @Test
    public void resumeFromPropertyDoesNotOverrideExistingRequestParameters()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setResumeFrom( ":module-b" );
        Properties properties = new Properties();
        properties.setProperty( "resumeFrom", ":module-a" );

        buildResumer.applyResumptionProperties( request, properties );

        assertThat( request.getResumeFrom(), is( ":module-b" ) );
    }

    @Test
    public void excludedProjectsFromPropertyGetsAddedToExistingRequestParameters()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        List<String> excludedProjects = new ArrayList<>();
        excludedProjects.add( ":module-a" );
        request.setExcludedProjects( excludedProjects );
        Properties properties = new Properties();
        properties.setProperty( "excludedProjects", ":module-b, :module-c" );

        buildResumer.applyResumptionProperties( request, properties );

        assertThat( request.getExcludedProjects(), contains( ":module-a", ":module-b", ":module-c" ) );
    }

    @Test
    public void resumeFromSelectorIsSuggestedWithoutGroupId()
    {
        List<MavenProject> allProjects = asList(
                createMavenProject( "group", "module-a" ),
                createMavenProject( "group", "module-b" ) );
        MavenProject failedProject = allProjects.get( 0 );

        String selector = buildResumer.getResumeFromSelector( allProjects, failedProject );

        assertThat( selector, is( ":module-a" ) );
    }

    @Test
    public void resumeFromSelectorContainsGroupIdWhenArtifactIdIsNotUnique()
    {
        List<MavenProject> allProjects = asList(
                createMavenProject( "group-a", "module" ),
                createMavenProject( "group-b", "module" ) );
        MavenProject failedProject = allProjects.get( 0 );

        String selector = buildResumer.getResumeFromSelector( allProjects, failedProject );

        assertThat( selector, is( "group-a:module" ) );
    }

    private MavenProject createMavenProject( String artifactId )
    {
        return createMavenProject( "test", artifactId );
    }

    private MavenProject createMavenProject( String groupId, String artifactId )
    {
        MavenProject project = new MavenProject();
        project.setGroupId( groupId );
        project.setArtifactId( artifactId );
        return project;
    }

    private Dependency toDependency( MavenProject mavenProject )
    {
        Dependency dependency = new Dependency();
        dependency.setGroupId( mavenProject.getGroupId() );
        dependency.setArtifactId( mavenProject.getArtifactId() );
        dependency.setVersion( mavenProject.getVersion() );
        return dependency;
    }

    private MavenProject createSucceededMavenProject( String artifactId )
    {
        MavenProject project = createMavenProject( artifactId );
        result.addBuildSummary( new BuildSuccess( project, 0 ) );
        return project;
    }

    private MavenProject createFailedMavenProject( String artifactId )
    {
        MavenProject project = createMavenProject( artifactId );
        result.addBuildSummary( new BuildFailure( project, 0, new Exception() ) );
        result.addException( new LifecycleExecutionException( "", project ) );
        return project;
    }
}
