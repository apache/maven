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
import org.apache.maven.lifecycle.MavenGoalExecutionContext;
import org.apache.maven.lifecycle.MavenLifecycleManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultMaven
    extends AbstractLogEnabled
    implements Maven, Contextualizable
{
    private PlexusContainer container;

    private String mavenHome;

    private String localRepository;

    private boolean logResults = true;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private PluginManager pluginManager;

    private MavenLifecycleManager lifecycleManager;

    private MavenProjectBuilder projectBuilder;

    private I18N i18n;

    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    public ExecutionResponse execute( List goals )
        throws GoalNotFoundException
    {
        return execute( (MavenProject) null, goals );
    }

    public ExecutionResponse execute( File projectFile, List goals )
        throws ProjectBuildingException, GoalNotFoundException
    {
        return execute( getProject( projectFile ), goals );
    }

    public ExecutionResponse execute( MavenProject project, List goals )
        throws GoalNotFoundException
    {
        Date fullStop;

        Date fullStart = new Date();

        ExecutionResponse response = new ExecutionResponse();

        for ( Iterator iterator = goals.iterator(); iterator.hasNext(); )
        {
            String goal = (String) iterator.next();

            /*

            //!! This needs to be thrown later because we may need to download the plugin first

            if ( !getMojoDescriptors().containsKey( goal ) )
            {
                throw new GoalNotFoundException( goal );
            }
            */

            MavenGoalExecutionContext context;

            try
            {
                //!! we may not know anything about the plugin at this point.

                context = new MavenGoalExecutionContext( container,
                                                     project,
                                                     getMojoDescriptor( goal ),
                                                     getLocalRepository() );

                context.setGoalName( goal );

                lifecycleManager.execute( context );

                if ( context.isExecutionFailure() )
                {
                    response.setExecutionFailure( context.getMojoDescriptor().getId(), context.getFailureResponse() );

                    break;
                }
            }
            catch ( Exception e )
            {
                response.setException( e );

                if ( logResults )
                {
                    line();

                    getLogger().error( "BUILD ERROR" );

                    line();

                    getLogger().error( "Cause: ", e );

                    line();

                    stats( fullStart, new Date() );

                    line();
                }
            }
        }

        fullStop = new Date();

        if ( logResults )
        {
            if ( response.isExecutionFailure() )
            {
                line();

                getLogger().info( "BUILD FAILURE" );

                line();

                getLogger().info( "Reason: " + response.getFailureResponse().shortMessage() );

                line();

                getLogger().info( response.getFailureResponse().longMessage() );

                line();

                stats( fullStart, fullStop );

                line();
            }
            else
            {
                line();

                getLogger().info( "BUILD SUCCESSFUL" );

                line();

                stats( fullStart, fullStop );

                line();
            }
        }

        return response;
    }

    private void stats( Date fullStart, Date fullStop )
    {
        long fullDiff = fullStop.getTime() - fullStart.getTime();

        getLogger().info( "Total time: " + formatTime( fullDiff ) );

        getLogger().info( "Finished at: " + fullStop );

        final long mb = 1024 * 1024;

        System.gc();

        Runtime r = Runtime.getRuntime();

        getLogger().info( "Final Memory: " + ((r.totalMemory() - r.freeMemory()) / mb) + "M/" + (r.totalMemory() / mb) + "M");

    }

    private void line()
    {
        getLogger().info( "----------------------------------------------------------------------------" );
    }

    // ----------------------------------------------------------------------
    // Reactor execution
    // ----------------------------------------------------------------------

    public ExecutionResponse executeReactor( String goals, String includes, String excludes )
        throws ReactorException, GoalNotFoundException
    {
        List projects = new ArrayList();

        getLogger().info( "Starting the reactor..." );

        try
        {
            List files = FileUtils.getFiles( new File( System.getProperty( "user.dir" ) ), includes, excludes );

            for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
            {
                File f = (File) iterator.next();

                MavenProject project = projectBuilder.build( f, getLocalRepository() );

                projects.add( project );
            }

            projects = projectBuilder.getSortedProjects( projects );
        }
        catch ( Exception e )
        {
            throw new ReactorException( "Error processing projects for the reactor: ", e );
        }

        getLogger().info( "Our processing order:" );

        for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();

            getLogger().info( project.getName() );
        }

        List goalsList = Arrays.asList( StringUtils.split( goals, "," ) );

        ExecutionResponse response = null;

        for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();

            System.out.println( "\n\n\n" );

            line();

            getLogger().info( "Building " + project.getName() );

            line();

            response = execute( project, goalsList );

            if ( response.isExecutionFailure() )
            {
                break;
            }
        }

        return response;
    }

    // ----------------------------------------------------------------------
    // Goal descriptors
    // ----------------------------------------------------------------------

    public Map getMojoDescriptors()
    {
        return pluginManager.getMojoDescriptors();
    }

    public MojoDescriptor getMojoDescriptor( String goalId )
    {
        return pluginManager.getMojoDescriptor( goalId );
    }

    // ----------------------------------------------------------------------
    // Project building
    // ----------------------------------------------------------------------

    public MavenProject getProject( File project )
        throws ProjectBuildingException
    {
        if ( project.exists() )
        {
            if ( project.length() == 0 )
            {
                throw new ProjectBuildingException( i18n.format( "empty.descriptor.error", project.getName() ) );
            }
        }

        return projectBuilder.build( project, getLocalRepository() );
    }

    // ----------------------------------------------------------------------
    // Reactor
    // ----------------------------------------------------------------------

    public List getSortedProjects( List projects )
        throws Exception
    {
        return projectBuilder.getSortedProjects( projects );
    }

    // ----------------------------------------------------------------------
    // Maven home
    // ----------------------------------------------------------------------

    public void setMavenHome( String mavenHome )
    {
        this.mavenHome = mavenHome;
    }

    public String getMavenHome()
    {
        return mavenHome;
    }

    // ----------------------------------------------------------------------
    // Maven local repository
    // ----------------------------------------------------------------------

    public void setLocalRepository( String localRepository )
    {
        this.localRepository = localRepository;
    }

    private ArtifactRepository wagonLocalRepository;

    public ArtifactRepository getLocalRepository()
    {
        if ( wagonLocalRepository == null )
        {
            wagonLocalRepository = new ArtifactRepository( "local", "file://" + localRepository );
        }

        return wagonLocalRepository;
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected static String formatTime( long ms )
    {
        long secs = ms / 1000;

        long min = secs / 60;

        secs = secs % 60;

        if ( min > 0 )
        {
            return min + " minutes " + secs + " seconds";
        }
        else
        {
            return secs + " seconds";
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void booty()
        throws Exception
    {
        pluginManager.setLocalRepository( getLocalRepository() );
    }
}
