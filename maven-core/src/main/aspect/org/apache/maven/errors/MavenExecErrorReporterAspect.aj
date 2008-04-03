package org.apache.maven.errors;

import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.reactor.MissingModuleException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.DefaultMaven;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.List;

public aspect MavenExecErrorReporterAspect
    extends AbstractCoreReporterAspect
{

    private pointcut dm_getProjects( MavenExecutionRequest request ):
        execution( List DefaultMaven.getProjects( MavenExecutionRequest ) )
        && args( request );

    private pointcut dm_collectProjects( MavenExecutionRequest request ):
        execution( List DefaultMaven.collectProjects( List, MavenExecutionRequest, boolean ) )
        && args( *, request, * );

    private MavenProject currentProject;
    private ArtifactVersion mavenVersion;

    before( RuntimeInformation ri ):
        call( * RuntimeInformation+.getApplicationVersion() )
        && within( DefaultMaven )
        && target( ri )
    {
        mavenVersion = ri.getApplicationVersion();
    }

    MavenProject around()
        throws ProjectBuildingException:
        cflow( dm_collectProjects( MavenExecutionRequest ) )
        && within( DefaultMaven )
        && call( MavenProject MavenProjectBuilder+.build( .. ) )
    {
        currentProject = proceed();
        return currentProject;
    }

    MavenExecutionException around():
        cflow( dm_getProjects( MavenExecutionRequest ) )
        && cflow( dm_collectProjects( MavenExecutionRequest ) )
        && call( MavenExecutionException.new( String, File ) )
    {
        MavenExecutionException err = proceed();

        getReporter().reportInvalidMavenVersion( currentProject, mavenVersion, err );

        return err;
    }

    after( MissingModuleException err ):
        execution( MissingModuleException.new( String, File, File ) )
        && this( err )
    {
        getReporter().reportMissingModulePom( err );
    }

    after(): dm_collectProjects( MavenExecutionRequest )
    {
        currentProject = null;
    }

}
