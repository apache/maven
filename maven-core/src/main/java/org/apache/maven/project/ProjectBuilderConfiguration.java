package org.apache.maven.project;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Profile;

@Deprecated
public interface ProjectBuilderConfiguration
    extends ProjectBuildingRequest
{
    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );
    
    ArtifactRepository getLocalRepository();

    ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    List<ArtifactRepository> getRemoteRepositories();

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    Properties getSystemProperties();

    void setTopLevelProjectForReactor(MavenProject mavenProject);

    MavenProject getTopLevelProjectFromReactor();
        
    ProjectBuilderConfiguration setProcessPlugins( boolean processPlugins );
    
    boolean isProcessPlugins();

    // Profiles
    
    /**
     * Set any active profiles that the {@link ProjectBuilder} should consider while constructing
     * a {@link MavenProject}.
     */
    void setActiveProfileIds( List<String> activeProfileIds );
        
    List<String> getActiveProfileIds();

    void setInactiveProfileIds( List<String> inactiveProfileIds );

    List<String> getInactiveProfileIds();
    
    /**
     * Add a {@link org.apache.maven.model.Profile} that has come from an external source. This may be from a custom configuration
     * like the MavenCLI settings.xml file, or from a custom dialog in an IDE integration like M2Eclipse.
     * @param profile
     */
    void addProfile( Profile profile );
    
    void setProfiles( List<Profile> profiles );
    
    List<Profile> getProfiles();
}
