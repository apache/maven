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

import org.apache.maven.execution.BuildResumptionDataRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.Result;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.locator.ModelLocator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.collector.DefaultProjectsSelector;
import org.apache.maven.project.collector.MultiModuleCollectionStrategy;
import org.apache.maven.project.collector.PomlessCollectionStrategy;
import org.apache.maven.project.collector.ProjectsSelector;
import org.apache.maven.project.collector.RequestPomCollectionStrategy;
import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM;
import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
import static org.apache.maven.graph.DefaultGraphBuilderTest.ScenarioBuilder.scenario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultGraphBuilderTest
{
    /*
    The multi-module structure in this project is displayed as follows:

    module-parent
    └─── module-independent     (without parent declaration)
         module-a
         module-b               (depends on module-a)
         module-c
         └─── module-c-1
              module-c-2        (depends on module-b)
     */
    private static final String PARENT_MODULE = "module-parent";
    private static final String INDEPENDENT_MODULE = "module-independent";
    private static final String MODULE_A = "module-a";
    private static final String MODULE_B = "module-b";
    private static final String MODULE_C = "module-c";
    private static final String MODULE_C_1 = "module-c-1";
    private static final String MODULE_C_2 = "module-c-2";

    private DefaultGraphBuilder graphBuilder;

    private final ProjectBuilder projectBuilder = mock( ProjectBuilder.class );
    private final MavenSession session = mock( MavenSession.class );
    private final MavenExecutionRequest mavenExecutionRequest = mock( MavenExecutionRequest.class );

    private final ProjectsSelector projectsSelector = new DefaultProjectsSelector( projectBuilder );

    // Not using mocks for these strategies - a mock would just copy the actual implementation.

    private final ModelLocator modelLocator = new DefaultModelLocator();
    private final PomlessCollectionStrategy pomlessCollectionStrategy = new PomlessCollectionStrategy( projectBuilder );
    private final MultiModuleCollectionStrategy multiModuleCollectionStrategy = new MultiModuleCollectionStrategy( modelLocator, projectsSelector );
    private final RequestPomCollectionStrategy requestPomCollectionStrategy = new RequestPomCollectionStrategy( projectsSelector );

    private Map<String, MavenProject> artifactIdProjectMap;

    public static Stream<Arguments> parameters()
    {
        return Stream.of(
                scenario( "Full reactor in order" )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected project" )
                        .selectedProjects( MODULE_B )
                        .expectResult( MODULE_B ),
                scenario( "Selected project (including child modules)" )
                        .selectedProjects( MODULE_C )
                        .expectResult( MODULE_C, MODULE_C_1, MODULE_C_2 ),
                scenario( "Excluded project" )
                        .excludedProjects( MODULE_B )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Resuming from project" )
                        .resumeFrom( MODULE_B )
                        .expectResult( MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected project with also make dependencies" )
                        .selectedProjects( MODULE_C_2 )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_B, MODULE_C_2 ),
                scenario( "Selected project with also make dependents" )
                        .selectedProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_DOWNSTREAM )
                        .expectResult( MODULE_B, MODULE_C_2 ),
                scenario( "Resuming from project with also make dependencies" )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .resumeFrom( MODULE_C_2 )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected project with resume from and also make dependency (MNG-4960 IT#1)" )
                        .selectedProjects( MODULE_C_2 )
                        .resumeFrom( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_B, MODULE_C_2 ),
                scenario( "Selected project with resume from and also make dependent (MNG-4960 IT#2)" )
                        .selectedProjects( MODULE_B )
                        .resumeFrom( MODULE_C_2 )
                        .makeBehavior( REACTOR_MAKE_DOWNSTREAM )
                        .expectResult( MODULE_C_2 ),
                scenario( "Excluding an also make dependency from selectedProject does take its transitive dependency" )
                        .selectedProjects( MODULE_C_2 )
                        .excludedProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_C_2 ),
                scenario( "Excluding an also make dependency from resumeFrom does take its transitive dependency" )
                        .resumeFrom( MODULE_C_2 )
                        .excludedProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Resume from exclude project downstream" )
                        .resumeFrom( MODULE_A )
                        .excludedProjects( MODULE_B )
                        .expectResult( MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Exclude the project we are resuming from (as proposed in MNG-6676)" )
                        .resumeFrom( MODULE_B )
                        .excludedProjects( MODULE_B )
                        .expectResult( MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected projects in wrong order are resumed correctly in order" )
                        .selectedProjects( MODULE_C_2, MODULE_B, MODULE_A )
                        .resumeFrom( MODULE_B )
                        .expectResult( MODULE_B, MODULE_C_2 ),
                scenario( "Duplicate projects are filtered out" )
                        .selectedProjects( MODULE_A, MODULE_A )
                        .expectResult( MODULE_A ),
                scenario( "Select reactor by specific pom" )
                        .requestedPom( MODULE_C )
                        .expectResult( MODULE_C, MODULE_C_1, MODULE_C_2 ),
                scenario( "Select reactor by specific pom with also make dependencies" )
                        .requestedPom( MODULE_C )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_B, MODULE_C_2 ),
                scenario( "Select reactor by specific pom with also make dependents" )
                        .requestedPom( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_DOWNSTREAM )
                        .expectResult( MODULE_B, MODULE_C_2 )
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testGetReactorProjects(
            String parameterDescription,
            List<String> parameterSelectedProjects,
            List<String> parameterExcludedProjects,
            String parameterResumeFrom,
            String parameterMakeBehavior,
            List<String> parameterExpectedResult,
            File parameterRequestedPom)
    {
        // Given
        List<String> selectedProjects = parameterSelectedProjects.stream().map( p -> ":" + p ).collect( Collectors.toList() );
        List<String> excludedProjects = parameterExcludedProjects.stream().map( p -> ":" + p ).collect( Collectors.toList() );

        when( mavenExecutionRequest.getSelectedProjects() ).thenReturn( selectedProjects );
        when( mavenExecutionRequest.getExcludedProjects() ).thenReturn( excludedProjects );
        when( mavenExecutionRequest.getMakeBehavior() ).thenReturn( parameterMakeBehavior );
        when( mavenExecutionRequest.getPom() ).thenReturn( parameterRequestedPom );
        if ( StringUtils.isNotEmpty( parameterResumeFrom ) )
        {
            when( mavenExecutionRequest.getResumeFrom() ).thenReturn( ":" + parameterResumeFrom );
        }

        // When
        Result<ProjectDependencyGraph> result = graphBuilder.build( session );

        // Then
        List<MavenProject> actualReactorProjects = result.get().getSortedProjects();
        List<MavenProject> expectedReactorProjects = parameterExpectedResult.stream()
                .map( artifactIdProjectMap::get )
                .collect( Collectors.toList());
        assertEquals( expectedReactorProjects, actualReactorProjects, parameterDescription );
    }

    @BeforeEach
    public void before() throws Exception
    {
        graphBuilder = new DefaultGraphBuilder(
                mock( BuildResumptionDataRepository.class ),
                pomlessCollectionStrategy,
                multiModuleCollectionStrategy,
                requestPomCollectionStrategy
        );

        // Create projects
        MavenProject projectParent = getMavenProject( PARENT_MODULE );
        MavenProject projectIndependentModule = getMavenProject( INDEPENDENT_MODULE );
        MavenProject projectModuleA = getMavenProject( MODULE_A, projectParent );
        MavenProject projectModuleB = getMavenProject( MODULE_B, projectParent );
        MavenProject projectModuleC = getMavenProject( MODULE_C, projectParent );
        MavenProject projectModuleC1 = getMavenProject( MODULE_C_1, projectModuleC );
        MavenProject projectModuleC2 = getMavenProject( MODULE_C_2, projectModuleC );

        artifactIdProjectMap = Stream.of( projectParent, projectIndependentModule, projectModuleA, projectModuleB, projectModuleC, projectModuleC1, projectModuleC2 )
                .collect( Collectors.toMap( MavenProject::getArtifactId, identity() ) );

        // Set dependencies and modules
        projectModuleB.setDependencies( singletonList( toDependency( projectModuleA ) ) );
        projectModuleC2.setDependencies( singletonList( toDependency( projectModuleB ) ) );
        projectParent.setCollectedProjects( asList( projectIndependentModule, projectModuleA, projectModuleB, projectModuleC, projectModuleC1, projectModuleC2 ) );
        projectModuleC.setCollectedProjects( asList( projectModuleC1, projectModuleC2 ) );

        // Set up needed mocks
        when( session.getRequest() ).thenReturn( mavenExecutionRequest );
        when( session.getProjects() ).thenReturn( null ); // needed, otherwise it will be an empty list by default
        when( mavenExecutionRequest.getProjectBuildingRequest() ).thenReturn( mock( ProjectBuildingRequest.class ) );
        List<ProjectBuildingResult> projectBuildingResults = createProjectBuildingResultMocks( artifactIdProjectMap.values() );
        when( projectBuilder.build( anyList(), anyBoolean(), any( ProjectBuildingRequest.class ) ) ).thenReturn( projectBuildingResults );
    }

    private MavenProject getMavenProject( String artifactId, MavenProject parentProject )
    {
        MavenProject project = getMavenProject( artifactId );
        Parent parent = new Parent();
        parent.setGroupId( parentProject.getGroupId() );
        parent.setArtifactId( parentProject.getArtifactId() );
        project.getModel().setParent( parent );
        return project;
    }

    private MavenProject getMavenProject( String artifactId )
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId( "unittest" );
        mavenProject.setArtifactId( artifactId );
        mavenProject.setVersion( "1.0" );
        mavenProject.setPomFile( new File ( artifactId, "pom.xml" ) );
        mavenProject.setCollectedProjects( new ArrayList<>() );
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

    private List<ProjectBuildingResult> createProjectBuildingResultMocks( Collection<MavenProject> projects )
    {
        return projects.stream()
                .map( project -> {
                    ProjectBuildingResult result = mock( ProjectBuildingResult.class );
                    when( result.getProject() ).thenReturn( project );
                    return result;
                } )
                .collect( Collectors.toList() );
    }

    static class ScenarioBuilder
    {
        private String description;
        private List<String> selectedProjects = emptyList();
        private List<String> excludedProjects = emptyList();
        private String resumeFrom = "";
        private String makeBehavior = "";
        private File requestedPom = new File( PARENT_MODULE, "pom.xml" );

        private ScenarioBuilder() { }

        public static ScenarioBuilder scenario( String description )
        {
            ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
            scenarioBuilder.description = description;
            return scenarioBuilder;
        }

        public ScenarioBuilder selectedProjects( String... selectedProjects )
        {
            this.selectedProjects = asList( selectedProjects );
            return this;
        }

        public ScenarioBuilder excludedProjects( String... excludedProjects )
        {
            this.excludedProjects = asList( excludedProjects );
            return this;
        }

        public ScenarioBuilder resumeFrom( String resumeFrom )
        {
            this.resumeFrom = resumeFrom;
            return this;
        }

        public ScenarioBuilder makeBehavior( String makeBehavior )
        {
            this.makeBehavior = makeBehavior;
            return this;
        }

        public ScenarioBuilder requestedPom( String requestedPom )
        {
            this.requestedPom = new File( requestedPom, "pom.xml" );
            return this;
        }

        public Arguments expectResult( String... expectedReactorProjects )
        {
            return Arguments.arguments(
                    description, selectedProjects, excludedProjects, resumeFrom, makeBehavior, asList( expectedReactorProjects ), requestedPom
            );
        }
    }
}