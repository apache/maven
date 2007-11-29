package org.apache.maven.project.error;

public final class ProjectReporterManager
{

    private static ProjectErrorReporter reporter;

    private ProjectReporterManager()
    {
    }

    public static ProjectErrorReporter getReporter()
    {
        if ( reporter == null )
        {
            reporter = new DefaultProjectErrorReporter();
        }

        return reporter;
    }

    public static void setReporter( ProjectErrorReporter instance )
    {
        reporter = instance;
    }

    public static void clearReporter()
    {
        reporter = null;
    }

}
