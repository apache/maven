package org.apache.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

import java.util.List;

public interface PluginManagerSupport
{

    Artifact resolvePluginArtifact( Plugin plugin,
                                    MavenProject project,
                                    MavenSession session )
        throws PluginManagerException, InvalidPluginException, PluginVersionResolutionException,
        ArtifactResolutionException, ArtifactNotFoundException;

    MavenProject buildPluginProject( Plugin plugin,
                                     ArtifactRepository localRepository,
                                     List remoteRepositories )
        throws InvalidPluginException;

    /**
     * @param pluginProject
     * @todo would be better to store this in the plugin descriptor, but then it won't be available to the version
     * manager which executes before the plugin is instantiated
     */
    void checkRequiredMavenVersion( Plugin plugin,
                                    MavenProject pluginProject,
                                    ArtifactRepository localRepository,
                                    List remoteRepositories )
        throws PluginVersionResolutionException, InvalidPluginException;

    void checkPluginDependencySpec( Plugin plugin,
                                    MavenProject pluginProject )
        throws InvalidPluginException;

    PluginDescriptor loadIsolatedPluginDescriptor( Plugin plugin,
                                                   MavenProject project,
                                                   MavenSession session );

}
