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

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class DefaultBuildResumptionDataRepositoryTest
{
    private final DefaultBuildResumptionDataRepository repository = new DefaultBuildResumptionDataRepository();

    @Test
    public void resumeFromPropertyGetsApplied()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        Properties properties = new Properties();
        properties.setProperty( "remainingProjects", ":module-a" );

        repository.applyResumptionProperties( request, properties );

        assertThat( request.getSelectedProjects(), is( asList( ":module-a" ) ) );
    }

    @Test
    public void resumeFromPropertyDoesNotOverrideExistingRequestParameters()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setResumeFrom( ":module-b" );
        Properties properties = new Properties();
        properties.setProperty( "remainingProjects", ":module-a" );

        repository.applyResumptionProperties( request, properties );

        assertThat( request.getResumeFrom(), is( ":module-b" ) );
    }

    @Test
    public void projectsFromPropertyGetsAddedToExistingRequestParameters()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        List<String> selectedProjects = new ArrayList<>();
        selectedProjects.add( ":module-a" );
        request.setSelectedProjects( selectedProjects );
        Properties properties = new Properties();
        properties.setProperty( "remainingProjects", ":module-b, :module-c" );

        repository.applyResumptionProperties( request, properties );

        assertThat( request.getSelectedProjects(), containsInAnyOrder( ":module-a", ":module-b", ":module-c" ) );
    }

    @Test
    public void selectedProjectsAreNotAddedWhenPropertyValueIsEmpty()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        Properties properties = new Properties();
        properties.setProperty( "remainingProjects", "" );

        repository.applyResumptionProperties( request, properties );

        assertThat( request.getSelectedProjects(), is( empty() ) );
    }

    @Test
    public void applyResumptionData_shouldLoadData()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        Build build = new Build();
        build.setDirectory( "src/test/resources/org/apache/maven/execution/" );
        MavenProject rootProject = new MavenProject();
        rootProject.setBuild( build );

        repository.applyResumptionData( request,  rootProject );

        assertThat( request.getSelectedProjects(), containsInAnyOrder( "example:module-c" ) );
    }
}
