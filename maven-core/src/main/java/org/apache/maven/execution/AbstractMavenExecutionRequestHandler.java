package org.apache.maven.execution;

import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.lifecycle.session.MavenSessionPhaseManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractMavenExecutionRequestHandler
    extends AbstractLogEnabled
    implements MavenExecutionRequestHandler, Contextualizable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    protected MavenProjectBuilder projectBuilder;

    protected PluginManager pluginManager;

    protected PlexusContainer container;

    protected MavenSessionPhaseManager lifecycleManager;

    protected I18N i18n;

    // ----------------------------------------------------------------------
    // Methods used by all execution request handlers
    // ----------------------------------------------------------------------

    //!! We should probably have the execution request handler create the session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request, MavenProject project )
    {
        MavenSession session = new MavenSession( container,
                                                 pluginManager,
                                                 project,
                                                 request.getLocalRepository(),
                                                 request.getGoals() );

        return session;
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    public void contextualize( Context context ) throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
