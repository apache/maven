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
import org.apache.maven.model.Parent;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM;
import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
import static org.apache.maven.graph.DefaultGraphBuilderTest.ScenarioBuilder.scenario;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( Parameterized.class )
public class DefaultGraphBuilderTest
{
    private static final String PARENT_MODULE = "module-parent"; // parent of all modules below
    private static final String INDEPENDENT_MODULE = "module-independent";
    private static final String MODULE_A = "module-a";
    private static final String MODULE_B = "module-b"; // depends on module-a
    private static final String MODULE_C = "module-c"; // depends on module-b

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
    private final String parameterDescription;
    private final List<String> parameterSelectedProjects;
    private final List<String> parameterExcludedProjects;
    private final String parameterResumeFrom;
    private final String parameterMakeBehavior;
    private final List<String> parameterExpectedResult;
    private final File parameterRequestedPom;

    @Parameters(name = "{index}. {0}")
    public static Collection<Object[]> parameters()
    {
        return asList(
                scenario( "Full reactor" )
                        .expectResult( PARENT_MODULE, INDEPENDENT_MODULE, MODULE_A, MODULE_B, MODULE_C ),
                scenario( "Selected project" )
                        .selectedProjects( MODULE_B )
                        .expectResult( MODULE_B ),
                scenario( "Excluded project" )
                        .excludedProjects( MODULE_B )
                        .expectResult( PARENT_MODULE, INDEPENDENT_MODULE, MODULE_A, MODULE_C ),
                scenario( "Resuming from project" )
                        .resumeFrom( MODULE_B )
                        .expectResult( MODULE_B, MODULE_C ),
                scenario( "Selected project with also make dependencies" )
                        .selectedProjects( MODULE_C )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_A, MODULE_B, MODULE_C ),
                scenario( "Selected project with also make dependents" )
                        .selectedProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_DOWNSTREAM )
                        .expectResult( MODULE_B, MODULE_C ),
                scenario( "Resuming from project with also make dependencies" )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .resumeFrom( MODULE_C )
                        .expectResult( PARENT_MODULE, MODULE_A, MODULE_B, MODULE_C ),
                scenario( "Selected project with resume from and also make dependency (MNG-4960 IT#1)" )
                        .selectedProjects( MODULE_C )
                        .resumeFrom( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_A, MODULE_B, MODULE_C ),
                scenario( "Selected project with resume from and also make dependent (MNG-4960 IT#2)" )
                        .selectedProjects( MODULE_B )
                        .resumeFrom( MODULE_C )
                        .makeBehavior( REACTOR_MAKE_DOWNSTREAM )
                        .expectResult( MODULE_C ),
                scenario( "Excluding an also make dependency from selectedProject does take its transitive dependency" )
                        .selectedProjects( MODULE_C )
                        .excludedProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_A, MODULE_C ),
                scenario( "Excluding an also make dependency from resumeFrom does take its transitive dependency" )
                        .resumeFrom( MODULE_C )
                        .excludedProjects( MODULE_B )
                        .makeBehavior( REACTOR_MAKE_UPSTREAM )
                        .expectResult( PARENT_MODULE, MODULE_A, MODULE_C ),
                scenario( "Resume from exclude project downstream" )
                        .resumeFrom( MODULE_A )
                        .excludedProjects( MODULE_B )
                        .expectResult( MODULE_A, MODULE_C ),
                scenario( "Exclude the project we are resuming from (as proposed in MNG-6676)" )
                        .resumeFrom( MODULE_B )
                        .excludedProjects( MODULE_B )
                        .expectResult( MODULE_C ),
                scenario( "Selected projects in wrong order are resumed correctly in order" )
                        .selectedProjects( MODULE_C, MODULE_B, MODULE_A )
                        .resumeFrom( MODULE_B )
                        .expectResult( MODULE_B, MODULE_C ),
                scenario( "Duplicate projects are filtered out" )
                        .selectedProjects( MODULE_A, MODULE_A )
                        .expectResult( MODULE_A )
        );
    }

    public DefaultGraphBuilderTest( String description, List<String> selectedProjects, List<String> excludedProjects, String resumedFrom, String makeBehavior, List<String> expectedReactorProjects, File requestedPom )
    {
        this.parameterDescription = description;
        this.parameterSelectedProjects = selectedProjects;
        this.parameterExcludedProjects = excludedProjects;
        this.parameterResumeFrom = resumedFrom;
        this.parameterMakeBehavior = makeBehavior;
        this.parameterExpectedResult = expectedReactorProjects;
        this.parameterRequestedPom = requestedPom;
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
        ProjectBuildingResult projectBuildingResult4 = mock( ProjectBuildingResult.class );
        ProjectBuildingResult projectBuildingResult5 = mock( ProjectBuildingResult.class );
        MavenProject projectParent = getMavenProject( PARENT_MODULE );
        MavenProject projectIndependentModule = getMavenProject( INDEPENDENT_MODULE );
        MavenProject projectModuleA = getMavenProject( MODULE_A, projectParent );
        MavenProject projectModuleB = getMavenProject( MODULE_B, projectParent );
        MavenProject projectModuleC = getMavenProject( MODULE_C, projectParent );

        projectModuleB.setDependencies( singletonList( toDependency( projectModuleA) ) );
        projectModuleC.setDependencies( singletonList( toDependency( projectModuleB) ) );
        projectParent.setModules( Arrays.asList( INDEPENDENT_MODULE, MODULE_A, MODULE_B, MODULE_C ) );

        when( session.getRequest() ).thenReturn( mavenExecutionRequest );
        when( session.getProjects() ).thenReturn( null ); // needed, otherwise it will be an empty list by default

        when( mavenExecutionRequest.getProjectBuildingRequest() ).thenReturn( projectBuildingRequest );

        when( projectBuildingResult1.getProject() ).thenReturn( projectParent );
        when( projectBuildingResult2.getProject() ).thenReturn( projectIndependentModule );
        when( projectBuildingResult3.getProject() ).thenReturn( projectModuleA );
        when( projectBuildingResult4.getProject() ).thenReturn( projectModuleB );
        when( projectBuildingResult5.getProject() ).thenReturn( projectModuleC );

        when( projectBuilder.build( anyList(), anyBoolean(), any( ProjectBuildingRequest.class ) ) )
                .thenReturn( asList( projectBuildingResult1, projectBuildingResult2, projectBuildingResult3, projectBuildingResult4, projectBuildingResult5 ) );

        artifactIdProjectMap = ImmutableMap.of(
                PARENT_MODULE, projectParent,
                INDEPENDENT_MODULE, projectIndependentModule,
                MODULE_A, projectModuleA,
                MODULE_B, projectModuleB,
                MODULE_C, projectModuleC
        );
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
            this.requestedPom = new File( requestedPom );
            return this;
        }

        public Object[] expectResult( String... expectedReactorProjects )
        {
            return new Object[] {
                    description, selectedProjects, excludedProjects, resumeFrom, makeBehavior, asList( expectedReactorProjects ), requestedPom
            };
        }
    }
}