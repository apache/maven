package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.shared.model.ModelEventListener;
import org.codehaus.plexus.PlexusContainer;

import java.util.Date;
import java.util.List;
import java.util.Properties;

public interface ProjectBuilderConfiguration
{
    ArtifactRepository getLocalRepository();
    
    List<ArtifactRepository> getRemoteRepositories();

    ProfileManager getGlobalProfileManager();

    Properties getUserProperties();

    Properties getExecutionProperties();

    ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager );

    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );

    ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    ProjectBuilderConfiguration setUserProperties( Properties userProperties );

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    Date getBuildStartTime();

    ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime );    
}
