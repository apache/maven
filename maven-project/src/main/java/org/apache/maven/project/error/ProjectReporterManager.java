package org.apache.maven.project.error;

public final class ProjectReporterManager
{

    // FIXME: This is not threadsafe!
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
