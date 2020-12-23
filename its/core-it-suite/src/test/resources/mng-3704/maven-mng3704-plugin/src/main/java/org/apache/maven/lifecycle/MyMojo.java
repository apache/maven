package org.apache.maven.lifecycle;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.BuildFailureException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Tests that the lifecycle executor with the out-of-date component configuration (missing some new component
 * requirements) will still work...
 *
 * @goal run
 * @phase validate
 */
public class MyMojo
    extends AbstractMojo
{
    /**
     * @component role-hint="test"
     */
    private LifecycleExecutor lifecycleExecutor;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * @component
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        if ( !( lifecycleExecutor instanceof TestLifecycleExecutor ) )
        {
            throw new MojoExecutionException( "Wrong LifecycleExecutor was injected into the mojo." );
        }

        MavenProject testProject;
        try
        {
            testProject = projectBuilder.build( new File( project.getBasedir(), "test-project/pom.xml" ),
                                  new DefaultProjectBuilderConfiguration() );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoExecutionException( "Failed to build test project instance prior to lifecycle execution.", e );
        }

        List tasks = new ArrayList();
        tasks.add( "compile" );

        ReactorManager rm;
        try
        {
            rm = new ReactorManager( Collections.singletonList( testProject ) );
        }
        catch ( CycleDetectedException e )
        {
            throw new MojoExecutionException( "Failed to construct ReactorManager instance prior to lifecycle execution.", e );
        }
        catch ( DuplicateProjectException e )
        {
            throw new MojoExecutionException( "Failed to construct ReactorManager instance prior to lifecycle execution.", e );
        }

        MavenSession s =
            new MavenSession( session.getContainer(), session.getSettings(), session.getLocalRepository(),
                              new DefaultEventDispatcher(), rm, tasks, session.getExecutionRootDirectory(),
                              session.getExecutionProperties(), session.getStartTime() );

        try
        {
            lifecycleExecutor.execute( s, rm, s.getEventDispatcher() );
        }
        catch ( LifecycleExecutionException e )
        {
            throw new MojoExecutionException( "Unexpected error: " + e.getMessage(), e );
        }
        catch ( BuildFailureException e )
        {
            throw new MojoExecutionException( "Unexpected error: " + e.getMessage(), e );
        }
        catch ( NullPointerException e )
        {
            throw new MojoExecutionException(
                                              "Encountered a NullPointerException. Check that all component requirements have been met or managed through the initialization phase.",
                                              e );
        }
    }
}
