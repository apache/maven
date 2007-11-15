package org.apache.maven.compat.plugin;

import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.LifecycleUtils;
import org.apache.maven.lifecycle.NoSuchPhaseException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.extension.DefaultExtensionManager;
import org.apache.maven.execution.MavenRealmManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.DefaultMavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LogEnabled;

import java.io.IOException;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;

public privileged aspect Maven20xCompatAspect
{

    // GRAB Session as soon as it's constructed.
    private MavenSession session;

    // GRAB the request when it's passed into a method that returns a corresponding result.
    // NOTE: We'll use this in multiple places below...
    private MavenExecutionRequest request;

    // Grab this so we have a voice!
    private Logger logger;

    // used for injecting plexus-utils into extension and plugin artifact sets.
    private VersionRange vr = null;
    private Artifact plexusUtilsArtifact = null;


    private pointcut mavenEmbedderStop():
        execution( * org.apache.maven.embedder.MavenEmbedder.stop( .. ) );

    // TODO: There must be a more elegant way to release these resources than depending on MavenEmbedder.stop().
    after(): mavenEmbedderStop()
    {
        session = null;
        request = null;
        logger = null;
        vr = null;
        plexusUtilsArtifact = null;
    }


    // pointcut to avoid recursive matching on behavior injected by this aspect.
    private pointcut notHere(): !within( Maven20xCompatAspect );

    private pointcut sessionCreation( MavenSession session ):
        execution( public MavenSession+.new(..) )
        && this( session )
        && notHere();

    // capture the session instance.
    after( MavenSession session ): sessionCreation( session )
    {
        this.session = session;
    }

    // USE Session to compensate for old verifyPlugin(..) API.
    private pointcut verifyPlugin( Plugin plugin, MavenProject project, PluginManager manager ):
        execution( public PluginDescriptor PluginManager+.verifyPlugin( Plugin, MavenProject, Settings, ArtifactRepository ) )
        && args( plugin, project, .. )
        && target( manager )
        && notHere();

    // redirect the old verifyPlugin(..) call to the new one, using the captured session instance above.
    PluginDescriptor around( Plugin plugin,
                             MavenProject project,
                             PluginManager manager )
        throws ArtifactResolutionException, ArtifactNotFoundException, PluginNotFoundException,
        PluginVersionResolutionException, InvalidPluginException, PluginManagerException,
        PluginVersionNotFoundException:
            verifyPlugin( plugin, project, manager )
    {
        return manager.verifyPlugin( plugin, project, session );
    }

    // Re-Introduce old verifyPlugin(..) API.
    public PluginDescriptor PluginManager.verifyPlugin( Plugin plugin,
                                                        MavenProject project,
                                                        Settings settings,
                                                        ArtifactRepository localRepository )
    {
        // this will always be diverted, so no need to do anything.
        return null;
    }

    public PluginDescriptor DefaultPluginManager.verifyPlugin( Plugin plugin,
                                                        MavenProject project,
                                                        Settings settings,
                                                        ArtifactRepository localRepository )
    {
        // this will always be diverted, so no need to do anything.
        return null;
    }

    private pointcut getPluginDescriptorForPrefix( String prefix, PluginManager manager ):
        execution( public PluginDescriptor PluginManager+.getPluginDescriptorForPrefix( String ) )
        && args( prefix )
        && target( manager )
        && notHere();

    PluginDescriptor around( String prefix, PluginManager manager ): getPluginDescriptorForPrefix( prefix, manager )
    {
        // TODO: Implement Me!
        throw new UnsupportedOperationException( "This method has not yet been implemented in Maven's backward-compatibility library." );
    }

    public PluginDescriptor PluginManager.getPluginDescriptorForPrefix( String prefix )
    {
        return null;
    }

    // Intercept retrieval of artifact dependencies of an extension, inject plexus-utils if it's not there.
    private pointcut extDepArtifactsResolved( DefaultExtensionManager mgr ):
        call( public Set ResolutionGroup+.getArtifacts() )
        && within( DefaultExtensionManager+ )
        && this( mgr )
        && notHere();

    // We use the same hack here to make sure that plexus 1.1 is available for extensions that do
    // not declare plexus-utils but need it. MNG-2900
    Set around( DefaultExtensionManager mgr ): extDepArtifactsResolved( mgr )
    {
        Set result = proceed( mgr );

        checkPlexusUtils( result, mgr.artifactFactory );

        return result;
    }

    // Intercept retrieval of artifact dependencies of a plugin, inject plexus-utils if it's not there.
    private pointcut pluginDepArtifactsResolved( DefaultPluginManager mgr ):
        call( public Set ResolutionGroup+.getArtifacts() )
        && cflow( execution( Set DefaultPluginManager+.getPluginArtifacts(..) ) )
        && this( mgr )
        && notHere();

    Set around( DefaultPluginManager mgr ): pluginDepArtifactsResolved( mgr )
    {
        Set result = proceed( mgr );

        checkPlexusUtils( result, mgr.artifactFactory );

        return result;
    }

    private pointcut methodsTakingRequest( MavenExecutionRequest request ):
        execution( MavenExecutionResult *.*( MavenExecutionRequest ) )
        && !withincode( * *.*( MavenExecutionRequest ) )
        && args( request )
        && notHere();

    // capture the request instance before it's passed into any method that returns a corresponding MavenExecutionResult.
    Object around( MavenExecutionRequest request ): methodsTakingRequest( request )
    {
        this.request = request;

        try
        {
            return proceed( request );
        }
        finally
        {
            this.request = null;
        }
    }

    // USE Request to compensate for old buildSettings() API.
    private pointcut buildSettings( MavenSettingsBuilder builder ):
        execution( public Settings MavenSettingsBuilder+.buildSettings() )
        && target( builder )
        && notHere();

    // redirect old buildSettings() call to the new one, using the request captured above.
    Settings around( MavenSettingsBuilder builder )
        throws IOException, XmlPullParserException:
            buildSettings( builder )
    {
        return builder.buildSettings( request );
    }

    // Re-Introduce old buildSettings() API.
    public Settings MavenSettingsBuilder.buildSettings()
        throws IOException, XmlPullParserException
    {
        return null;
    }

    public Settings DefaultMavenSettingsBuilder.buildSettings()
    throws IOException, XmlPullParserException
    {
        return null;
    }

    private pointcut pluginManager( DefaultPluginManager manager ):
        execution( * DefaultPluginManager.*( .. ) )
        && this( manager );

    private pointcut pluginRealmCreation( Plugin plugin, DefaultPluginManager manager ):
        call( ClassRealm MavenRealmManager+.createPluginRealm( Plugin, Artifact, Collection ) )
        && cflow( pluginManager( manager ) )
        && args( plugin, .. );

    // Add various imports for Xpp3 stuff back into the core realm every time a plugin realm is created.
    ClassRealm around( Plugin plugin, DefaultPluginManager manager ): pluginRealmCreation( plugin, manager )
    {
        ClassRealm pluginRealm = proceed( plugin, manager );

        try
        {
            String parentRealmId = manager.container.getContainerRealm().getId();

            // adding for MNG-3012 to try to work around problems with Xpp3Dom (from plexus-utils)
            // spawning a ClassCastException when a mojo calls plugin.getConfiguration() from maven-model...
            pluginRealm.importFrom( parentRealmId, Xpp3Dom.class.getName() );
            pluginRealm.importFrom( parentRealmId, "org.codehaus.plexus.util.xml.pull" );

            // Adding for MNG-2878, since maven-reporting-impl was removed from the
            // internal list of artifacts managed by maven, the classloader is different
            // between maven-reporting-impl and maven-reporting-api...so this resource
            // is not available from the AbstractMavenReport since it uses:
            // getClass().getResourceAsStream( "/default-report.xml" )
            // (maven-reporting-impl version 2.0; line 134; affects: checkstyle plugin, and probably others)
            pluginRealm.importFrom( parentRealmId, "/default-report.xml" );
        }
        catch ( NoSuchRealmException e )
        {
            // can't happen here. All realms are concretely resolved by now.
        }

        return pluginRealm;
    }

    private pointcut enableLoggingCall( Logger logger ):
        execution( void LogEnabled+.enableLogging( Logger ) )
        && args( logger );

    after( Logger logger ): enableLoggingCall( logger )
    {
        if ( this.logger == null )
        {
            this.logger = logger;
        }
    }

    private pointcut addMojoBindingCall( String phase, MojoBinding binding ):
        call( void LifecycleUtils.addMojoBinding( String, MojoBinding, .. ) )
        && args( phase, binding, .. );

    void around( String phase, MojoBinding binding ): addMojoBindingCall( phase, binding )
    {
        try
        {
            proceed( phase, binding );
        }
        catch ( NoSuchPhaseException e )
        {
            logger.debug( "Mojo execution: " + MojoBindingUtils.toString( binding )
                          + " cannot be attached to lifecycle phase: " + phase
                          + "; it does not exist. Ignoring this binding." );
        }
    }

    // --------------------------
    // UTILITIES
    // --------------------------

    private void checkPlexusUtils( Set dependencyArtifacts, ArtifactFactory artifactFactory )
    {
        // ----------------------------------------------------------------------------
        // If the plugin already declares a dependency on plexus-utils then we're all
        // set as the plugin author is aware of its use. If we don't have a dependency
        // on plexus-utils then we must protect users from stupid plugin authors who
        // did not declare a direct dependency on plexus-utils because the version
        // Maven uses is hidden from downstream use. We will also bump up any
        // anything below 1.1 to 1.1 as this mimics the behaviour in 2.0.5 where
        // plexus-utils 1.1 was being forced into use.
        // ----------------------------------------------------------------------------

        if ( vr == null )
        {
            try
            {
                vr = VersionRange.createFromVersionSpec( "[1.1,)" );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                // Won't happen
            }
        }

        boolean plexusUtilsPresent = false;

        for ( Iterator i = dependencyArtifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( a.getArtifactId().equals( "plexus-utils" )
                 && vr.containsVersion( new DefaultArtifactVersion( a.getVersion() ) ) )
            {
                plexusUtilsPresent = true;

                break;
            }
        }

        if ( !plexusUtilsPresent )
        {
            // We will add plexus-utils as every plugin was getting this anyway from Maven itself. We will set the
            // version to the latest version we know that works as of the 2.0.6 release. We set the scope to runtime
            // as this is what's implicitly happening in 2.0.6.

            if ( plexusUtilsArtifact == null )
            {
                plexusUtilsArtifact = artifactFactory.createArtifact( "org.codehaus.plexus",
                                                                      "plexus-utils",
                                                                      "1.1",
                                                                      Artifact.SCOPE_RUNTIME,
                                                                      "jar" );
            }

            dependencyArtifacts.add( plexusUtilsArtifact );
        }
    }

}
