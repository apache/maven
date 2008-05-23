package org.apache.maven.project.error;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.activation.ProfileActivator;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.InvalidProjectVersionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.build.model.ModelAndFile;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ProjectErrorReporter
{

    void clearErrors();

    String getFormattedMessage( Throwable error );

    Throwable getRealCause( Throwable error );

    List getReportedExceptions();

    Throwable findReportedException( Throwable error );

    boolean isStackTraceRecommended( Throwable error );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * DefaultProfileAdvisor.applyActivatedProfiles(..)
     * DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
     * --&gt; DefaultProfileAdvisor.applyActivatedProfiles(..) (private)
     * --&gt; DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
     *     --&gt; DefaultProfileManager.getActiveProfiles(..)
     *         --&gt; DefaultProfileManager.isActive(..) (private)
     *             --&gt; ProfileActivator.canDetermineActivation(..)
     *             --&gt; ProfileActivator.isActive(..)
     *         &lt;------ ProfileActivationException
     * &lt;------ ProjectBuildingException
     * </pre>
     */
    void reportActivatorError( ProfileActivator activator,
                               Model model,
                               File pomFile,
                               Profile profile,
                               ProfileActivationContext context,
                               ProfileActivationException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * DefaultProfileAdvisor.applyActivatedProfiles(..)
     * DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
     * --&gt; DefaultProfileAdvisor.applyActivatedProfiles(..) (private)
     * --&gt; DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
     *     --&gt; DefaultProfileManager.getActiveProfiles(..)
     *         --&gt; DefaultProfileManager.isActive(..) (private)
     *             --&gt; PlexusContainer.lookupList(..)
     *             &lt;-- ComponentLookupException
     *         &lt;-- ProfileActivationException
     * &lt;------ ProjectBuildingException
     * </pre>
     */
    void reportActivatorLookupError( Model model,
                                     File pomFile,
                                     Profile profile,
                                     ProfileActivationContext context,
                                     ComponentLookupException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * DefaultProfileAdvisor.applyActivatedProfiles(..)
     * DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
     * --&gt; DefaultProfileAdvisor.buildProfileManager(..) (private)
     *     --&gt; DefaultProfileAdvisor.loadExternalProjectProfiles(..) (private)
     *         --&gt; MavenProfilesBuilder.buildProfiles(..)
     *         &lt;-- IOException
     * &lt;------ ProjectBuildingException
     * </pre>
     */
    void reportErrorLoadingExternalProfilesFromFile( Model model,
                                                     File pomFile,
                                                     File projectDir,
                                                     IOException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * DefaultProfileAdvisor.applyActivatedProfiles(..)
     * DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
     * --&gt; DefaultProfileAdvisor.buildProfileManager(..) (private)
     *     --&gt; DefaultProfileAdvisor.loadExternalProjectProfiles(..) (private)
     *         --&gt; MavenProfilesBuilder.buildProfiles(..)
     *         &lt;-- XmlPullParserException
     * &lt;------ ProjectBuildingException
     * </pre>
     */
    void reportErrorLoadingExternalProfilesFromFile( Model model,
                                                     File pomFile,
                                                     File projectDir,
                                                     XmlPullParserException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * DefaultProfileAdvisor.applyActivatedProfiles(..)
     * DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
     * --&gt; DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
     *     --&gt; MavenTools.buildArtifactRepository(..)
     *     &lt;-- InvalidRepositoryException
     * &lt;-- ProjectBuildingException
     * </pre>
     */
    void reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( Repository repo,
                                                                      Model model,
                                                                      File pomFile,
                                                                      InvalidRepositoryException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *  DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.buildInternal(..) (private)
     *         --&gt; DefaultMavenProjectBuilder.processProjectLogic(..) (private)
     *             --&gt; DefaultMavenTools.buildDeploymentArtifactRepository(..)
     *             &lt;-- UnknownRepositoryLayoutException
     * &lt;-------- ProjectBuildingException
     * </pre>
     */
    void reportErrorCreatingDeploymentArtifactRepository( MavenProject project,
                                                          File pomFile,
                                                          DeploymentRepository repo,
                                                          InvalidRepositoryException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *  DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.buildInternal(..) (private)
     *         --&gt; DefaultMavenProjectBuilder.processProjectLogic(..) (private)
     *             --&gt; DefaultMavenTools.buildArtifactRepositories(..)
     *                 --&gt; DefaultMavenTools.buildArtifactRepository(..)
     *             &lt;------ UnknownRepositoryLayoutException
     * &lt;---------- ProjectBuildingException
     * </pre>
     */
    void reportErrorCreatingArtifactRepository( String projectId,
                                                File pomFile,
                                                Repository repo,
                                                InvalidRepositoryException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *  DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.buildInternal(..) (private)
     *         --&gt; DefaultMavenProjectBuilder.processProjectLogic(..) (private)
     *             --&gt; ModelInterpolator.interpolate(..)
     *             &lt;-- ModelInterpolationException
     * &lt;---------- ProjectBuildingException
     * </pre>
     */
    void reportErrorInterpolatingModel( MavenProject project,
                                        File pomFile,
                                        ModelInterpolationException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *  DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.buildInternal(..) (private)
     *         --&gt; DefaultMavenProjectBuilder.processProjectLogic(..) (private)
     *             --&gt; DefaultMavenProjectBuilder.createPluginArtifacts(..)
     *             --&gt; DefaultMavenProjectBuilder.createReportArtifacts(..)
     *             --&gt; DefaultMavenProjectBuilder.createExtensionArtifacts(..)
     *             &lt;-- InvalidProjectVersionException
     * &lt;---------- ProjectBuildingException
     * </pre>
     */
    void reportBadNonDependencyProjectArtifactVersion( MavenProject project,
                                                       File pomFile,
                                                       InvalidProjectVersionException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *  DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.buildInternal(..) (private)
     *         --&gt; DefaultMavenProjectBuilder.processProjectLogic(..) (private)
     *             --&gt; (model validator result)
     *         &lt;-- InvalidProjectModelException
     * &lt;---------- ProjectBuildingException
     * </pre>
     */
    void reportProjectValidationFailure( MavenProject project,
                                         File pomFile,
                                         InvalidProjectModelException error );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *  DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.buildInternal(..) (private)
     *         --&gt; DefaultMavenProjectBuilder.createManagedVersionMap(..) (private)
     *         &lt;-- InvalidDependencyVersionException
     * &lt;---------- ProjectBuildingException
     * </pre>
     */
    void reportBadManagedDependencyVersion( MavenProject projectBeingBuilt,
                                            File pomFile,
                                            InvalidDependencyVersionException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; MavenProject.createArtifacts(..)
     *     --&gt; MavenMetadataSource.createArtifacts(..)
     *     &lt;-- InvalidDependencyVersionException
     * &lt;-- ProjectBuildingException
     * </pre>
     */
    void reportBadDependencyVersion( MavenProject project,
                                     File pomFile,
                                     InvalidDependencyVersionException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *     --&gt; DefaultMavenProjectBuilder.findModelFromRepository(..) (private)
     * DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.readModel(..) (private)
     *         --&gt; thrown XmlPullParserException
     * &lt;------ InvalidProjectModelException
     * </pre>
     */
    void reportErrorParsingProjectModel( String projectId,
                                         File pomFile,
                                         XmlPullParserException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultModelLineageBuilder.buildModelLineage(..)
     *     --&gt; DefaultModelLineageBuilder.readModel(..) (private)
     *         --&gt; thrown XmlPullParserException
     * &lt;------ ProjectBuildingException
     * </pre>
     */
    void reportErrorParsingParentProjectModel( ModelAndFile childInfo,
                                               File parentPomFile,
                                               XmlPullParserException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultMavenProjectBuilder.buildFromRepository(..)
     *     --&gt; DefaultMavenProjectBuilder.findModelFromRepository(..) (private)
     * DefaultMavenProjectBuilder.build(..)
     * --&gt; DefaultMavenProjectBuilder.buildFromSourceFileInternal(..) (private)
     *     --&gt; DefaultMavenProjectBuilder.readModel(..) (private)
     *         --&gt; thrown IOException
     * &lt;------ InvalidProjectModelException
     * </pre>
     */
    void reportErrorParsingProjectModel( String projectId,
                                         File pomFile,
                                         IOException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultModelLineageBuilder.buildModelLineage(..)
     *     --&gt; DefaultModelLineageBuilder.readModel(..) (private)
     *         --&gt; thrown IOException
     * &lt;------ ProjectBuildingException
     * </pre>
     */
    void reportErrorParsingParentProjectModel( ModelAndFile childInfo,
                                               File parentPomFile,
                                               IOException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultModelLineageBuilder.buildModelLineage(..)
     *     --&gt; DefaultModelLineageBuilder.resolveParentPom(..) (private)
     *         --&gt; DefaultModelLineageBuilder.resolveParentFromRepository(..) (private)
     *             --&gt; thrown ArtifactNotFoundException
     * &lt;---------- ProjectBuildingException
     * </pre>
     */
    void reportParentPomArtifactNotFound( Parent parentRef,
                                          ProjectBuilderConfiguration config,
                                          List remoteRepos,
                                          String childId,
                                          File childPomFile,
                                          ArtifactNotFoundException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * ...
     * --&gt; DefaultModelLineageBuilder.buildModelLineage(..)
     *     --&gt; DefaultModelLineageBuilder.resolveParentPom(..) (private)
     *         --&gt; DefaultModelLineageBuilder.resolveParentFromRepository(..) (private)
     *             --&gt; thrown ArtifactResolutionException
     * &lt;---------- ProjectBuildingException
     * </pre>
     */
    void reportParentPomArtifactUnresolvable( Parent parentRef,
                                              ProjectBuilderConfiguration config,
                                              List remoteRepos,
                                              String childId,
                                              File childPomFile,
                                              ArtifactResolutionException cause );

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * MavenEmbedder.execute(MavenExecutionRequest)
     * MavenEmbedder.readProjectWithDependencies(MavenExecutionRequest)
     * --&gt; DefaultMaven.execute(MavenExecutionRequest)
     *        --&gt; DefaultMaven.createReactorManager(MavenExecutionRequest, MavenExecutionResult)
     *               --&gt; new ReactorManager(List, String)
     *                      --&gt; new ProjectSorter(List)
     * &lt;----------------------- DuplicateProjectException
     * </pre>
     */
    void reportProjectCollision( List allProjectInstances,
                                 DuplicateProjectException err );
}
