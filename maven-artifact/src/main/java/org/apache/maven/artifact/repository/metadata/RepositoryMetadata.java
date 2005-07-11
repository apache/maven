package org.apache.maven.artifact.repository.metadata;

import java.io.File;

public interface RepositoryMetadata
{
    
    String getRepositoryPath();
    
    File getFile();
    
    void setFile( File file );

}
