package org.apache.maven.errors;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.model.Model;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.extension.DefaultBuildExtensionScanner;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.interpolation.ModelInterpolationException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public aspect ExtensionErrorReporterAspect
    extends AbstractCoreReporterAspect
{

    before( ProjectBuildingException cause ):
        withincode( List DefaultBuildExtensionScanner.getInitialRemoteRepositories() )
        && call( ExtensionScanningException.new( String, ProjectBuildingException ) )
        && args( *, cause )
    {
        getReporter().handleSuperPomBuildingError( cause );
    }

    private pointcut within_dbes_buildModelLineage( MavenExecutionRequest request ):
        withincode( ModelLineage DefaultBuildExtensionScanner.buildModelLineage( File, MavenExecutionRequest, List ) )
        && args( *, request, * );

    before( MavenExecutionRequest request, File pomFile, ProjectBuildingException cause ):
        within_dbes_buildModelLineage( request )
        && call( ExtensionScanningException.new( String, File, ProjectBuildingException ) )
        && args( .., pomFile, cause )
    {
        getReporter().handleProjectBuildingError( request, pomFile, cause );
    }

    private pointcut within_dbes_checkModulesForExtensions():
        withincode( * DefaultBuildExtensionScanner.checkModulesForExtensions( File, Model, MavenExecutionRequest, List, List, List ) );

    before( File pomFile, IOException cause ):
        within_dbes_checkModulesForExtensions()
        && call( ExtensionScanningException.new( String, File, String, IOException ) )
        && args( *, pomFile, *, cause )
    {
        getReporter().reportPomFileCanonicalizationError( pomFile, cause );
    }

    private pointcut dbes_scanInternal( File pomFile, MavenExecutionRequest request ):
        execution( void DefaultBuildExtensionScanner.scanInternal( File, MavenExecutionRequest, .. ) )
        && args( pomFile, request, .. );

    after( File pomFile, MavenExecutionRequest request, Model model, Map inheritedValues )
    throwing( ModelInterpolationException cause ):
        cflow( dbes_scanInternal( pomFile, request ) )
        && within( DefaultBuildExtensionScanner )
        && call( Model ModelInterpolator+.interpolate( Model, Map, .. ) )
        && args( model, inheritedValues, .. )
    {
        getReporter().reportErrorInterpolatingModel( model, inheritedValues, pomFile, request, cause );
    }

    // TODO: Finish ExtensionManagerException mapping!

}
