package org.apache.maven.lifecycle;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;

public class NoPluginFoundForPrefixException
    extends Exception
{
    private String prefix;
    
    private ArtifactRepository localRepository;
    
    private List<ArtifactRepository> remoteRepositories;
    
    public NoPluginFoundForPrefixException( String prefix, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
    {
        super( "No plugin found for prefix '" + prefix + "'" );
        this.prefix = prefix;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;        
    }
}
