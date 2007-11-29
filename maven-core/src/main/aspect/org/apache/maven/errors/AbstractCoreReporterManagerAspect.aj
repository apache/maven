package org.apache.maven.errors;

public abstract aspect AbstractCoreReporterManagerAspect
{

    protected CoreErrorReporter getReporter()
    {
        return CoreReporterManager.getReporter();
    }

}
