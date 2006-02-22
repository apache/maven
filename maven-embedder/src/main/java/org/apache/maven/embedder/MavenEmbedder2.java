/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.embedder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.Maven;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.cli.BatchModeDownloadMonitor;
import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


/** Replacement for the {@link MavenEmbedder}.
 */
public class MavenEmbedder2 {
	private final MavenEmbedderLogger logger;

	private Embedder embedder;

	private boolean debug, showErrors, batchMode, suppressPluginRegistry;
	private boolean forcePluginUpdates, forcePluginUpdates2;
	private boolean suppressPluginUpdates, offline, updateSnapshots;
	private boolean checksumFailurePolicy, checksumWarningPolicy;
	private boolean nonRecursive, failFast, failAtEnd, failNever;
	private boolean reactor;
	private String activateProfiles;
	private Properties executionProperties;
	private File userSettingsFile, alternatePomFile;

	/** Creates a new instance with the given logger.
	 */
	public MavenEmbedder2(MavenEmbedderLogger pLogger) {
		logger = pLogger;
	}

	/** Returns, whether debugging mode is on.
	 */
	public boolean isDebug() {
		return debug;
	}

	/** Sets, whether debugging mode is on.
	 */
	public void setDebug(boolean pDebug) {
		debug = pDebug;
	}

	/** Returns, whether stack traces are being shown.
	 */
	public boolean isShowErrors() {
		return showErrors;
	}

	/** Sets, whether stack traces are being shown.
	 */
	public void setShowErrors(boolean pShowErrors) {
		showErrors = pShowErrors;
	}

	/** Returns the set of predefined properties.
	 */
	public Properties getExecutionProperties() {
		return executionProperties;
	}

	/** Sets the set of predefined properties.
	 */
	public void setExecutionProperties(Properties pExecutionProperties) {
		executionProperties = pExecutionProperties;
	}

	/** Returns the location of an alternate file with
	 * user settings.
	 */
	public File getUserSettingsFile() {
		return userSettingsFile;
	}

	/** Sets the location of an alternate file with
	 * user settings.
	 */
	public void setUserSettingsFile(File pUserSettingsPath) {
		userSettingsFile = pUserSettingsPath;
	}

	/** Returns, whether batch mode is on.
	 */
	public boolean isBatchMode() {
		return batchMode;
	}

	/** Sets, whether batch mode is on.
	 */
	public void setBatchMode(boolean pBatchMode) {
		batchMode = pBatchMode;
	}

	/** Returns, whether the plugin registry is being suppressed.
	 */
	public boolean isSuppressPluginRegistry() {
		return suppressPluginRegistry;
	}

	/** Sets, whether the plugin registry is being suppressed.
	 */
	public void setSuppressPluginRegistry(boolean pSuppressPluginRegistry) {
		suppressPluginRegistry = pSuppressPluginRegistry;
	}

	/** Returns, whether plugin updates are being forced.
	 */
	public boolean isForcePluginUpdates() {
		return forcePluginUpdates;
	}

	/** Sets, whether plugin updates are being forced.
	 */
	public void setForcePluginUpdates(boolean pForcePluginUpdates) {
		forcePluginUpdates = pForcePluginUpdates;
	}

	/** Returns, whether plugin updates are being forced2.
	 */
	public boolean isForcePluginUpdates2() {
		return forcePluginUpdates2;
	}

	/** Sets, whether plugin updates are being forced2.
	 */
	public void setForcePluginUpdates2(boolean pForcePluginUpdates2) {
		forcePluginUpdates2 = pForcePluginUpdates2;
	}

	/** Returns a definition of profiles, which are being
	 * activated.
	 */
    public String getActivateProfiles() {
		return activateProfiles;
	}

    /** Sets a definition of profiles, which are being
	 * activated.
	 */
	public void setActivateProfiles(String pActivateProfiles) {
		activateProfiles = pActivateProfiles;
	}

	/** Returns, whether offline mode is enabled.
	 */
	public boolean isOffline() {
		return offline;
	}

	/** Sets, whether offline mode is enabled.
	 */
	public void setOffline(boolean pOffline) {
		offline = pOffline;
	}

	/** Returns, whether snapshots are being updated.
	 */
	public boolean isUpdateSnapshots() {
		return updateSnapshots;
	}

	/** Sets, whether snapshots are being updated.
	 */
	public void setUpdateSnapshots(boolean pUpdateSnapshots) {
		updateSnapshots = pUpdateSnapshots;
	}

	/** Returns, whether wrong checksums are considered a
	 * failure.
	 */
	public boolean isChecksumFailurePolicy() {
		return checksumFailurePolicy;
	}

	/** Sets, whether wrong checksums are considered a
	 * failure.
	 */
	public void setChecksumFailurePolicy(boolean pChecksumFailurePolicy) {
		checksumFailurePolicy = pChecksumFailurePolicy;
	}

