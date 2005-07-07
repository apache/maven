package org.apache.maven.plugin.mapping;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;

import java.util.List;

public interface MavenPluginMappingBuilder
{

    PluginMappingManager loadPluginMappings( List pluginGroupIds, List pluginRepositories,
                                            ArtifactRepository localRepository )
        throws RepositoryMetadataManagementException, PluginMappingManagementException;

}
