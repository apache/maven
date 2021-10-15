package org.apache.maven.project.collector;

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

import org.apache.maven.DefaultMaven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * Strategy to collect projects for building when the Maven invocation is not in a directory that contains a pom.xml.
 */
@Named( "PomlessCollectionStrategy" )
@Singleton
public class PomlessCollectionStrategy
    implements ProjectCollectionStrategy
{
    private final ProjectBuilder projectBuilder;

    @Inject
    public PomlessCollectionStrategy( ProjectBuilder projectBuilder )
    {
        this.projectBuilder = projectBuilder;
    }

    @Override
    public List<MavenProject> collectProjects( final MavenExecutionRequest request )
            throws ProjectBuildingException
    {
        ProjectBuildingRequest buildingRequest = request.getProjectBuildingRequest();
        ModelSource modelSource = new UrlModelSource( DefaultMaven.class.getResource( "project/standalone.xml" ) );
        MavenProject project = projectBuilder.build( modelSource,  buildingRequest ).getProject();
        project.setExecutionRoot( true );
        request.setProjectPresent( false );

        return Arrays.asList( project );
    }
}
