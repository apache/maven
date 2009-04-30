package org.apache.maven.project;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

public interface ProjectBuilderConfiguration
{
    ArtifactRepository getLocalRepository();
    
    List<ArtifactRepository> getRemoteRepositories();

    ProfileManager getGlobalProfileManager();

    Properties getExecutionProperties();

    ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager );

    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );

    ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );
    
    MavenProject getTopLevelProjectFromReactor();
    
    void setTopLevelProjectForReactor(MavenProject mavenProject);
    
}