	/** Returns, whether wrong checksums are considered a
	 * warning.
	 */
	public boolean isChecksumWarningPolicy() {
		return checksumWarningPolicy;
	}

	/** Sets, whether wrong checksums are considered a
	 * warning.
	 */
	public void setChecksumWarningPolicy(boolean pChecksumWarningPolicy) {
		checksumWarningPolicy = pChecksumWarningPolicy;
	}

	/** Returns, whether calling submodules is disabled.
	 */
	public boolean isNonRecursive() {
		return nonRecursive;
	}

	/** Sets, whether calling submodules is disabled.
	 */
	public void setNonRecursive(boolean pNonRecursive) {
		nonRecursive = pNonRecursive;
	}

	/** Returns, whether failures will abort the program.
	 */
	public boolean isFailFast() {
		return failFast;
	}

	/** Sets, whether failures will abort the program.
	 */
	public void setFailFast(boolean pFailFast) {
		failFast = pFailFast;
	}

	/** Returns, whether failures are reported at the end.
	 */
	public boolean isFailAtEnd() {
		return failAtEnd;
	}

	/** Sets, whether failures are reported at the end.
	 */
	public void setFailAtEnd(boolean pFailAtEnd) {
		failAtEnd = pFailAtEnd;
	}

	/** Returns, whether failures are suppressed.
	 */
	public boolean isFailNever() {
		return failNever;
	}

	/** Sets, whether failures are suppressed.
	 */
	public void setFailNever(boolean pFailNever) {
		failNever = pFailNever;
	}

	/** Returns, whether the reactor is on.
	 */
	public boolean isReactor() {
		return reactor;
	}

	/** Sets, whether the reactor is on.
	 */
	public void setReactor(boolean pReactor) {
		reactor = pReactor;
	}

	/** Returns the location of an alternate POM file.
	 */
	public File getAlternatePomFile() {
		return alternatePomFile;
	}

	/** Sets the location of an alternate POM file.
	 */
	public void setAlternatePomFile(File pAlternatePomFile) {
		alternatePomFile = pAlternatePomFile;
	}

	/** Returns, whether plugin updates are being suppressed.
	 */
    public boolean isSuppressPluginUpdates() {
		return suppressPluginUpdates;
	}

	/** Sets, whether plugin updates are being suppressed.
	 */
	public void setSuppressPluginUpdates(boolean pSuppressPluginUpdates) {
		suppressPluginUpdates = pSuppressPluginUpdates;
	}

	private Settings buildSettings()
    	throws ComponentLookupException, SettingsConfigurationException
	{

    	Settings settings = null;

    	MavenSettingsBuilder settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );

    	try
    	{
    		if ( userSettingsFile != null )
    		{

    			if ( userSettingsFile.exists() && !userSettingsFile.isDirectory() )
    			{
    				settings = settingsBuilder.buildSettings( userSettingsFile );
    			}
    			else
    			{
    				logger.warn( "WARNING: Alternate user settings file: " + userSettingsFile +
                    	" is invalid. Using default path." );
    			}
    		}

    		if ( settings == null )
    		{
    			settings = settingsBuilder.buildSettings();
    		}
    	}
    	catch ( IOException e )
    	{
    		throw new SettingsConfigurationException( "Error reading settings file", e );
    	}
    	catch ( XmlPullParserException e )
    	{
    		throw new SettingsConfigurationException( e.getMessage(), e.getDetail(), e.getLineNumber(),
    												  e.getColumnNumber() );
    	}

    	// why aren't these part of the runtime info? jvz.

    	if ( isBatchMode() )
    	{
    		settings.setInteractiveMode( false );
    	}

    	if ( isSuppressPluginRegistry() )
    	{
    		settings.setUsePluginRegistry( false );
    	}

    	// Create settings runtime info

    	settings.setRuntimeInfo( createRuntimeInfo( settings ) );

