package org.apache.maven.execution;

import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface MavenExecutionResult
{
    MavenProject getMavenProject();

    // for each exception
    // - knowing what artifacts are missing
    // - project building exception
    // - invalid project model exception: list of markers
    // - xmlpull parser exception
    List getExceptions();
}
