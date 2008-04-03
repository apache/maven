package org.apache.maven.project.aspect;

import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.model.Parent;

import java.io.File;
import java.util.List;

public privileged aspect ProjectArtifactErrorReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    private pointcut mlbldr_resolveParentFromRepositories( Parent parentRef, ProjectBuilderConfiguration config,
                                                           List remoteRepos, String childId, File childPomFile ):
        execution( private File DefaultModelLineageBuilder.resolveParentFromRepositories( Parent, ProjectBuilderConfiguration, List, String, File ) )
        && args( parentRef, config, remoteRepos, childId, childPomFile );

    private pointcut mlbldr_parentArtifactNotFound( Parent parentRef, ProjectBuilderConfiguration config, List remoteRepos, String childId, File childPomFile, ArtifactNotFoundException cause ):
        cflow( mlbldr_resolveParentFromRepositories( parentRef, config, remoteRepos, childId, childPomFile ) )
        && call( ProjectBuildingException.new( .., ArtifactNotFoundException ) )
        && within( DefaultModelLineageBuilder )
        && args( .., cause )
        && notWithinAspect();

    private pointcut mlbldr_parentArtifactUnresolvable( Parent parentRef, ProjectBuilderConfiguration config, List remoteRepos, String childId, File childPomFile, ArtifactResolutionException cause ):
        cflow( mlbldr_resolveParentFromRepositories( parentRef, config, remoteRepos, childId, childPomFile ) )
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
    before( Parent parentRef, ProjectBuilderConfiguration config, List remoteRepos, String childId, File childPomFile, ArtifactNotFoundException cause ):
        mlbldr_parentArtifactNotFound( parentRef, config, remoteRepos, childId, childPomFile, cause )
    {
        getReporter().reportParentPomArtifactNotFound( parentRef, config, remoteRepos, childId, childPomFile, cause );
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
    before( Parent parentRef, ProjectBuilderConfiguration config, List remoteRepos, String childId, File childPomFile, ArtifactResolutionException cause ):
        mlbldr_parentArtifactUnresolvable( parentRef, config, remoteRepos, childId, childPomFile, cause )
    {
        getReporter().reportParentPomArtifactUnresolvable( parentRef, config, remoteRepos, childId, childPomFile, cause );
    }
}
