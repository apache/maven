package org.apache.maven.errors;

import org.aspectj.lang.Aspects;

public aspect CoreReporterManagerAspect
{

    public void setReporter( CoreErrorReporter reporter )
    {
        BuildFailureReporterAspect buildFailureReporterAspect = (BuildFailureReporterAspect) Aspects.aspectOf( BuildFailureReporterAspect.class );
        buildFailureReporterAspect.setCoreErrorReporter( reporter );
    }

}
