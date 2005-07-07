package org.apache.maven.artifact.repository.metadata;

import java.io.File;

public interface RepositoryMetadata
{
    
    String getRepositoryPath();
    
    void setFile( File metadataFile );
    
    File getFile();

}
