package org.apache.maven.lifecycle;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultLifecycleExecutor
    implements LifecycleExecutor, Initializable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private ArtifactResolver artifactResolver;

    private MavenProjectBuilder projectBuilder;

    private PluginManager pluginManager;

    private List phases;

    private Map phaseMap;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute a list of tasks. Each task may be a phase in the lifecycle
     * or the execution of a mojo.
     *
     * @param tasks
     * @param session
     * @throws LifecycleExecutionException
     */
    public void execute( List tasks, MavenSession session )
        throws LifecycleExecutionException
    {
        for ( Iterator i = tasks.iterator(); i.hasNext(); )
        {
            String task = (String) i.next();

            if ( phaseMap.containsKey( task ) )
            {
                executePhase( task, session );
            }
            else
            {
                executeMojo( task, session );
            }
        }
    }

    protected void executePhase( String phase, MavenSession session )
        throws LifecycleExecutionException
    {
        resolveTransitiveDependencies( session );

        downloadDependencies( session );

        System.out.println( "executing phase = " + phase );

        int i = phases.indexOf( phaseMap.get( phase ) );

        for ( int j = 0; j <= i; j++ )
        {
            Phase p = (Phase) phases.get( j );

            if ( p.getGoal() != null )
            {
                try
                {
                    pluginManager.executeMojo( session, p.getGoal() );
                }
                catch ( GoalExecutionException e )
                {
                    throw new LifecycleExecutionException( "Problem executing " + p.getGoal(), e );
                }
            }
        }
    }

    protected void executeMojo( String id, MavenSession session )
        throws LifecycleExecutionException
    {
        // ----------------------------------------------------------------------
        // We have something of the form <pluginId>:<mojoId>, so this might be
        // something like:
        //
        // clean:clean
        // idea:idea
        // archetype:create
        // ----------------------------------------------------------------------

        try
        {
            pluginManager.verifyPluginForGoal( id, session );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( id );

        if ( mojoDescriptor.requiresDependencyResolution() )
        {
            resolveTransitiveDependencies( session );

            downloadDependencies( session );
        }

        try
        {
            pluginManager.executeMojo( session, id );
        }
        catch ( GoalExecutionException e )
        {
            throw new LifecycleExecutionException( "Problem executing " + id, e );
        }
    }

    // ----------------------------------------------------------------------
    // Artifact resolution
    // ----------------------------------------------------------------------

    private void resolveTransitiveDependencies( MavenSession context )
        throws LifecycleExecutionException
    {
        MavenProject project = context.getProject();

        try
        {
            MavenMetadataSource sourceReader = new MavenMetadataSource( artifactResolver, projectBuilder );

            ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(),
                                                                                    context.getRemoteRepositories(),
                                                                                    context.getLocalRepository(),
                                                                                    sourceReader );

            project.getArtifacts().addAll( result.getArtifacts().values() );

        }
        catch ( Exception e )
        {
            throw new LifecycleExecutionException( "Error resolving transitive dependencies.", e );
        }
    }

    // ----------------------------------------------------------------------
    // Artifact downloading
    // ----------------------------------------------------------------------

    public void downloadDependencies( MavenSession context )
        throws LifecycleExecutionException
    {
        try
        {
            for ( Iterator it = context.getProject().getArtifacts().iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                artifactResolver.resolve( artifact,
                                          context.getRemoteRepositories(),
                                          context.getLocalRepository() );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new LifecycleExecutionException( "Can't resolve artifact: ", e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public List getPhases()
    {
        return phases;
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    public void initialize()
        throws Exception
    {
        phaseMap = new HashMap();

        for ( Iterator i = phases.iterator(); i.hasNext(); )
        {
            Phase p = (Phase) i.next();

            phaseMap.put( p.getId(), p );
        }
    }
}
