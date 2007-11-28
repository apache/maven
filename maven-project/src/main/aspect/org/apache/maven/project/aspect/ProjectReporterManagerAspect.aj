package org.apache.maven.project.aspect;

import org.aspectj.lang.Aspects;

import org.apache.maven.project.error.ProjectErrorReporter;

public aspect ProjectReporterManagerAspect
{

    public void setReporter( ProjectErrorReporter reporter )
    {
        PBEDerivativeReporterAspect pbeDerivativeReporterAspect = (PBEDerivativeReporterAspect) Aspects.aspectOf( PBEDerivativeReporterAspect.class );
        pbeDerivativeReporterAspect.setProjectErrorReporter( reporter );

        ProfileErrorReporterAspect profileErrorReporterAspect = (ProfileErrorReporterAspect) Aspects.aspectOf( ProfileErrorReporterAspect.class );
        profileErrorReporterAspect.setProjectErrorReporter( reporter );

        ProjectIOErrorReporterAspect projectIOErrorReporterAspect = (ProjectIOErrorReporterAspect) Aspects.aspectOf( ProjectIOErrorReporterAspect.class );
        projectIOErrorReporterAspect.setProjectErrorReporter( reporter );

        ProjectArtifactErrorReporterAspect projectArtifactErrorReporterAspect = (ProjectArtifactErrorReporterAspect) Aspects.aspectOf( ProjectArtifactErrorReporterAspect.class );
        projectArtifactErrorReporterAspect.setProjectErrorReporter( reporter );
    }

}
