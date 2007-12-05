package org.apache.maven.project.aspect;

import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.model.Model;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.build.model.ModelAndFile;
import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public privileged aspect ProjectIOErrorReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    private pointcut pbldr_readModel( String projectId, File pomFile ):
        execution( Model DefaultMavenProjectBuilder.readModel( String, File, boolean ) )
        && args( projectId, pomFile, * );

    private pointcut within_pbldr_readModel( String projectId, File pomFile ):
        within( DefaultMavenProjectBuilder )
        && cflow( pbldr_readModel( projectId, pomFile ) )
        && !cflowbelow( pbldr_readModel( String, File ) )
        && notWithinAspect();

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    //     --> DefaultMavenProjectBuilder.findModelFromRepository(..) (private)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.readModel(..) (private)
    //         --> thrown XmlPullParserException
    // <------ InvalidProjectModelException
    // =========================================================================
    before( String projectId, File pomFile, XmlPullParserException cause ):
        within_pbldr_readModel( projectId, pomFile )
        && call( ProjectBuildingException.new( .., XmlPullParserException ))
        && args( .., cause )
    {
        getReporter().reportErrorParsingProjectModel( projectId, pomFile, cause );
    }

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultMavenProjectBuilder.buildFromRepository(..)
    //     --> DefaultMavenProjectBuilder.findModelFromRepository(..) (private)
    // DefaultMavenProjectBuilder.build(..)
    // --> DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
    //     --> DefaultMavenProjectBuilder.readModel(..) (private)
    //         --> thrown IOException
    // <------ InvalidProjectModelException
    // =========================================================================
    before( String projectId, File pomFile, IOException cause ):
        within_pbldr_readModel( projectId, pomFile )
        && call( ProjectBuildingException.new( .., IOException ))
        && args( .., cause )
    {
        getReporter().reportErrorParsingProjectModel( projectId, pomFile, cause );
    }

    private pointcut mlbldr_resolveParentPom( ModelAndFile childInfo ):
        execution( ModelAndFile DefaultModelLineageBuilder.resolveParentPom( ModelAndFile, .. ) )
        && args( childInfo, .. );

    private pointcut mlbldr_readModel( File pomFile ):
        execution( * DefaultModelLineageBuilder.readModel( File ) )
        && args( pomFile );

    private pointcut within_mlbldr_readModel( File pomFile ):
        cflow( mlbldr_readModel( pomFile ) )
        && within( DefaultModelLineageBuilder )
        && notWithinAspect();

    private pointcut mlbldr_errorParsingParentPom( ModelAndFile childInfo, File parentPomFile, XmlPullParserException cause ):
        cflowbelow( mlbldr_resolveParentPom( childInfo ) )
        && within_mlbldr_readModel( parentPomFile )
        && call( ProjectBuildingException.new( .., XmlPullParserException ) )
        && args( .., cause );

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultModelLineageBuilder.buildModelLineage(..)
    // --> DefaultModelLineageBuilder.resumeBuildingModelLineage(..)
    //     --> DefaultModelLineageBuilder.resolveParentPom(..) (private)
    //         [--> DefaultModelLineageBuilder.resolveParentWithRelativePath(..) (private)
    //         --> DefaultModelLineageBuilder.readModel(..) (private)
    //             --> thrown XmlPullParserException
    // <---------- ProjectBuildingException
    // =========================================================================
    before( ModelAndFile childInfo, File parentPomFile, XmlPullParserException cause ):
        mlbldr_errorParsingParentPom( childInfo, parentPomFile, cause )
    {
        getReporter().reportErrorParsingParentProjectModel( childInfo, parentPomFile, cause );
    }

    private pointcut mlbldr_errorReadingParentPom( ModelAndFile childInfo, File parentPomFile, IOException cause ):
        cflow( mlbldr_resolveParentPom( childInfo ) )
        && within_mlbldr_readModel( parentPomFile )
        && call( ProjectBuildingException.new( .., IOException ))
        && args( .., cause );

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultModelLineageBuilder.buildModelLineage(..)
    // --> DefaultModelLineageBuilder.resumeBuildingModelLineage(..)
    //     --> DefaultModelLineageBuilder.resolveParentPom(..) (private)
    //         [--> DefaultModelLineageBuilder.resolveParentWithRelativePath(..) (private)
    //         --> DefaultModelLineageBuilder.readModel(..) (private)
    //             --> thrown XmlPullParserException
    // <---------- ProjectBuildingException
    // =========================================================================
    before( ModelAndFile childInfo, File parentPomFile, IOException cause ):
        mlbldr_errorReadingParentPom( childInfo, parentPomFile, cause )
    {
        getReporter().reportErrorParsingParentProjectModel( childInfo, parentPomFile, cause );
    }

    private pointcut mlbldr_errorParsingNonParentPom( File pomFile, XmlPullParserException cause ):
        !cflow( mlbldr_resolveParentPom( ModelAndFile ) )
        && cflow( mlbldr_readModel( pomFile ) )
        && call( ProjectBuildingException.new( .., XmlPullParserException ))
        && args( .., cause )
        && within( DefaultModelLineageBuilder )
        && notWithinAspect();

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultModelLineageBuilder.buildModelLineage(..)
    //     --> DefaultModelLineageBuilder.readModel(..) (private)
    //         --> thrown XmlPullParserException
    // <------ ProjectBuildingException
    // =========================================================================
    before( File pomFile, XmlPullParserException cause ):
        mlbldr_errorParsingNonParentPom( pomFile, cause )
    {
        getReporter().reportErrorParsingProjectModel( "unknown", pomFile, cause );
    }

    private pointcut mlbldr_errorReadingNonParentPom( File pomFile, IOException cause ):
        !cflow( mlbldr_resolveParentPom( ModelAndFile ) )
        && cflow( mlbldr_readModel( pomFile ) )
        && call( ProjectBuildingException.new( .., IOException ))
        && args( .., cause )
        && within( DefaultModelLineageBuilder )
        && notWithinAspect();

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultModelLineageBuilder.buildModelLineage(..)
    //     --> DefaultModelLineageBuilder.readModel(..) (private)
    //         --> thrown XmlPullParserException
    // <------ ProjectBuildingException
    // =========================================================================
    before( File pomFile, IOException cause ):
        mlbldr_errorReadingNonParentPom( pomFile, cause )
    {
        getReporter().reportErrorParsingProjectModel( "unknown", pomFile, cause );
    }
}
