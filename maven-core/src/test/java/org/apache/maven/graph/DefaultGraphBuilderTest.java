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

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.BuildResumptionDataRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectActivation;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM;
import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
import static org.apache.maven.graph.DefaultGraphBuilderTest.ScenarioBuilder.scenario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultGraphBuilderTest
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
    private static final String GROUP_ID = "unittest";
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
                        .activeRequiredProjects( MODULE_B )
                        .expectResult( MODULE_B ),
                scenario( "Selected aggregator project (including child modules)" )
                        .activeRequiredProjects( MODULE_C )
                        .expectResult( MODULE_C, MODULE_C_1, MODULE_C_2 ),
                scenario( "Selected aggregator project with non-recursive" )
                        .activeRequiredProjects( MODULE_C )
                        .nonRecursive()
                        .expectResult( MODULE_C ),
                scenario( "Selected optional project" )
                        .activeOptionalProjects( MODULE_B )
                        .expectResult( MODULE_B ),
                scenario( "Selected missing optional project" )
                        .activeOptionalProjects( "non-existing-module" )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected missing optional and required project" )
                        .activeOptionalProjects( "non-existing-module" )
                        .activeRequiredProjects( MODULE_B )
                        .expectResult( MODULE_B ),
                scenario( "Excluded project" )
                        .inactiveRequiredProjects( MODULE_B )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Excluded optional project" )
                        .inactiveOptionalProjects( MODULE_B )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Excluded missing optional project" )
                        .inactiveOptionalProjects( "non-existing-module" )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Excluded missing optional and required project" )
                        .inactiveOptionalProjects( "non-existing-module" )
                        .inactiveRequiredProjects( MODULE_B )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_C_1, MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Excluded aggregator project with non-recursive" )
                        .inactiveRequiredProjects( MODULE_C )
                        .nonRecursive()
                        .expectResult( PARENT_MODULE, MODULE_C_1, MODULE_A, MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected and excluded same project" )
                        .activeRequiredProjects( MODULE_A )
                        .inactiveRequiredProjects( MODULE_A )
                        .expectResult( MavenExecutionException.class, "empty reactor" ),
                scenario( "Excluded aggregator, but selected child" )
                        .activeRequiredProjects( MODULE_C_1 )
                        .inactiveRequiredProjects( MODULE_C )
                        .expectResult( MavenExecutionException.class, "empty reactor" ),
                scenario( "Project selected with different selector resolves to same project" )
                        .activeRequiredProjects( GROUP_ID + ":" + MODULE_A )
                        .inactiveRequiredProjects( MODULE_A )
                        .expectResult( MavenExecutionException.class, "empty reactor" ),
                scenario( "Selected and excluded same project, but also selected another project" )
                        .activeRequiredProjects( MODULE_A, MODULE_B )
                        .inactiveRequiredProjects( MODULE_A )
                        .expectResult( MODULE_B ),
                scenario( "Selected missing project as required and as optional" )
                        .activeRequiredProjects( "non-existing-module" )
                        .activeOptionalProjects( "non-existing-module" )
                        .expectResult( MavenExecutionException.class, "not find the selected project" ),
                scenario( "Resuming from project" )
                        .resumeFrom( MODULE_B )
                        .expectResult( MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected project with also make dependencies" )
                        .activeRequiredProjects( MODULE_C_2 )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_B, MODULE_C_2 ),
                scenario( "Selected project with also make dependents" )
                        .activeRequiredProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_DOWNSTREAM )
                        .expectResult( MODULE_B, MODULE_C_2 ),
                scenario( "Resuming from project with also make dependencies" )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .resumeFrom( MODULE_C_2 )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_B, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected project with resume from and also make dependency (MNG-4960 IT#1)" )
                        .activeRequiredProjects( MODULE_C_2 )
                        .resumeFrom( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_B, MODULE_C_2 ),
                scenario( "Selected project with resume from and also make dependent (MNG-4960 IT#2)" )
                        .activeRequiredProjects( MODULE_B )
                        .resumeFrom( MODULE_C_2 )
                        .makeBehavior( REACTOR_MAKE_DOWNSTREAM )
                        .expectResult( MODULE_C_2 ),
                scenario( "Excluding an also make dependency from selectedProject does take its transitive dependency" )
                        .activeRequiredProjects( MODULE_C_2 )
                        .inactiveRequiredProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_C_2 ),
                scenario( "Excluding a project also excludes its children" )
                        .inactiveRequiredProjects( MODULE_C )
                        .expectResult( PARENT_MODULE, MODULE_A, MODULE_B, INDEPENDENT_MODULE ),
                scenario( "Excluding an also make dependency from resumeFrom does take its transitive dependency" )
                        .resumeFrom( MODULE_C_2 )
                        .inactiveRequiredProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_C, MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Resume from exclude project downstream" )
                        .resumeFrom( MODULE_A )
                        .inactiveRequiredProjects( MODULE_B )
                        .expectResult( MODULE_A, MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Exclude the project we are resuming from (as proposed in MNG-6676)" )
                        .resumeFrom( MODULE_B )
                        .inactiveRequiredProjects( MODULE_B )
                        .expectResult( MODULE_C_2, INDEPENDENT_MODULE ),
                scenario( "Selected projects in wrong order are resumed correctly in order" )
                        .activeRequiredProjects( MODULE_C_2, MODULE_B, MODULE_A )
                        .resumeFrom( MODULE_B )
                        .expectResult( MODULE_B, MODULE_C_2 ),
                scenario( "Duplicate projects are filtered out" )
                        .activeRequiredProjects( MODULE_A, MODULE_A )
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

    interface ExpectedResult {

    }
    static class SelectedProjectsResult implements ExpectedResult {
        final List<String> projectNames;

        public SelectedProjectsResult( List<String> projectSelectors )
        {
            this.projectNames = projectSelectors;
        }
    }
    static class ExceptionThrown implements ExpectedResult {
        final Class<? extends Throwable> expected;
        final String partOfMessage;

        public ExceptionThrown( final Class<? extends Throwable> expected, final String partOfMessage )
        {
            this.expected = expected;
            this.partOfMessage = partOfMessage;
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testGetReactorProjects(
            String parameterDescription,
            List<String> parameterActiveRequiredProjects,
            List<String> parameterActiveOptionalProjects,
            List<String> parameterInactiveRequiredProjects,
            List<String> parameterInactiveOptionalProjects,
            String parameterResumeFrom,
            String parameterMakeBehavior,
            ExpectedResult parameterExpectedResult,
            File parameterRequestedPom,
            boolean parameterRecursive )
    {
        // Given
        ProjectActivation projectActivation = new ProjectActivation();
        parameterActiveRequiredProjects.forEach( projectActivation::activateRequiredProject );
        parameterActiveOptionalProjects.forEach( projectActivation::activateOptionalProject );
        parameterInactiveRequiredProjects.forEach( projectActivation::deactivateRequiredProject );
        parameterInactiveOptionalProjects.forEach( projectActivation::deactivateOptionalProject );

        when( mavenExecutionRequest.getProjectActivation() ).thenReturn( projectActivation );
        when( mavenExecutionRequest.getMakeBehavior() ).thenReturn( parameterMakeBehavior );
        when( mavenExecutionRequest.getPom() ).thenReturn( parameterRequestedPom );
        when( mavenExecutionRequest.isRecursive() ).thenReturn( parameterRecursive );
        if ( StringUtils.isNotEmpty( parameterResumeFrom ) )
        {
            when( mavenExecutionRequest.getResumeFrom() ).thenReturn( ":" + parameterResumeFrom );
        }

        // When
        Result<ProjectDependencyGraph> result = graphBuilder.build( session );

        // Then
        if ( parameterExpectedResult instanceof SelectedProjectsResult )
        {
            assertThat( result.hasErrors() ).withFailMessage( "Expected result not to have errors" ).isFalse();
            List<String> expectedProjectNames = ((SelectedProjectsResult) parameterExpectedResult).projectNames;
            List<MavenProject> actualReactorProjects = result.get().getSortedProjects();
            List<MavenProject> expectedReactorProjects = expectedProjectNames.stream()
                    .map( artifactIdProjectMap::get )
                    .collect( toList() );
            assertEquals( expectedReactorProjects, actualReactorProjects, parameterDescription );
        }
        else
        {
            assertThat( result.hasErrors() ).withFailMessage( "Expected result to have errors" ).isTrue();
            Class<? extends Throwable> expectedException = ((ExceptionThrown) parameterExpectedResult).expected;
            String partOfMessage = ((ExceptionThrown) parameterExpectedResult).partOfMessage;

            assertThat( result.getProblems() ).hasSize( 1 );
            result.getProblems().forEach( p ->
                assertThat( p.getException() ).isInstanceOf( expectedException ).hasMessageContaining( partOfMessage )
            );
        }
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
        mavenProject.setGroupId( GROUP_ID );
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
                .collect( toList() );
    }

    static class ScenarioBuilder
    {
        private String description;
        private List<String> activeRequiredProjects = emptyList();
        private List<String> activeOptionalProjects = emptyList();
        private List<String> inactiveRequiredProjects = emptyList();
        private List<String> inactiveOptionalProjects = emptyList();
        private String resumeFrom = "";
        private String makeBehavior = "";
        private File requestedPom = new File( PARENT_MODULE, "pom.xml" );
        private boolean recursive = true;

        private ScenarioBuilder() { }

        public static ScenarioBuilder scenario( String description )
        {
            ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
            scenarioBuilder.description = description;
            return scenarioBuilder;
        }

        public ScenarioBuilder activeRequiredProjects( String... activeRequiredProjects )
        {
            this.activeRequiredProjects = prependWithColonIfNeeded( activeRequiredProjects );
            return this;
        }

        public ScenarioBuilder activeOptionalProjects( String... activeOptionalProjects )
        {
            this.activeOptionalProjects = prependWithColonIfNeeded( activeOptionalProjects );
            return this;
        }

        public ScenarioBuilder inactiveRequiredProjects( String... inactiveRequiredProjects )
        {
            this.inactiveRequiredProjects = prependWithColonIfNeeded( inactiveRequiredProjects );
            return this;
        }

        public ScenarioBuilder inactiveOptionalProjects( String... inactiveOptionalProjects )
        {
            this.inactiveOptionalProjects = prependWithColonIfNeeded( inactiveOptionalProjects );
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

        public ScenarioBuilder nonRecursive()
        {
            this.recursive = false;
            return this;
        }

        public Arguments expectResult( String... expectedReactorProjects )
        {
            ExpectedResult expectedResult = new SelectedProjectsResult( asList( expectedReactorProjects ) );
            return createTestArguments( expectedResult );
        }

        public Arguments expectResult( Class<? extends Exception> expected, final String partOfMessage )
        {
            ExpectedResult expectedResult = new ExceptionThrown( expected, partOfMessage );
            return createTestArguments( expectedResult );
        }

        private Arguments createTestArguments( ExpectedResult expectedResult )
        {
            return Arguments.arguments( description, activeRequiredProjects, activeOptionalProjects,
                    inactiveRequiredProjects, inactiveOptionalProjects, resumeFrom, makeBehavior, expectedResult,
                    requestedPom, recursive );
        }

        private List<String> prependWithColonIfNeeded( String[] selectors )
        {
            return Arrays.stream( selectors )
                    .map( this::prependWithColonIfNeeded )
                    .collect( toList() );
        }

        private String prependWithColonIfNeeded( String selector ) {
            return selector.indexOf( ':' ) == -1 ? ":" + selector : selector;
        }
    }
}