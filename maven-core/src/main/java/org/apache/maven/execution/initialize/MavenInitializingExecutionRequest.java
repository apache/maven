package org.apache.maven.execution.initialize;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.AbstractMavenExecutionRequest;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenInitializingExecutionRequest
    extends AbstractMavenExecutionRequest
{
    public MavenInitializingExecutionRequest( ArtifactRepository localRepository, List goals )
    {
        super( localRepository, goals );

        type = "initializing";               
    }
}
