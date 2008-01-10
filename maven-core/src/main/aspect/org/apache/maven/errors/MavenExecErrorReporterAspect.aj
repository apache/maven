package org.apache.maven.errors;

import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.DefaultMaven;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public aspect MavenExecErrorReporterAspect
    extends AbstractCoreReporterAspect
{

    private pointcut dm_getProjects( MavenExecutionRequest request ):
        execution( List DefaultMaven.getProjects( MavenExecutionRequest ) )
        && args( request );

    private pointcut dm_collectProjects( ArtifactRepository localRepository, ProfileManager globalProfileManager ):
        execution( List DefaultMaven.collectProjects( List, ArtifactRepository, boolean, ProfileManager, boolean ) )
        && args( *, localRepository, *, globalProfileManager, * );

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
        cflow( dm_collectProjects( ArtifactRepository, ProfileManager ) )
        && within( DefaultMaven )
        && call( MavenProject MavenProjectBuilder+.build( .. ) )
    {
        currentProject = proceed();
        return currentProject;
    }

    before( MavenExecutionException err ):
        cflow( dm_getProjects( MavenExecutionRequest ) )
        && cflow( dm_collectProjects( ArtifactRepository, ProfileManager ) )
        && execution( MavenExecutionException.new( String, File ) )
        && this( err )
    {
        getReporter().reportInvalidMavenVersion( currentProject, mavenVersion, err );
    }

    after(): dm_collectProjects( ArtifactRepository, ProfileManager )
    {
        currentProject = null;
    }

    after( File basedir, String includes, String excludes ) throwing( IOException cause ):
        cflow( dm_getProjects( MavenExecutionRequest ) )
        && cflow( execution( * DefaultMaven.getProjectFiles( MavenExecutionRequest ) ) )
        && call( * FileUtils.getFiles( File, String, String ) )
        && args( basedir, includes, excludes )
    {
        getReporter().reportPomFileScanningError( basedir, includes, excludes, cause );
    }

    after( File pomFile ) throwing( IOException cause ):
        cflow( dm_getProjects( MavenExecutionRequest ) )
        && cflow( dm_collectProjects( ArtifactRepository, ProfileManager ) )
        && call( File File.getCanonicalFile() )
        && target( pomFile )
    {
        getReporter().reportPomFileCanonicalizationError( pomFile, cause );
    }

}
