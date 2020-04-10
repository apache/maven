package org.apache.maven.graph;

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

import com.google.common.collect.ImmutableMap;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.Result;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static org.apache.maven.execution.MavenExecutionRequest.*;
import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( Parameterized.class )
public class DefaultGraphBuilderTest
{
    private static final String MODULE_A = "module-a";
    private static final String MODULE_B = "module-b";
    private static final String MODULE_C = "module-c";

    @InjectMocks
    private DefaultGraphBuilder graphBuilder;

    @Mock
    private ProjectBuilder projectBuilder;

    @Mock
    private MavenSession session;

    @Mock
    private MavenExecutionRequest mavenExecutionRequest;

    private Map<String, MavenProject> artifactIdProjectMap;

    // Parameters for the test
    private String parameterDescription;
    private List<String> parameterSelectedProjects;
    private List<String> parameterExcludedProjects;
    private String parameterResumeFrom;
    private String parameterMakeBehavior;
    private List<String> parameterExpectedReactorProjects;

    @Parameters(name = "{index}. {0}")
    public static Collection<Object[]> parameters()
    {
        return asList( new Object[][] {
                { "Full reactor", emptyList(), emptyList(), "", "", asList( MODULE_A, MODULE_B, MODULE_C ) },
                { "Selected project", singletonList( MODULE_B ), emptyList(), "", "", singletonList( MODULE_B )},
                { "Excluded project", emptyList(), singletonList( MODULE_B ), "", "", asList( MODULE_A, MODULE_C )},
                { "Resuming from project", emptyList(), emptyList(), MODULE_B, "", asList( MODULE_B, MODULE_C )},
                { "Selected project with also make dependencies", singletonList( MODULE_C ), emptyList(), "", REACTOR_MAKE_UPSTREAM, asList( MODULE_B, MODULE_C )},
                { "Selected project with also make dependents", singletonList( MODULE_B ), emptyList(), "", REACTOR_MAKE_DOWNSTREAM, asList( MODULE_B, MODULE_C )},
                { "Resuming from project with also make dependencies", emptyList(), emptyList(), MODULE_C, REACTOR_MAKE_UPSTREAM, asList( MODULE_B, MODULE_C )},
                { "Selected project with resume from an also make dependency (MNG-4960 IT#1)", singletonList( MODULE_C ), emptyList(), MODULE_B, REACTOR_MAKE_UPSTREAM, asList( MODULE_B, MODULE_C )},
                { "Selected project with resume from an also make dependent (MNG-4960 IT#2)", singletonList( MODULE_B ), emptyList(), MODULE_C, REACTOR_MAKE_DOWNSTREAM, singletonList( MODULE_C )}
        } );
    }

    public DefaultGraphBuilderTest( String description, List<String> selectedProjects, List<String> excludedProjects, String resumedFrom, String makeBehavior, List<String> expectedReactorProjects )
    {
        this.parameterDescription = description;
        this.parameterSelectedProjects = selectedProjects;
        this.parameterExcludedProjects = excludedProjects;
        this.parameterResumeFrom = resumedFrom;
        this.parameterMakeBehavior = makeBehavior;
        this.parameterExpectedReactorProjects = expectedReactorProjects;
    }

    @Test
    public void testGetReactorProjects()
    {
        // Given
        List<String> selectedProjects = parameterSelectedProjects.stream().map( p -> ":" + p ).collect( Collectors.toList() );
        List<String> excludedProjects = parameterExcludedProjects.stream().map( p -> ":" + p ).collect( Collectors.toList() );

        when( mavenExecutionRequest.getSelectedProjects() ).thenReturn( selectedProjects );
        when( mavenExecutionRequest.getExcludedProjects() ).thenReturn( excludedProjects );
        when( mavenExecutionRequest.getMakeBehavior() ).thenReturn( parameterMakeBehavior );
        if ( StringUtils.isNotEmpty( parameterResumeFrom ) )
        {
            when( mavenExecutionRequest.getResumeFrom() ).thenReturn( ":" + parameterResumeFrom );
        }

        // When
        Result<ProjectDependencyGraph> result = graphBuilder.build( session );

        // Then
        List<MavenProject> actualReactorProjects = result.get().getSortedProjects();
        List<MavenProject> expectedReactorProjects = parameterExpectedReactorProjects.stream()
                .map( artifactIdProjectMap::get )
                .collect( Collectors.toList());
        assertEquals( parameterDescription, expectedReactorProjects, actualReactorProjects );
    }

    @Before
    public void before() throws Exception
    {
        MockitoAnnotations.initMocks( this );

        ProjectBuildingRequest projectBuildingRequest = mock( ProjectBuildingRequest.class );
        ProjectBuildingResult projectBuildingResult1 = mock( ProjectBuildingResult.class );
        ProjectBuildingResult projectBuildingResult2 = mock( ProjectBuildingResult.class );
        ProjectBuildingResult projectBuildingResult3 = mock( ProjectBuildingResult.class );
        MavenProject projectModuleA = getMavenProject( "module-a" );
        MavenProject projectModuleB = getMavenProject( "module-b" );
        MavenProject projectModuleC = getMavenProject( "module-c" );
        projectModuleC.setDependencies( singletonList( toDependency( projectModuleB) ) );

        when( session.getRequest() ).thenReturn( mavenExecutionRequest );
        when( session.getProjects() ).thenReturn( null ); // needed, otherwise it will be an empty list by default

        when( mavenExecutionRequest.getProjectBuildingRequest() ).thenReturn( projectBuildingRequest );
        when( mavenExecutionRequest.getPom() ).thenReturn( new File( "/tmp/unit-test" ) );

        when( projectBuildingResult1.getProject() ).thenReturn( projectModuleA );
        when( projectBuildingResult2.getProject() ).thenReturn( projectModuleB );
        when( projectBuildingResult3.getProject() ).thenReturn( projectModuleC );

        when( projectBuilder.build( anyList(), anyBoolean(), any( ProjectBuildingRequest.class ) ) ).thenReturn( asList( projectBuildingResult1, projectBuildingResult2, projectBuildingResult3 ) );

        artifactIdProjectMap = ImmutableMap.of(
                MODULE_A, projectModuleA,
                MODULE_B, projectModuleB,
                MODULE_C, projectModuleC
        );
    }

    private MavenProject getMavenProject( String artifactId )
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId( "unittest" );
        mavenProject.setArtifactId( artifactId );
        mavenProject.setVersion( "1.0" );
        return mavenProject;
    }

    private Dependency toDependency( MavenProject mavenProject )
    {
        Dependency dependency = new Dependency();
        dependency.setGroupId( mavenProject.getGroupId() );
        dependency.setArtifactId( mavenProject.getArtifactId() );
        dependency.setVersion( mavenProject.getVersion() );
        return dependency;
    }
}