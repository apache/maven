package org.apache.maven;

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
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

import org.codehaus.plexus.ArtifactEnabledPlexusTestCase;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenTestCase
    extends ArtifactEnabledPlexusTestCase
{
    protected PluginManager pluginManager;

    protected MavenProjectBuilder projectBuilder;

    protected String testRepoUrl;

    private File mavenHome = new File( getBasedir(), "target/maven.home" );

    private File mavenLocalHome = new File( getBasedir(), "target/maven.home.local" );;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File testRepoLocation = new File( "target/repo" );

        if ( !testRepoLocation.exists() )
        {
            testRepoLocation.mkdirs();
        }

        testRepoUrl = testRepoLocation.toURL().toExternalForm();

        testRepoUrl = testRepoUrl.substring( 0, testRepoUrl.length() - 1 );

        pluginManager = (PluginManager) lookup( PluginManager.ROLE );

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
    }

    // ----------------------------------------------------------------------
    // Customizing the PlexusTestCase
    // ----------------------------------------------------------------------

    protected void customizeContext()
        throws Exception
    {
        MavenTestUtils.customizeContext( getContainer(), getTestFile( "" ), mavenHome, mavenLocalHome );
    }

    // ----------------------------------------------------------------------
    // 
    // ----------------------------------------------------------------------

    protected String getTestRepoURL()
    {
        return testRepoUrl;
    }

    protected File getMavenHome()
    {
        return mavenHome;
    }

    protected File getMavenLocalHome()
    {
        return mavenLocalHome;
    }

    protected MavenGoalExecutionContext createGoalExecutionContext()
        throws Exception
    {
        return createGoalExecutionContext( null, null );
    }

    protected MavenGoalExecutionContext createGoalExecutionContext( File pom )
        throws Exception
    {
        return createGoalExecutionContext( pom, null );
    }

    protected MavenGoalExecutionContext createGoalExecutionContext( String goal )
        throws Exception
    {
        return createGoalExecutionContext( null, goal );
    }

    protected MavenGoalExecutionContext createGoalExecutionContext( File pom, String goal )
        throws Exception
    {
        ArtifactRepository localRepository = new ArtifactRepository( "local", testRepoUrl );

        MavenProject project;

        if ( pom != null )
        {
            project = projectBuilder.build( mavenLocalHome, pom );
        }
        else
        {
            File f = getTestFile( "target/test-classes/pom.xml" );

            project = projectBuilder.build( mavenLocalHome, f );
        }

        return createGoalExecutionContext( project, localRepository, goal );
    }

    protected MavenGoalExecutionContext createGoalExecutionContext( MavenProject project,
        ArtifactRepository localRepository, String goal )
    {
        List goals = new ArrayList();

        MavenSession session = new MavenSession( getContainer(), pluginManager, project, localRepository, goals );

        pluginManager.setLocalRepository( localRepository );

        MavenGoalExecutionContext context = new MavenGoalExecutionContext( session, goal );

        return context;
    }
}
