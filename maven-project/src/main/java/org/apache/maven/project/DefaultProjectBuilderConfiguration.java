package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;

import java.util.Properties;

public class DefaultProjectBuilderConfiguration
    implements ProjectBuilderConfiguration
{

    private ProfileManager globalProfileManager;

    private ArtifactRepository localRepository;

    private Properties userProperties;

    private Properties executionProperties = System.getProperties();

    public DefaultProjectBuilderConfiguration()
    {
    }

    public ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager )
    {
        this.globalProfileManager = globalProfileManager;
        return this;
    }

    public ProfileManager getGlobalProfileManager()
    {
        return globalProfileManager;
    }

    public ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public ProjectBuilderConfiguration setUserProperties( Properties userProperties )
    {
        this.userProperties = userProperties;
        return this;
    }

    public Properties getUserProperties()
    {
        if ( userProperties == null )
        {
            userProperties = new Properties();
        }

        return userProperties;
    }

    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties )
    {
        this.executionProperties = executionProperties;
        return this;
    }

}
