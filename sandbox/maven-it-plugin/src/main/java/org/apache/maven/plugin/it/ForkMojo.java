package org.apache.maven.plugin.it;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @goal fork
 *
 * @execute phase="package"
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public class ForkMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${component.org.apache.maven.lifecycle.LifecycleExecutor}"
     */
    private LifecycleExecutor lifecycleExecutor;

    /**
     * @parameter expression="${session}"
     */
    private MavenSession session;

    /**
     * @parameter expression="${settings}"
     */
    private Settings settings;

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.basedir}/src/it/"
     */
    private File integrationTestsDirectory;


    public void execute()
        throws MojoExecutionException
    {
        try
        {
            buildProjects();
        }
        catch ( CycleDetectedException e )
        {
            throw new MojoExecutionException( "Error building projects", e );
        }
        catch ( LifecycleExecutionException e )
        {
            throw new MojoExecutionException( "Error building projects", e );
        }
    }

    private void buildProjects()
        throws CycleDetectedException, LifecycleExecutionException, MojoExecutionException
    {
        List projects = collectProjects();

        projects.add( project );

        ReactorManager rm = new ReactorManager( projects );

        rm.setFailureBehavior( ReactorManager.FAIL_AT_END );

        rm.blackList( project );

        List goals = Collections.singletonList( "package" );

        MavenSession forkedSession = new MavenSession(
            session.getContainer(), session.getSettings(),
            session.getLocalRepository(),
            session.getEventDispatcher(),
            rm, goals, integrationTestsDirectory.toString()
        );

        MavenExecutionResponse response = lifecycleExecutor.execute( forkedSession,
            rm, forkedSession.getEventDispatcher()
        );

        if ( response.isExecutionFailure() )
        {
            getLog().error( "Integration test failed", response.getException() );
            throw new MojoExecutionException( "Integration test failed" );
        }
    }

    private List collectProjects()
        throws MojoExecutionException
    {
        List projects = new ArrayList();

        MavenProjectBuilder projectBuilder;

        try
        {
            projectBuilder = (MavenProjectBuilder)
                session.getContainer().lookup( MavenProjectBuilder.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoExecutionException( "Cannot get a MavenProjectBuilder", e );
        }

        List poms = listITPoms();

        for ( Iterator i = poms.iterator(); i.hasNext(); )
        {
            File pom = (File) i.next();

            try
            {
                MavenProject p = projectBuilder.build(
                    pom, session.getLocalRepository(),
                    new DefaultProfileManager( session.getContainer() ) );

                getLog().debug( "Adding project " + p.getId() );

                projects.add( p );

            }
            catch (ProjectBuildingException e)
            {
                throw new MojoExecutionException( "Error loading " + pom, e );
            }
        }

        return projects;
    }

    private List listITPoms()
    {
        List poms = new ArrayList();

        File [] children = integrationTestsDirectory.listFiles();

        for ( int i = 0; i < children.length; i++ )
        {
            if ( children[i].isDirectory() )
            {
                File pomFile = new File( children[i], "pom.xml" );

                if ( pomFile.exists() && pomFile.isFile() )
                {
                    poms.add( pomFile );
                }
            }
        }

        return poms;
    }
}
