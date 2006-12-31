package org.apache.maven.embedder.execution;

import java.io.File;

import org.apache.maven.MavenTools;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.logging.AbstractLogEnabled;

public class DefaultMavenExecutionRequestDefaultsPopulator
    extends AbstractLogEnabled
	implements MavenExecutionRequestDefaultsPopulator
{
	private MavenTools mavenTools;
	
	private ArtifactRepositoryFactory artifactRepositoryFactory;
	
	public MavenExecutionRequest populateDefaults(MavenExecutionRequest request) 
        throws MavenEmbedderException
    {
        // Settings        
        // Local repository  
        // TransferListener
        // EventMonitor
		// Proxy

		// Settings
		
        if ( request.getSettings() == null )
        {
            File userSettingsPath = mavenTools.getUserSettingsPath( request.getSettingsFile() );

            File globalSettingsFile = mavenTools.getGlobalSettingsPath();

            try
            {
                request.setSettings( mavenTools.buildSettings( userSettingsPath, globalSettingsFile, request.isInteractiveMode(),
                                                         request.isOffline(), request.isUsePluginRegistry(),
                                                         request.isUsePluginUpdateOverride() ) );
            }
            catch ( SettingsConfigurationException e )
            {
                throw new MavenEmbedderException( "Error processing settings.xml.", e );
            }
        }

        // Local repository
        
        if ( request.getLocalRepository() == null )
        {
            String localRepositoryPath = mavenTools.getLocalRepositoryPath( request.getSettings() );

            if ( request.getLocalRepository() == null )
            {
                request.setLocalRepository( mavenTools.createLocalRepository( new File( localRepositoryPath ) ) );
            }                        
        }
        
        // Repository update policies
        
        boolean snapshotPolicySet = false;

        if ( request.isOffline() )
        {
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet ) {
            if ( request.isUpdateSnapshots() )
            {
                artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
            }
            else if ( request.isNoSnapshotUpdates() )
            {
                getLogger().info( "+ Supressing SNAPSHOT updates.");
                artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
            }
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( request.getGlobalChecksumPolicy() );        

        return request;
    }	
}
