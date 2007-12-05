package org.apache.maven.project.aspect;

import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.model.Parent;

import java.io.File;
import java.util.List;

public privileged aspect ProjectArtifactErrorReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    private pointcut mlbldr_resolveParentFromRepositories( Parent parentRef, ArtifactRepository localRepo,
                                                           List remoteRepos, String childId, File childPomFile ):
        execution( File DefaultModelLineageBuilder.resolveParentFromRepository( Parent, ArtifactRepository, List, String, File ) )
        && args( parentRef, localRepo, remoteRepos, childId, childPomFile );

    private pointcut mlbldr_parentArtifactNotFound( Parent parentRef, ArtifactRepository localRepo, List remoteRepos, String childId, File childPomFile, ArtifactNotFoundException cause ):
        cflow( mlbldr_resolveParentFromRepositories( parentRef, localRepo, remoteRepos, childId, childPomFile ) )
        && !cflowbelow( mlbldr_resolveParentFromRepositories( Parent, ArtifactRepository, List, String, File ) )
        && call( ProjectBuildingException.new( .., ArtifactNotFoundException ) )
        && within( DefaultModelLineageBuilder )
        && args( .., cause )
        && notWithinAspect();

    private pointcut mlbldr_parentArtifactUnresolvable( Parent parentRef, ArtifactRepository localRepo, List remoteRepos, String childId, File childPomFile, ArtifactResolutionException cause ):
        cflow( mlbldr_resolveParentFromRepositories( parentRef, localRepo, remoteRepos, childId, childPomFile ) )
        && !cflowbelow( mlbldr_resolveParentFromRepositories( Parent, ArtifactRepository, List, String, File ) )
        && call( ProjectBuildingException.new( .., ArtifactResolutionException ) )
        && within( DefaultModelLineageBuilder )
        && args( .., cause )
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
        mlbldr_parentArtifactNotFound( parentRef, localRepo, remoteRepos, childId, childPomFile, cause )
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
        mlbldr_parentArtifactUnresolvable( parentRef, localRepo, remoteRepos, childId, childPomFile, cause )
    {
        getReporter().reportParentPomArtifactUnresolvable( parentRef, localRepo, remoteRepos, childId, childPomFile, cause );
    }
}
