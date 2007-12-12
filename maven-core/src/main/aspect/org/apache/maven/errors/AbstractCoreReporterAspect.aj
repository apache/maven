package org.apache.maven.errors;

public abstract aspect AbstractCoreReporterAspect
{

    protected CoreErrorReporter getReporter()
    {
        return CoreReporterManager.getReporter();
    }

}
