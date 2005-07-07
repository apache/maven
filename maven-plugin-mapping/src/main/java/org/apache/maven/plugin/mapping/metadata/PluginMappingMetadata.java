package org.apache.maven.plugin.mapping.metadata;

import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.io.File;

public class PluginMappingMetadata
    implements RepositoryMetadata
{
    private static final String PLUGIN_MAPPING_FILE = "plugins.xml";
    
    private final String groupId;

    private File metadataFile;

    public PluginMappingMetadata( String groupId )
    {
        this.groupId = groupId;
    }

    public String getRepositoryPath()
    {
        return groupId + "/" + PLUGIN_MAPPING_FILE;
    }

    public void setFile( File metadataFile )
    {
        this.metadataFile = metadataFile;
    }

    public File getFile()
    {
        return metadataFile;
    }

}
