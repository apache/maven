package org.apache.maven.project.aspect;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.profiles.build.DefaultProfileAdvisor;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.InvalidProjectVersionException;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.MavenTools;
import org.apache.maven.DefaultMavenTools;
import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.Set;
import java.util.List;

public privileged aspect PBEDerivativeReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    // UnknownRepositoryLayoutException

    private pointcut mavenTools_buildDeploymentArtifactRepository( DeploymentRepository repo ):
        call( ArtifactRepository MavenTools+.buildDeploymentArtifactRepository( DeploymentRepository ) )
        && args( repo );

    private pointcut pbldr_processProjectLogic( MavenProject project, File pomFile ):
        execution( private MavenProject DefaultMavenProjectBuilder.processProjectLogic( MavenProject, File, .. ) )
        && args( project, pomFile, .. );

    private pointcut within_pbldr_processProjectLogic( MavenProject project, File pomFile ):
        withincode( private MavenProject DefaultMavenProjectBuilder.processProjectLogic( MavenProject, File, .. ) )
        && args( project, pomFile, .. );

    private pointcut within_DefaultMavenProjectBuilder():
        !withincode( * DefaultProfileAdvisor.*( .. ) )
        && notWithinAspect();

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

    after( MavenProject project, File pomFile, DeploymentRepository repo ) throwing( InvalidRepositoryException cause ):
        mavenTools_buildDeploymentArtifactRepository( repo ) &&
        cflow( pbldr_processProjectLogic( project, pomFile ) )
        && within_DefaultMavenProjectBuilder()
    {
        getReporter().reportErrorCreatingDeploymentArtifactRepository( project, pomFile, repo, cause );
    }

    private pointcut mavenTools_buildArtifactRepository( Repository repo ):
        execution( ArtifactRepository MavenTools+.buildArtifactRepository( Repository ) )
        && args( repo );

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
    after( MavenProject project, File pomFile, Repository repo ) throwing( InvalidRepositoryException cause ):
        mavenTools_buildArtifactRepository( repo )
        && cflow( pbldr_processProjectLogic( project, pomFile ) )
    {
        getReporter().reportErrorCreatingArtifactRepository( project.getId(), pomFile, repo, cause );
    }

    private pointcut mlbldr_updateRepositorySet( Model model, File pomFile ):
        execution( List DefaultModelLineageBuilder.updateRepositorySet( Model, *, File, .. ) )
        && args( model, *, pomFile, .. );

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultModelLineageBuilder.buildModelLineage(..)
    // --> DefaultModelLineageBuilder.resumeBuildingModelLineage(..)
    //     --> DefaultModelLineageBuilder.updateRepositorySet(..) (private)
    //         --> DefaultMavenTools.buildArtifactRepositories(..)
    //             --> DefaultMavenTools.buildArtifactRepository(..)
    //         <------ UnknownRepositoryLayoutException
    // <------ ProjectBuildingException
    // =========================================================================
    after( Model model, File pomFile, Repository repo ) throwing( InvalidRepositoryException cause ):
        mavenTools_buildArtifactRepository( repo )
        && cflow( mlbldr_updateRepositorySet( model, pomFile ) )
    {
        getReporter().reportErrorCreatingArtifactRepository( model.getId(), pomFile, repo, cause );
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
        ( call( protected * DefaultMavenProjectBuilder.createPluginArtifacts( .. ) )
        || call( protected * DefaultMavenProjectBuilder.createReportArtifacts( .. ) )
        || call( protected * DefaultMavenProjectBuilder.createExtensionArtifacts( .. ) ) )
        && notWithinAspect();

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
        && within_DefaultMavenProjectBuilder()
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
        && within_DefaultMavenProjectBuilder()
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
        && within_DefaultMavenProjectBuilder()
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
        && within_DefaultMavenProjectBuilder()
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
        && args( .., project )
        && notWithinAspect();

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
