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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenTestCase
    extends PlexusTestCase
{
    protected PluginManager pluginManager;

    protected MavenProjectBuilder projectBuilder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginManager = (PluginManager) lookup( PluginManager.ROLE );

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
    }

    protected void customizeContext()
        throws Exception
    {
        ClassWorld classWorld = new ClassWorld();

        ClassRealm rootClassRealm = classWorld.newRealm( "root", Thread.currentThread().getContextClassLoader() );

        getContainer().addContextValue( "rootClassRealm", rootClassRealm );

        getContainer().addContextValue( "maven.home", new File( getBasedir(), "target/maven.home" ).getPath() );

        getContainer().addContextValue( "maven.home.local", new File( getBasedir(), "target/maven.home.local" ).getPath() );
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
        ArtifactRepository localRepository = new ArtifactRepository( "local", "file://" );

        MavenProject project;

        if ( pom != null )
        {
            project = projectBuilder.build( pom, localRepository );
        }
        else
        {
            File f = new File( basedir, "target/test-classes/pom.xml" );

            project = projectBuilder.build( f, localRepository );
        }

        project.setProperty( "foo", "bar" );

        List goals = new ArrayList();

        MavenSession session = new MavenSession( getContainer(), pluginManager, project, localRepository, goals );

        MojoDescriptor descriptor;

        if ( goal != null )
        {
            descriptor = pluginManager.getMojoDescriptor( goal );
        }
        else
        {
            descriptor = new MojoDescriptor();
        }

        MavenGoalExecutionContext context = new MavenGoalExecutionContext( session, descriptor );

        return context;
    }
}
