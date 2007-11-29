package org.apache.maven.project.aspect;

import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Parent;

import java.io.File;
import java.util.List;

public privileged aspect ProjectArtifactErrorReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    private pointcut mlbldr_resolveParentFromRepositories( Parent parentRef, ArtifactRepository localRepo,
                                                           List remoteRepos, String childId, File childPomFile ):
        execution( File DefaultModelLineageBuilder.resolveParentFromRepository( Parent, ArtifactRepository, List, String, File ) )
        && args( parentRef, localRepo, remoteRepos, childId, childPomFile )
        && notWithinAspect();

    private pointcut anfe_handler( ArtifactNotFoundException cause ):
        handler( ArtifactNotFoundException )
        && args( cause )
        && notWithinAspect();

    private pointcut are_handler( ArtifactResolutionException cause ):
        handler( ArtifactResolutionException )
        && args( cause )
        && notWithinAspect();

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultModelLineageBuilder.buildModelLineage(..)
    //     --> DefaultModelLineageBuilder.resolveParentPom(..) (private)
    //         --> DefaultModelLineageBuilder.resolveParentFromRepository(..) (private)
    //             --> thrown ArtifactNotFoundException
    // <---------- ProjectBuildingException
    // =========================================================================
    before( Parent parentRef, ArtifactRepository localRepo, List remoteRepos, String childId, File childPomFile, ArtifactNotFoundException cause ):
        cflow( mlbldr_resolveParentFromRepositories( parentRef, localRepo, remoteRepos, childId, childPomFile ) )
        && anfe_handler( cause )
    {
        getReporter().reportParentPomArtifactNotFound( parentRef, localRepo, remoteRepos, childId, childPomFile, cause );
    }

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // ...
    // --> DefaultModelLineageBuilder.buildModelLineage(..)
    //     --> DefaultModelLineageBuilder.resolveParentPom(..) (private)
    //         --> DefaultModelLineageBuilder.resolveParentFromRepository(..) (private)
    //             --> thrown ArtifactResolutionException
    // <---------- ProjectBuildingException
    // =========================================================================
    before( Parent parentRef, ArtifactRepository localRepo, List remoteRepos, String childId, File childPomFile, ArtifactResolutionException cause ):
        cflow( mlbldr_resolveParentFromRepositories( parentRef, localRepo, remoteRepos, childId, childPomFile ) )
        && are_handler( cause )
    {
        getReporter().reportParentPomArtifactUnresolvable( parentRef, localRepo, remoteRepos, childId, childPomFile, cause );
    }
}
