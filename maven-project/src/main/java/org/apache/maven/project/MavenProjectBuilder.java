package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.wagon.events.TransferListener;

import java.io.File;
import java.util.List;

public interface MavenProjectBuilder
{
    String ROLE = MavenProjectBuilder.class.getName();

    String STANDALONE_SUPERPOM_GROUPID = "org.apache.maven";

    String STANDALONE_SUPERPOM_ARTIFACTID = "super-pom";

    String STANDALONE_SUPERPOM_VERSION = "2.0";

    MavenProject build( File project, ArtifactRepository localRepository, ProfileManager globalProfileManager )
        throws ProjectBuildingException;

    MavenProject build( File project, ArtifactRepository localRepository, ProfileManager globalProfileManager,
                        boolean checkDistributionManagementStatus )
        throws ProjectBuildingException;

    // ----------------------------------------------------------------------
    // These methods are used by the MavenEmbedder
    // ----------------------------------------------------------------------

    MavenProject buildWithDependencies( File project, ArtifactRepository localRepository,
                                        ProfileManager globalProfileManager, TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException;

    MavenProject buildWithDependencies( File project, ArtifactRepository localRepository,
                                        ProfileManager globalProfileManager )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Build the artifact from the local repository, resolving it if necessary.
     *
     * @param artifact the artifact description
     * @param localRepository the local repository
     * @param remoteArtifactRepositories the remote repository list
     * @return the built project
     * @throws ProjectBuildingException
     */
    MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                      ArtifactRepository localRepository )
        throws ProjectBuildingException;

    /**
     * Build the artifact from the local repository, resolving it if necessary.
     *
     * @param artifact the artifact description
     * @param localRepository the local repository
     * @param remoteArtifactRepositories the remote repository list
     * @param allowStubModel return a stub if the POM is not found
     * @return the built project
     * @throws ProjectBuildingException
     */
    MavenProject buildFromRepository( Artifact artifact, List remoteArtifactRepositories,
                                      ArtifactRepository localRepository, boolean allowStubModel )
        throws ProjectBuildingException;

    /**
     * @deprecated Use {@link MavenProjectBuilder#buildStandaloneSuperProject(ProjectBuilderConfiguration)} instead.
     */
    MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository )
        throws ProjectBuildingException;

    /**
     * need to pass a profilemanager with correct context (eg. with execution properties)
     * @deprecated Use {@link MavenProjectBuilder#buildStandaloneSuperProject(ProjectBuilderConfiguration)} instead.
     */
    MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException;

    MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException;

    MavenProject build( File pom,
                        ProjectBuilderConfiguration config )
        throws ProjectBuildingException;

    MavenProject build( File pom,
                        ProjectBuilderConfiguration config,
                        boolean checkDistributionManagementStatus )
        throws ProjectBuildingException;

 // ----------------------------------------------------------------------------
 // API BELOW IS USED TO PRESERVE DYNAMISM IN THE BUILD SECTION OF THE POM.
 // ----------------------------------------------------------------------------

    /**
     * Variant of {@link MavenProjectBuilder#calculateConcreteState(MavenProject, ProjectBuilderConfiguration, boolean)}
     * which assumes that project references should be processed. This is provided for performance reasons, for cases
     * where you know all projects in the reactor will be processed, making traversal of project references unnecessary.
     */
    void calculateConcreteState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException;

    /**
     * Up to this point, the build section of the POM remains uninterpolated except for the artifact coordinates
     * it contains. This method will interpolate the build section and associated project-instance data
     * structures. Along with the {@link MavenProjectBuilder#restoreDynamicState(MavenProject, ProjectBuilderConfiguration, boolean)}
     * method, this method allows expressions in these areas of the POM and project instance to
     * be reevaluated in the event that a mojo changes one the build-path values, or a project property.
     * <br/><br/>
     * This method will process the following:
     * <ol>
     *   <li>the specified project's parent project (if not null)</li>
     *   <li>specified project</li>
     *   <li>its execution project (if not null)</li>
     *   <li>any project references (iff processReferences == true)</li>
     * </ol>
     */
    void calculateConcreteState( MavenProject project, ProjectBuilderConfiguration config, boolean processReferences )
        throws ModelInterpolationException;

//    /**
//     * Variant of {@link MavenProjectBuilder#restoreDynamicState(MavenProject, ProjectBuilderConfiguration, boolean)}
//     * which assumes that project references should be processed. This is provided for performance reasons, for cases
//     * where you know all projects in the reactor will be processed, making traversal of project references unnecessary.
//     */
//    void restoreDynamicState( MavenProject project, ProjectBuilderConfiguration config )
//        throws ModelInterpolationException;
//    
//    /**
//     * In the event that a mojo execution has changed one or more build paths, or changed the project properties,
//     * this method can restore the build section of the POM to its uninterpolated form, to allow reevaluation of
//     * any expressions that may depend on this changed information. This method will short-circuit if the project
//     * is not in a concrete state (see {@link MavenProjectBuilder#calculateConcreteState(MavenProject, ProjectBuilderConfiguration, boolean)}
//     * or if the properties and build paths of the project remain unchanged.
//     * <br/><br/>
//     * This method will process the following:
//     * <ol>
//     *   <li>the specified project's parent project (if not null)</li>
//     *   <li>specified project</li>
//     *   <li>its execution project (if not null)</li>
//     *   <li>any project references (iff processReferences == true)</li>
//     * </ol>
//     */
//    void restoreDynamicState( MavenProject project, ProjectBuilderConfiguration config, boolean processReferences )
//        throws ModelInterpolationException;
}
