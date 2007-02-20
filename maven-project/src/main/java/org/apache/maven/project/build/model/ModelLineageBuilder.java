package org.apache.maven.project.build.model;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.List;

/**
 * Builds the lineage of Model instances, starting from a given POM file, and stretching back through
 * all of the parent POMs that are defined in the respective <parent/> sections.
 * 
 * NOTE: In all of the build/resume methods below, each Model MUST have its active profiles searched
 * for new repositories from which to discover parent POMs.
 */
public interface ModelLineageBuilder
{

    String ROLE = ModelLineageBuilder.class.getName();

    /**
     * Construct a lineage of the current POM plus all of its ancestors. 
     * 
     * COMING: Also, set ProjectBuildContext.currentModelLineage build-context to the result of this 
     * method before returning.
     * 
     * @param pom The current POM, whose Model will terminate the constructed lineage
     * @param localRepository The local repository against which parent POMs should be resolved
     * @param remoteRepositories List of ArtifactRepository instances against which parent POMs 
     *   should be resolved
     * @param profileManager The profile manager containing information about global profiles to be
     *   applied (from settings.xml, for instance)
     */
    ModelLineage buildModelLineage( File pom, ArtifactRepository localRepository, List remoteRepositories,
                                    ProfileManager profileManager )
        throws ProjectBuildingException;

    /**
     * Resume the process of constructing a lineage of inherited models, picking up using the deepest
     * parent already in the lineage.
     * 
     * @param lineage The ModelLineage instance in progress, which should be completed.
     * @param localRepository The local repository against which parent POMs should be resolved
     * @param profileManager The profile manager containing information about global profiles to be
     *   applied (from settings.xml, for instance)
     */
    void resumeBuildingModelLineage( ModelLineage lineage, ArtifactRepository localRepository,
                                     ProfileManager profileManager )
        throws ProjectBuildingException;

}
