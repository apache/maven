package org.apache.maven.errors;

public abstract aspect AbstractCoreReporterManagerAspect
{

    private CoreErrorReporter reporter;

    public void setCoreErrorReporter( CoreErrorReporter reporter )
    {
        this.reporter = reporter;
    }

    protected CoreErrorReporter getReporter()
    {
        if ( reporter == null )
        {
            reporter = new DefaultCoreErrorReporter();
        }

        return reporter;
    }

}
