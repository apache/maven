package org.apache.maven.errors;

public final class CoreReporterManager
{

    private static CoreErrorReporter reporter;

    private CoreReporterManager()
    {
    }

    public static CoreErrorReporter getReporter()
    {
        if ( reporter == null )
        {
            reporter = new DefaultCoreErrorReporter();
        }

        return reporter;
    }

    public static void setReporter( CoreErrorReporter instance )
    {
        reporter = instance;
    }

    public static void clearReporter()
    {
        reporter = null;
    }

}
