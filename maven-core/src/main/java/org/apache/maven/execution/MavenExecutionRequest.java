package org.apache.maven.execution;

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface MavenExecutionRequest
{
    ArtifactRepository getLocalRepository();

    List getGoals();

    String getType();
}
