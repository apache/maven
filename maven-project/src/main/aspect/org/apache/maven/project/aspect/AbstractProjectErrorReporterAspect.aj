package org.apache.maven.project.aspect;

import org.apache.maven.project.error.DefaultProjectErrorReporter;
import org.apache.maven.project.error.ProjectErrorReporter;

public abstract aspect AbstractProjectErrorReporterAspect
{

    private ProjectErrorReporter reporter;

    public void setProjectErrorReporter( ProjectErrorReporter reporter )
    {
        this.reporter = reporter;
    }

    protected ProjectErrorReporter getReporter()
    {
        if ( reporter == null )
        {
            reporter = new DefaultProjectErrorReporter();
        }

        return reporter;
    }

}