    	return settings;
	}

    private RuntimeInfo createRuntimeInfo( Settings settings )
    {
        RuntimeInfo runtimeInfo = new RuntimeInfo( settings );

        if ( isForcePluginUpdates() || isForcePluginUpdates2() )
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.TRUE );
        }
        else if ( isSuppressPluginUpdates() )
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.FALSE );
        }

        return runtimeInfo;
    }

    /** Executes the given set of goals in the given
     * directory.
     */
	public void execute( String[] pGoals, File pBaseDir )
		throws PlexusContainerException, ComponentLookupException,
		SettingsConfigurationException, MavenExecutionException
	{
        embedder = new Embedder();
        EventDispatcher eventDispatcher = new DefaultEventDispatcher();
        embedder.start();

        Settings settings = buildSettings();

        Maven maven = null;
        MavenExecutionRequest request = null;
        final LoggerManager loggerManager;

        // logger must be created first
        loggerManager = (LoggerManager) embedder.lookup( LoggerManager.ROLE );

        try {
        	if ( debug )
        	{
        		loggerManager.setThreshold( Logger.LEVEL_DEBUG );
        	}
        	
        	ProfileManager profileManager = new DefaultProfileManager( embedder.getContainer() );

        	profileManager.loadSettingsProfiles( settings );
        	
        	String profilesLine = getActivateProfiles();
        	if ( profilesLine != null )
        	{
        		StringTokenizer profileTokens = new StringTokenizer( profilesLine, "," );
        		
        		while ( profileTokens.hasMoreTokens() )
        		{
        			String profileAction = profileTokens.nextToken().trim();
        			
        			if ( profileAction.startsWith( "-" ) )
        			{
        				profileManager.explicitlyDeactivate( profileAction.substring( 1 ) );
        			}
        			else if ( profileAction.startsWith( "+" ) )
        			{
        				profileManager.explicitlyActivate( profileAction.substring( 1 ) );
        			}
        			else
        			{
        				// TODO: deprecate this eventually!
        				profileManager.explicitlyActivate( profileAction );
        			}
        		}
        	}
        	
        	request = createRequest( settings, pGoals, eventDispatcher,
        			loggerManager, profileManager, pBaseDir );
        	
        	setProjectFileOptions( request );
        	
        	maven = createMavenInstance( settings.isInteractiveMode() );
        }
        finally
        {
        	try
        	{
        		embedder.release( loggerManager );
        	}
        	catch ( ComponentLifecycleException e )
        	{
        		logger.fatalError( "Error releasing logging manager", e );
        	}
        }

        maven.execute( request );
	}


    private MavenExecutionRequest createRequest( Settings settings, String[] goals,
                                                 EventDispatcher eventDispatcher, LoggerManager loggerManager,
                                                 ProfileManager profileManager,
                                                 File baseDir)
        throws ComponentLookupException
    {
        MavenExecutionRequest request;

        ArtifactRepository localRepository = createLocalRepository( settings );

        request = new DefaultMavenExecutionRequest( localRepository, settings, eventDispatcher,
                                                    Arrays.asList(goals), baseDir.getPath(), profileManager,
                                                    executionProperties, showErrors );

        // TODO [BP]: do we set one per mojo? where to do it?
        Logger log = loggerManager.getLoggerForComponent( Mojo.ROLE );

        if ( log != null )
        {
            request.addEventMonitor( new DefaultEventMonitor( log ) );
        }

        if ( isNonRecursive() )
        {
            request.setRecursive( false );
        }

        if ( isFailFast() )
        {
            request.setFailureBehavior( ReactorManager.FAIL_FAST );
        }
        else if ( isFailAtEnd() )
        {
            request.setFailureBehavior( ReactorManager.FAIL_AT_END );
        }
        else if ( isFailNever() )
        {
            request.setFailureBehavior( ReactorManager.FAIL_NEVER );
        }

        return request;
    }

    private void setProjectFileOptions( MavenExecutionRequest request )
    {
        if ( isReactor() )
        {
            request.setReactorActive( true );
        }
        else if ( getAlternatePomFile() != null )
        {
            request.setPomFile( getAlternatePomFile().getPath() );
        }
    }

    private Maven createMavenInstance( boolean interactive )
        throws ComponentLookupException
    {
        // TODO [BP]: doing this here as it is CLI specific, though it doesn't feel like the right place (likewise logger).
        WagonManager wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );
        if ( interactive )
        {
            wagonManager.setDownloadMonitor( new ConsoleDownloadMonitor() );
        }
        else
        {
            wagonManager.setDownloadMonitor( new BatchModeDownloadMonitor() );
        }

        wagonManager.setInteractive( interactive );

        return (Maven) embedder.lookup( Maven.ROLE );
    }

    private ArtifactRepository createLocalRepository( Settings settings )
        throws ComponentLookupException
    {
        // TODO: release
        // TODO: something in plexus to show all active hooks?
        ArtifactRepositoryLayout repositoryLayout =
            (ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, "default" );

        ArtifactRepositoryFactory artifactRepositoryFactory =
            (ArtifactRepositoryFactory) embedder.lookup( ArtifactRepositoryFactory.ROLE );

        String url = settings.getLocalRepository();

        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( isOffline() )
        {
            settings.setOffline( true );

            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && isUpdateSnapshots() )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        if ( isChecksumFailurePolicy() )
        {
        	logger.info( "+ Enabling strict checksum verification on all artifact downloads." );

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL );
        }
        else if ( isChecksumWarningPolicy() )
        {
            System.out.println( "+ Disabling strict checksum verification on all artifact downloads." );

            artifactRepositoryFactory.setGlobalChecksumPolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
        }

        return localRepository;
    }
}
