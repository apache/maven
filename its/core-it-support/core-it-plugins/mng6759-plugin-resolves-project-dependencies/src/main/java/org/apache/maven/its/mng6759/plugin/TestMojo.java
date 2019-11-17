package org.apache.maven.its.mng6759.plugin;

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

import java.util.Arrays;
import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Resolves the project dependency tree
 */
@Mojo( name = "resolve", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true )
public class TestMojo
    extends AbstractMojo
{

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession mavenSession;

    @Component( hint = "default" )
    protected ProjectDependenciesResolver dependencyResolver;

    public void execute()
        throws MojoExecutionException
    {

        try
        {
            dependencyResolver.resolve( project, Arrays.asList( "test" ), mavenSession );
        }
        catch ( ArtifactResolutionException | ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

    }
}
