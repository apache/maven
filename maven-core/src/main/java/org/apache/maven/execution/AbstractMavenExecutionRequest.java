package org.apache.maven.execution;

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class AbstractMavenExecutionRequest
    implements MavenExecutionRequest
{
    protected ArtifactRepository localRepository;

    protected List goals;

    protected File mavenHome;

    protected String type;

    public AbstractMavenExecutionRequest( ArtifactRepository localRepository, List goals )
    {
        this.localRepository = localRepository;

        this.goals = goals;

        this.mavenHome = mavenHome;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public List getGoals()
    {
        return goals;
    }

    public File getMavenHome()
    {
        return mavenHome;
    }

    public String getType()
    {
        return type;
    }
}
