package org.apache.maven.project.aspect;

import org.apache.maven.project.error.ProjectErrorReporter;
import org.apache.maven.project.error.ProjectReporterManager;

public abstract aspect AbstractProjectErrorReporterAspect issingleton()
{

    protected pointcut notWithinAspect():
        !within( org.apache.maven.project.aspect.*+ );

    protected ProjectErrorReporter getReporter()
    {
        return ProjectReporterManager.getReporter();
    }

}
