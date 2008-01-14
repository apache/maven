package org.apache.maven.errors;

public abstract aspect AbstractCoreReporterAspect
{

    protected pointcut notWithinAspect():
        !within( *.*Aspect+ );

    protected CoreErrorReporter getReporter()
    {
        return CoreReporterManager.getReporter();
    }

}
