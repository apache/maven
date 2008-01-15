package org.apache.maven.error;

import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.extension.ExtensionManagerException;
import org.apache.maven.lifecycle.LifecycleException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.activation.ProfileActivator;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.InvalidProjectVersionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.build.model.ModelAndFile;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.realm.RealmManagementException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DummyCoreErrorReporter
    extends DefaultCoreErrorReporter
{

    public void handleSuperPomBuildingError( ProjectBuildingException exception )
    {


    }

    public void reportAggregatedMojoFailureException( MavenSession session,
                                                      MojoBinding binding,
                                                      MojoFailureException cause )
    {


    }

    public void reportAttemptToOverrideUneditableMojoParameter( Parameter currentParameter,
                                                                MojoBinding binding,
                                                                MavenProject project,
                                                                MavenSession session,
                                                                MojoExecution exec,
                                                                PathTranslator translator,
                                                                Logger logger,
                                                                PluginConfigurationException cause )
    {


    }

    public void reportErrorApplyingMojoConfiguration( MojoBinding binding,
                                                      MavenProject project,
                                                      PlexusConfiguration config,
                                                      PluginConfigurationException cause )
    {


    }

    public void reportErrorConfiguringExtensionPluginRealm( Plugin plugin,
                                                            Model originModel,
                                                            List remoteRepos,
                                                            MavenExecutionRequest request,
                                                            PluginManagerException cause )
    {


    }

    public void reportErrorFormulatingBuildPlan( List tasks,
                                                 MavenProject configuringProject,
                                                 String targetDescription,
                                                 LifecycleException cause )
    {


    }

    public void reportErrorInterpolatingModel( Model model,
                                               Map inheritedValues,
                                               File pomFile,
                                               MavenExecutionRequest request,
                                               ModelInterpolationException cause )
    {


    }

    public void reportErrorLoadingPlugin( MojoBinding binding,
                                          MavenProject project,
                                          PluginLoaderException cause )
    {


    }

    public void reportErrorManagingRealmForExtension( Artifact extensionArtifact,
                                                      Artifact projectArtifact,
                                                      List remoteRepos,
                                                      MavenExecutionRequest request,
                                                      RealmManagementException cause )
    {


    }

    public void reportErrorManagingRealmForExtensionPlugin( Plugin plugin,
                                                            Model originModel,
                                                            List remoteRepos,
                                                            MavenExecutionRequest request,
                                                            RealmManagementException cause )
    {


    }

    public void reportErrorResolvingExtensionDependencies( Artifact extensionArtifact,
                                                           Artifact projectArtifact,
                                                           List remoteRepos,
                                                           MavenExecutionRequest request,
                                                           ArtifactResolutionResult resolutionResult,
                                                           ExtensionManagerException err )
    {


    }

    public void reportErrorResolvingExtensionDirectDependencies( Artifact extensionArtifact,
                                                                 Artifact projectArtifact,
                                                                 List remoteRepos,
                                                                 MavenExecutionRequest request,
                                                                 ArtifactMetadataRetrievalException cause )
    {


    }

    public void reportErrorSearchingforCompatibleExtensionPluginVersion( Plugin plugin,
                                                                         Model originModel,
                                                                         List remoteRepos,
                                                                         MavenExecutionRequest request,
                                                                         String requiredMavenVersion,
                                                                         String currentMavenVersion,
                                                                         InvalidVersionSpecificationException cause )
    {


    }

    public void reportExtensionPluginArtifactNotFound( Plugin plugin,
                                                       Model originModel,
                                                       List remoteRepos,
                                                       MavenExecutionRequest request,
                                                       PluginNotFoundException cause )
    {


    }

    public void reportExtensionPluginVersionNotFound( Plugin plugin,
                                                      Model originModel,
                                                      List remoteRepos,
                                                      MavenExecutionRequest request,
                                                      PluginVersionNotFoundException cause )
    {


    }

    public void reportIncompatibleMavenVersionForExtensionPlugin( Plugin plugin,
                                                                  Model originModel,
                                                                  List remoteRepos,
                                                                  MavenExecutionRequest request,
                                                                  String requiredMavenVersion,
                                                                  String currentMavenVersion,
                                                                  PluginVersionResolutionException err )
    {


    }

    public void reportInvalidDependencyVersionInExtensionPluginPOM( Plugin plugin,
                                                                    Model originModel,
                                                                    List remoteRepos,
                                                                    MavenExecutionRequest request,
                                                                    InvalidDependencyVersionException cause )
    {


    }

    public void reportInvalidMavenVersion( MavenProject project,
                                           ArtifactVersion mavenVersion,
                                           MavenExecutionException err )
    {


    }

    public void reportInvalidPluginExecutionEnvironment( MojoBinding binding,
                                                         MavenProject project,
                                                         PluginExecutionException cause )
    {


    }

    public void reportLifecycleLoaderErrorWhileValidatingTask( MavenSession session,
                                                               MavenProject rootProject,
                                                               LifecycleLoaderException cause,
                                                               TaskValidationResult result )
    {


    }

    public void reportLifecycleSpecErrorWhileValidatingTask( MavenSession session,
                                                             MavenProject rootProject,
                                                             LifecycleSpecificationException cause,
                                                             TaskValidationResult result )
    {


    }

    public void reportMissingArtifactWhileAddingExtensionPlugin( Plugin plugin,
                                                                 Model originModel,
                                                                 List remoteRepos,
                                                                 MavenExecutionRequest request,
                                                                 ArtifactNotFoundException cause )
    {


    }

    public void reportMissingPluginDescriptor( MojoBinding binding,
                                               MavenProject project,
                                               LifecycleExecutionException err )
    {


    }

    public void reportMissingRequiredMojoParameter( MojoBinding binding,
                                                    MavenProject project,
                                                    List invalidParameters,
                                                    PluginParameterException err )
    {


    }

    public void reportMojoExecutionException( MojoBinding binding,
                                              MavenProject project,
                                              MojoExecutionException cause )
    {


    }

    public void reportMojoLookupError( MojoBinding binding,
                                       MavenProject project,
                                       ComponentLookupException cause )
    {


    }

    public void reportNoGoalsSpecifiedException( MavenProject rootProject,
                                                 NoGoalsSpecifiedException error )
    {


    }

    public void reportPluginErrorWhileValidatingTask( MavenSession session,
                                                      MavenProject rootProject,
                                                      PluginLoaderException cause,
                                                      TaskValidationResult result )
    {


    }

    public void reportPomFileCanonicalizationError( File pomFile,
                                                    IOException cause )
    {


    }

    public void reportPomFileScanningError( File basedir,
                                            String includes,
                                            String excludes,
                                            IOException cause )
    {


    }

    public void reportProjectCycle( ProjectCycleException error )
    {


    }

    public void reportProjectDependenciesNotFound( MavenProject project,
                                                   String scope,
                                                   ArtifactNotFoundException cause )
    {


    }

    public void reportProjectDependenciesUnresolvable( MavenProject project,
                                                       String scope,
                                                       ArtifactResolutionException cause )
    {


    }

    public void reportProjectDependencyArtifactNotFound( MavenProject project,
                                                         Artifact artifact,
                                                         ArtifactNotFoundException cause )
    {


    }

    public void reportProjectDependencyArtifactUnresolvable( MavenProject project,
                                                             Artifact artifact,
                                                             ArtifactResolutionException cause )
    {


    }

    public void reportProjectMojoFailureException( MavenSession session,
                                                   MojoBinding binding,
                                                   MojoFailureException cause )
    {


    }

    public void reportReflectionErrorWhileEvaluatingMojoParameter( Parameter currentParameter,
                                                                   MojoBinding binding,
                                                                   MavenProject project,
                                                                   String expression,
                                                                   Exception cause )
    {


    }

    public void reportUnresolvableArtifactWhileAddingExtensionPlugin( Plugin plugin,
                                                                      Model originModel,
                                                                      List remoteRepos,
                                                                      MavenExecutionRequest request,
                                                                      ArtifactResolutionException cause )
    {


    }

    public void reportUnresolvableExtensionPluginPOM( Plugin plugin,
                                                      Model originModel,
                                                      List remoteRepos,
                                                      MavenExecutionRequest request,
                                                      ArtifactMetadataRetrievalException cause )
    {


    }

    public void reportUseOfBannedMojoParameter( Parameter currentParameter,
                                                MojoBinding binding,
                                                MavenProject project,
                                                String expression,
                                                String altExpression,
                                                ExpressionEvaluationException err )
    {


    }

    public void reportActivatorError( ProfileActivator activator,
                                      String projectId,
                                      File pomFile,
                                      Profile profile,
                                      ProfileActivationContext context,
                                      ProfileActivationException cause )
    {


    }

    public void reportActivatorLookupError( String projectId,
                                            File pomFile,
                                            Profile profile,
                                            ComponentLookupException cause )
    {


    }

    public void reportBadDependencyVersion( MavenProject project,
                                            File pomFile,
                                            InvalidDependencyVersionException cause )
    {


    }

    public void reportBadManagedDependencyVersion( MavenProject projectBeingBuilt,
                                                   File pomFile,
                                                   InvalidDependencyVersionException cause )
    {


    }

    public void reportBadNonDependencyProjectArtifactVersion( MavenProject project,
                                                              File pomFile,
                                                              InvalidProjectVersionException cause )
    {


    }

    public void reportErrorCreatingArtifactRepository( MavenProject project,
                                                       File pomFile,
                                                       Repository repo,
                                                       UnknownRepositoryLayoutException cause,
                                                       boolean isPluginRepo )
    {


    }

    public void reportErrorCreatingDeploymentArtifactRepository( MavenProject project,
                                                                 File pomFile,
                                                                 DeploymentRepository repo,
                                                                 UnknownRepositoryLayoutException cause )
    {


    }

    public void reportErrorInterpolatingModel( MavenProject project,
                                               File pomFile,
                                               ModelInterpolationException cause )
    {


    }

    public void reportErrorLoadingExternalProfilesFromFile( Model model,
                                                            File pomFile,
                                                            File projectDir,
                                                            IOException cause )
    {


    }

    public void reportErrorLoadingExternalProfilesFromFile( Model model,
                                                            File pomFile,
                                                            File projectDir,
                                                            XmlPullParserException cause )
    {


    }

    public void reportErrorParsingParentProjectModel( ModelAndFile childInfo,
                                                      File parentPomFile,
                                                      XmlPullParserException cause )
    {


    }

    public void reportErrorParsingParentProjectModel( ModelAndFile childInfo,
                                                      File parentPomFile,
                                                      IOException cause )
    {


    }

    public void reportErrorParsingProjectModel( String projectId,
                                                File pomFile,
                                                XmlPullParserException cause )
    {


    }

    public void reportErrorParsingProjectModel( String projectId,
                                                File pomFile,
                                                IOException cause )
    {


    }

    public void reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( Repository repo,
                                                                             String projectId,
                                                                             File pomFile,
                                                                             InvalidRepositoryException cause )
    {


    }

    public void reportParentPomArtifactNotFound( Parent parentRef,
                                                 ArtifactRepository localRepo,
                                                 List remoteRepos,
                                                 String childId,
                                                 File childPomFile,
                                                 ArtifactNotFoundException cause )
    {


    }

    public void reportParentPomArtifactUnresolvable( Parent parentRef,
                                                     ArtifactRepository localRepo,
                                                     List remoteRepos,
                                                     String childId,
                                                     File childPomFile,
                                                     ArtifactResolutionException cause )
    {


    }

    public void reportProjectCollision( List allProjectInstances,
                                        DuplicateProjectException err )
    {


    }

    public void reportProjectValidationFailure( MavenProject project,
                                                File pomFile,
                                                InvalidProjectModelException error )
    {


    }

}
