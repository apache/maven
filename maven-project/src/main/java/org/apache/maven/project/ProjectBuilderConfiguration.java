package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.profiles.ProfileManager;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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

    //TODO: these do not belong here, we can profile things else where
    Date getBuildStartTime();

    ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime );    
    
    MavenProject getTopLevelProjectFromReactor();
    
    void setTopLevelProjectForReactor(MavenProject mavenProject);
    
    void setPlugins(Set<Plugin> plugins);
    
    Set<Plugin> getPlugins();
}
