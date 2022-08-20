package org.apache.maven.its.mng7529.plugin;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

/**
 * Attempts to resolve a single artifact from dependencies with the project dependency resolver,
 * and logs the results for the Verifier to look at.
 */
@Mojo( name = "resolve", requiresDependencyResolution = ResolutionScope.NONE )
public class ResolveMojo
    extends AbstractMojo
{
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession mavenSession;

    @Component
    private ProjectDependenciesResolver dependencyResolver;

    public void execute()
        throws MojoExecutionException
    {

        try
        {
            DefaultProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
                mavenSession.getProjectBuildingRequest() );
            buildingRequest.setRemoteRepositories( project.getRemoteArtifactRepositories() );

            DependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
            request.setMavenProject( project );
            request.setRepositorySession( buildingRequest.getRepositorySession() );

            DependencyResolutionResult result = dependencyResolver.resolve( request );

            getLog().info( "Resolution successful, resolved ok" );
        }
        catch ( Exception e )
        {
            getLog().error( "Resolution failed, could not resolve ranged dependency"
                + " (you hit MNG-7529)" );
        }
    }
}
