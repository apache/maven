package org.apache.maven.execution.reactor;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.AbstractMavenExecutionRequest;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenReactorExecutionRequest
    extends AbstractMavenExecutionRequest
{
    private String includes;

    private String excludes;

    private File baseDirectory;

    public MavenReactorExecutionRequest( ArtifactRepository localRepository, List goals, String includes, String excludes, File baseDirectory )
    {
        super( localRepository, goals );

        this.includes = includes;

        this.excludes = excludes;

        this.baseDirectory = baseDirectory;

        type = "reactor";
    }

    public String getIncludes()
    {
        return includes;
    }

    public String getExcludes()
    {
        return excludes;
    }

    public File getBaseDirectory()
    {
        return baseDirectory;
    }
}
