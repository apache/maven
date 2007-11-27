package org.apache.maven.project.aspect;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.InvalidProjectVersionException;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.MavenTools;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.error.DefaultProjectErrorReporter;
import org.apache.maven.project.error.ProjectErrorReporter;

import java.io.File;
import java.util.Set;

public privileged aspect PBEDerivativeReporterAspect
{

    private ProjectErrorReporter reporter;

    public void setProjectErrorReporter( ProjectErrorReporter reporter )
    {
        this.reporter = reporter;
    }

    private ProjectErrorReporter getReporter()
    {
        if ( reporter == null )
        {
            reporter = new DefaultProjectErrorReporter();
        }

        return reporter;
    }

    // UnknownRepositoryLayoutException

    private pointcut mavenTools_buildDeploymentArtifactRepository( DeploymentRepository repo ):
        call( ArtifactRepository MavenTools+.buildDeploymentArtifactRepository( DeploymentRepository ) )
        && args( repo );

    private pointcut pbldr_processProjectLogic( MavenProject project, File pomFile ):
        execution( private MavenProject DefaultMavenProjectBuilder.processProjectLogic( MavenProject, File, .. ) )
        && args( project, pomFile, .. );

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.buildInternal(..) (private)
    //         --> DefaultMavenProjectBuilder.processProjectLogic(..) (private)
    //             --> DefaultMavenTools.buildDeploymentArtifactRepository(..)
    //             <-- UnknownRepositoryLayoutException
    // <---------- ProjectBuildingException
    // =========================================================================
    after( MavenProject project, File pomFile, DeploymentRepository repo ) throwing( UnknownRepositoryLayoutException cause ):
        mavenTools_buildDeploymentArtifactRepository( repo ) &&
        cflow( pbldr_processProjectLogic( project, pomFile ) )
    {
        getReporter().reportErrorCreatingDeploymentArtifactRepository( project, pomFile, repo, cause );
    }

    private pointcut mavenTools_buildArtifactRepository( Repository repo ):
        call( ArtifactRepository MavenTools+.buildArtifactRepository( Repository ) )
        && args( repo );

    private boolean processingPluginRepositories = false;

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.buildInternal(..) (private)
    //         --> DefaultMavenProjectBuilder.processProjectLogic(..) (private)
    //             --> DefaultMavenTools.buildArtifactRepositories(..)
    //                 --> DefaultMavenTools.buildArtifactRepository(..)
    //             <------ UnknownRepositoryLayoutException
    // <---------- ProjectBuildingException
    // =========================================================================
    after( MavenProject project, File pomFile, Repository repo ) throwing( UnknownRepositoryLayoutException cause ):
        mavenTools_buildArtifactRepository( repo ) && cflow( pbldr_processProjectLogic( project, pomFile ) )
    {
        getReporter().reportErrorCreatingArtifactRepository( project, pomFile, repo, cause, processingPluginRepositories );
    }

    after():
        call( * Model+.getPluginRepositories() )
    {
        processingPluginRepositories = true;
    }

    after():
        call( * Model+.getRepositories() )
    {
        processingPluginRepositories = false;
    }

    after( MavenProject project, File pomFile ): pbldr_processProjectLogic( project, pomFile )
    {
        processingPluginRepositories = false;
    }

    // ModelInterpolationException

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.buildInternal(..) (private)
    //         --> DefaultMavenProjectBuilder.processProjectLogic(..) (private)
    //             --> ModelInterpolator.interpolate(..)
    //             <-- ModelInterpolationException
    // <---------- ProjectBuildingException
    // =========================================================================
    after( MavenProject project, File pomFile ) throwing( ModelInterpolationException cause ):
        pbldr_processProjectLogic( project, pomFile )
    {
        getReporter().reportErrorInterpolatingModel( project, pomFile, cause );
    }

    // InvalidProjectVersionException

    private pointcut pbldr_createNonDependencyArtifacts():
        call( protected * DefaultMavenProjectBuilder.createPluginArtifacts( .. ) )
        || call( protected * DefaultMavenProjectBuilder.createReportArtifacts( .. ) )
        || call( protected * DefaultMavenProjectBuilder.createExtensionArtifacts( .. ) );

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.buildInternal(..) (private)
    //         --> DefaultMavenProjectBuilder.processProjectLogic(..) (private)
    //             --> DefaultMavenProjectBuilder.createPluginArtifacts(..)
    //             --> DefaultMavenProjectBuilder.createReportArtifacts(..)
    //             --> DefaultMavenProjectBuilder.createExtensionArtifacts(..)
    //             <-- InvalidProjectVersionException
    // <---------- ProjectBuildingException
    // =========================================================================
    after( MavenProject project, File pomFile ) throwing( ProjectBuildingException cause ):
        cflow( pbldr_processProjectLogic( project, pomFile ) )
        && pbldr_createNonDependencyArtifacts()
    {
        if ( cause instanceof InvalidProjectVersionException )
        {
            getReporter().reportBadNonDependencyProjectArtifactVersion( project, pomFile, (InvalidProjectVersionException) cause );
        }
    }

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.buildInternal(..) (private)
    //         --> DefaultMavenProjectBuilder.processProjectLogic(..) (private)
    //             --> (model validator result)
    //         <-- InvalidProjectModelException
    // <------ ProjectBuildingException
    // =========================================================================
    after( MavenProject project, File pomFile ) throwing( InvalidProjectModelException cause ):
        cflow( pbldr_processProjectLogic( project, pomFile ) )
        && execution( void DefaultMavenProjectBuilder.validateModel( .. ) )
    {
        getReporter().reportProjectValidationFailure( project, pomFile, cause );
    }


    // InvalidDependencyVersionException

    private pointcut pbldr_buildInternal():
        execution( * DefaultMavenProjectBuilder.buildInternal( .. ) );

    private MavenProject projectBeingBuilt;

    after( MavenProject project ):
        cflow( pbldr_buildInternal() )
        && !cflowbelow( pbldr_buildInternal() )
        && call( DependencyManagement MavenProject.getDependencyManagement() )
        && target( project )
    {
        projectBeingBuilt = project;
    }

    after(): pbldr_buildInternal()
    {
        projectBeingBuilt = null;
    }

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.buildInternal(..) (private)
    //         --> DefaultMavenProjectBuilder.createManagedVersionMap(..) (private)
    //         <-- InvalidDependencyVersionException
    // <------ ProjectBuildingException
    // =========================================================================
    after( File pomFile ) throwing( ProjectBuildingException cause ):
        cflow( pbldr_buildInternal() )
        && execution( * DefaultMavenProjectBuilder.createManagedVersionMap( .., File ) )
        && args( .., pomFile )
    {
        if ( cause instanceof InvalidDependencyVersionException )
        {
            getReporter().reportBadManagedDependencyVersion( projectBeingBuilt, pomFile, (InvalidDependencyVersionException) cause );
        }
    }

    protected pointcut mms_createArtifacts( MavenProject project ):
        call( public static Set MavenMetadataSource.createArtifacts( .., MavenProject ) )
        && args( .., project );

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> MavenProject.createArtifacts(..)
    //     --> MavenMetadataSource.createArtifacts(..)
    //     <-- InvalidDependencyVersionException
    // <-- ProjectBuildingException
    // =========================================================================
    after( MavenProject project ) throwing( InvalidDependencyVersionException cause ):
        cflow( execution( * MavenProject.createArtifacts( .. ) ) )
        && mms_createArtifacts( project )
    {
        getReporter().reportBadDependencyVersion( project, project.getFile(), cause );
    }

}
