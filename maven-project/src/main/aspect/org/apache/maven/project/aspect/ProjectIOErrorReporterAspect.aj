package org.apache.maven.project.aspect;

import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.build.model.ModelAndFile;
import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public privileged aspect ProjectIOErrorReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    private pointcut pbldr_readProject( String projectId, File pomFile ):
        execution( * DefaultMavenProjectBuilder.readModel( String, File, .. ) )
        && args( projectId, pomFile, .. );

    private pointcut xppEx_handler( XmlPullParserException cause ):
        handler( XmlPullParserException )
        && args( cause );

    private pointcut ioEx_handler( IOException cause ):
        handler( IOException )
        && args( cause );

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
        cflow( pbldr_readProject( projectId, pomFile ) )
        && xppEx_handler( cause )
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
        cflow( pbldr_readProject( projectId, pomFile ) )
        && ioEx_handler( cause )
    {
        getReporter().reportErrorParsingProjectModel( projectId, pomFile, cause );
    }

    private pointcut mlbldr_resolveParentPom( ModelAndFile childInfo ):
        execution( ModelAndFile DefaultModelLineageBuilder.resolveParentPom( ModelAndFile, .. ) )
        && args( childInfo, .. );

    private pointcut mlbldr_readModel( File pomFile ):
        execution( * DefaultModelLineageBuilder.readModel( File ) )
        && args( pomFile );

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
        cflow( mlbldr_resolveParentPom( childInfo ) )
        && cflow( mlbldr_readModel( parentPomFile ) )
        && xppEx_handler( cause )
    {
        getReporter().reportErrorParsingParentProjectModel( childInfo, parentPomFile, cause );
    }

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
        cflow( mlbldr_resolveParentPom( childInfo ) )
        && cflow( mlbldr_readModel( parentPomFile ) )
        && ioEx_handler( cause )
    {
        getReporter().reportErrorParsingParentProjectModel( childInfo, parentPomFile, cause );
    }

    private pointcut mlbldr_buildModelLineage():
        execution( * DefaultModelLineageBuilder.buildModelLineage( .. ) );

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
        cflow( mlbldr_buildModelLineage() )
        && !cflowbelow( mlbldr_buildModelLineage() )
        && cflow( mlbldr_readModel( pomFile ) )
        && xppEx_handler( cause )
    {
        getReporter().reportErrorParsingProjectModel( "unknown", pomFile, cause );
    }

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
        cflow( mlbldr_buildModelLineage() )
        && !cflowbelow( mlbldr_buildModelLineage() )
        && cflow( mlbldr_readModel( pomFile ) )
        && ioEx_handler( cause )
    {
        getReporter().reportErrorParsingProjectModel( "unknown", pomFile, cause );
    }
}
