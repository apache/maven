package org.apache.maven.project;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Profile;
import org.apache.maven.model.profile.ProfileActivationContext;

public interface ProjectBuilderConfiguration
    extends ProfileActivationContext
{
    ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository );
    
    ArtifactRepository getLocalRepository();

    ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    List<ArtifactRepository> getRemoteRepositories();

    ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties );

    Properties getExecutionProperties();

    void setTopLevelProjectForReactor(MavenProject mavenProject);

    MavenProject getTopLevelProjectFromReactor();
        
    ProjectBuilderConfiguration setProcessPlugins( boolean processPlugins );
    
    boolean isProcessPlugins();

    /**
     * Controls the level of validation to perform on processed models. By default, models are validated in strict mode.
     * 
     * @param lenientValidation A flag whether validation should be lenient instead of strict. For building of projects,
     *            strict validation should be used to ensure proper building. For the mere retrievel of dependencies
     *            during artifact resolution, lenient validation should be used to account for models of poor quality.
     * @return This configuration, never {@code null}.
     */
    ProjectBuilderConfiguration setLenientValidation( boolean lenientValidation );

    /**
     * Gets the level of validation to perform on processed models.
     * 
     * @return {@code true} if lenient validation is enabled and only the dependency information is to be validated,
     *         {@code false} if strict validation is enabled and the entire model is validated.
     */
    boolean istLenientValidation();

    // Profiles
    
    /**
     * Set any active profiles that the {@link MavenProjectBuilder} should consider while constructing
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
