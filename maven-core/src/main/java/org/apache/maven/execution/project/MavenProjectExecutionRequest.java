package org.apache.maven.execution.project;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.AbstractMavenExecutionRequest;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenProjectExecutionRequest
    extends AbstractMavenExecutionRequest
{
    private File pom;

    public MavenProjectExecutionRequest( ArtifactRepository localRepository, List goals, File pom )
    {
        super( localRepository, goals );

        this.pom = pom;

        type = "project";
    }

    public File getPom()
    {
        return pom;
    }
}
