package org.apache.maven.execution.project;

import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.lifecycle.session.MavenSessionPhaseManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.project.MavenProjectExecutionRequest;
import org.apache.maven.execution.AbstractMavenExecutionRequestHandler;
import org.apache.maven.execution.MavenExecutionRequestHandler;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.codehaus.plexus.ArtifactEnabledContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.util.Date;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenProjectExecutionRequestHandler
    extends AbstractMavenExecutionRequestHandler
    implements MavenExecutionRequestHandler, Contextualizable
{
    private boolean logResults = true;

    protected MavenProjectBuilder projectBuilder;

    protected PluginManager pluginManager;

    protected PlexusContainer container;

    protected MavenSessionPhaseManager lifecycleManager;

    protected I18N i18n;

    public void handle( MavenExecutionRequest request, MavenExecutionResponse response )
        throws Exception
    {
        MavenProject project = getProject( ((MavenProjectExecutionRequest) request).getPom(), request.getLocalRepository() );

        Date fullStop;

        Date fullStart = new Date();

        pluginManager.setLocalRepository( request.getLocalRepository() );

        MavenSession session = new MavenSession( container,
                                                 pluginManager,
                                                 project,
                                                 request.getLocalRepository(),
                                                 request.getGoals() );

        try
        {
            response = lifecycleManager.execute( session );
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

            return;
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
    }

    protected void stats( Date fullStart, Date fullStop )
    {
        long fullDiff = fullStop.getTime() - fullStart.getTime();

        getLogger().info( "Total time: " + formatTime( fullDiff ) );

        getLogger().info( "Finished at: " + fullStop );

        final long mb = 1024 * 1024;

        System.gc();

        Runtime r = Runtime.getRuntime();

        getLogger().info( "Final Memory: " + ( ( r.totalMemory() - r.freeMemory() ) / mb ) + "M/" + ( r.totalMemory() / mb ) + "M" );
    }

    protected void line()
    {
        getLogger().info( "----------------------------------------------------------------------------" );
    }

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
    // Project building
    // ----------------------------------------------------------------------

    public MavenProject getProject( File pom, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        if ( pom.exists() )
        {
            if ( pom.length() == 0 )
            {
                throw new ProjectBuildingException( i18n.format( "empty.descriptor.error", pom.getName() ) );
            }
        }

        return projectBuilder.build( pom, localRepository );
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    public void contextualize( Context context ) throws ContextException
    {
        container = (ArtifactEnabledContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
