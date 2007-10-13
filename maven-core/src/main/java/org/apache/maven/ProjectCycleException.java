package org.apache.maven;

import org.apache.maven.execution.ReactorManager;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.util.List;

/**
 * Exception which occurs when creating a new {@link ReactorManager} instance,
 * due to failure to sort the current projects. The embedded {@link CycleDetectedException}
 * is thrown by the {@link ProjectSorter}, and context of this wrapped exception
 * includes the list of projects that contain the cycle, along with a friendly
 * rendering of the cycle message indicating that it comes from the current projects list.
 *
 * @author jdcasey
 *
 */
public class ProjectCycleException
    extends BuildFailureException
{

    private final List projects;

    public ProjectCycleException( List projects, String message,
                                  CycleDetectedException cause )
    {
        super( message, cause );
        this.projects = projects;
    }

    public List getProjects()
    {
        return projects;
    }

}
