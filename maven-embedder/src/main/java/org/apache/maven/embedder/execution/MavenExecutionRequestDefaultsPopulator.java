package org.apache.maven.embedder.execution;

import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;

public interface MavenExecutionRequestDefaultsPopulator 
{
    String ROLE = MavenExecutionRequestDefaultsPopulator.class.getName();
    
    MavenExecutionRequest populateDefaults( MavenExecutionRequest request )
		throws MavenEmbedderException;
}
