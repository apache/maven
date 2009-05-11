package org.apache.maven.project;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

public interface ProjectBuilderConfiguration
{
    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );
    
    ArtifactRepository getLocalRepository();

    ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    List<ArtifactRepository> getRemoteRepositories();

    ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager );

    ProfileManager getGlobalProfileManager();

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    Properties getExecutionProperties();

    void setTopLevelProjectForReactor(MavenProject mavenProject);

    MavenProject getTopLevelProjectFromReactor();
        
    ProjectBuilderConfiguration setProcessPlugins( boolean processPlugins );
    
    boolean isProcessPlugins();
    
}
