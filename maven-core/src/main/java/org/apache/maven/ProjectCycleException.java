package org.apache.maven;

import java.util.List;

import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @author jdcasey
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
