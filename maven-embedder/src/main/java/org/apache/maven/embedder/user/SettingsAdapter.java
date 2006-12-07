package org.apache.maven.embedder.user;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.Settings;

import java.util.List;

/**
 * Adapt a {@link MavenExecutionRequest} to a {@link Settings} object for use in the Maven core. 
 *
 * @author Jason van Zyl
 */
public class SettingsAdapter
    extends Settings
{
    private MavenExecutionRequest request;

    public SettingsAdapter( MavenExecutionRequest request )
    {
        this.request = request;
    }

    public String getLocalRepository()
    {
        return request.getLocalRepositoryPath().getAbsolutePath();
    }

    public boolean isInteractiveMode()
    {
        return request.isInteractiveMode();
    }

    public boolean isUsePluginRegistry()
    {
        return request.isUsePluginRegistry();
    }

    public boolean isOffline()
    {
        return request.isOffline();
    }

    public List getProxies()
    {
        return request.getProxies();
    }

    public List getServers()
    {
        return request.getServers();
    }

    public List getMirrors()
    {
        return request.getMirrors();
    }

    public List getProfiles()
    {
        return request.getProfiles();
    }

    public List getActiveProfiles()
    {
        return request.getActiveProfiles();
    }

    public List getPluginGroups()
    {
        return request.getPluginGroups();
    }
}
