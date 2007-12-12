package org.apache.maven.errors;

import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RealmManagementException;
import org.apache.maven.extension.ExtensionManagerException;
import org.apache.maven.lifecycle.LifecycleException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
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
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CoreErrorReporter
{

    String NEWLINE = "\n";

    void clearErrors();

    String getFormattedMessage( Throwable error );

    Throwable getRealCause( Throwable error );

    Throwable findReportedException( Throwable error );

    void reportNoGoalsSpecifiedException( MavenProject rootProject, NoGoalsSpecifiedException error );

    void reportAggregatedMojoFailureException( MavenSession session, MojoBinding binding, MojoFailureException cause );

    void reportProjectMojoFailureException( MavenSession session, MojoBinding binding, MojoFailureException cause );

    void reportProjectCycle( ProjectCycleException error );

    void reportPluginErrorWhileValidatingTask( MavenSession session, MavenProject rootProject, PluginLoaderException cause, TaskValidationResult result );

    void reportLifecycleSpecErrorWhileValidatingTask( MavenSession session, MavenProject rootProject, LifecycleSpecificationException cause, TaskValidationResult result );

    void reportLifecycleLoaderErrorWhileValidatingTask( MavenSession session, MavenProject rootProject, LifecycleLoaderException cause, TaskValidationResult result );

    void reportMissingPluginDescriptor( MojoBinding binding, MavenProject project, LifecycleExecutionException err );

    void reportMojoExecutionException( MojoBinding binding, MavenProject project, MojoExecutionException cause );

    void reportInvalidPluginExecutionEnvironment( MojoBinding binding, MavenProject project, PluginExecutionException cause );

    void reportMojoLookupError( MojoBinding binding, MavenProject project, ComponentLookupException cause );

    void reportAttemptToOverrideUneditableMojoParameter( Parameter currentParameter, MojoBinding binding, MavenProject project, MavenSession session, MojoExecution exec, PathTranslator translator, Logger logger, PluginConfigurationException cause );

    void reportMissingRequiredMojoParameter( MojoBinding binding, MavenProject project, List invalidParameters, PluginParameterException err );

    void reportUseOfBannedMojoParameter( Parameter currentParameter, MojoBinding binding, MavenProject project, String expression, String altExpression, ExpressionEvaluationException err );

    void reportReflectionErrorWhileEvaluatingMojoParameter( Parameter currentParameter, MojoBinding binding, MavenProject project, String expression, Exception cause );

    void reportErrorApplyingMojoConfiguration( MojoBinding binding, MavenProject project, PlexusConfiguration config, PluginConfigurationException cause );

    void reportProjectDependenciesNotFound( MavenProject project, String scope, ArtifactNotFoundException cause );

    void reportProjectDependenciesUnresolvable( MavenProject project, String scope, ArtifactResolutionException cause );

    void reportProjectDependencyArtifactNotFound( MavenProject project, Artifact artifact, ArtifactNotFoundException cause );

    void reportProjectDependencyArtifactUnresolvable( MavenProject project, Artifact artifact, ArtifactResolutionException cause );

    void reportErrorLoadingPlugin( MojoBinding binding, MavenProject project, PluginLoaderException cause );

    void reportErrorFormulatingBuildPlan( List tasks, MavenProject configuringProject, String targetDescription, LifecycleException cause );

    void handleProjectBuildingError( MavenExecutionRequest request, File pomFile, ProjectBuildingException exception );

    void reportInvalidMavenVersion( MavenProject project, ArtifactVersion mavenVersion, MavenExecutionException err );

    void reportPomFileScanningError( File basedir, String includes, String excludes, IOException cause );

    void reportPomFileCanonicalizationError( File pomFile, IOException cause );

    void handleSuperPomBuildingError( ProfileManager globalProfileManager, ProjectBuildingException exception );

    void handleSuperPomBuildingError( ProjectBuildingException exception );

    void reportErrorInterpolatingModel( Model model, Map inheritedValues, File pomFile, MavenExecutionRequest request, ModelInterpolationException cause );

    void reportErrorResolvingExtensionDirectDependencies( Artifact extensionArtifact, Artifact projectArtifact, List remoteRepos, MavenExecutionRequest request, ArtifactMetadataRetrievalException cause );

    void reportErrorResolvingExtensionDependencies( Artifact extensionArtifact, Artifact projectArtifact, List remoteRepos, MavenExecutionRequest request, ArtifactResolutionResult resolutionResult, ExtensionManagerException err );

    void reportErrorManagingRealmForExtension( Artifact extensionArtifact, Artifact projectArtifact, List remoteRepos, MavenExecutionRequest request, RealmManagementException cause );

    void reportErrorManagingRealmForExtensionPlugin( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, RealmManagementException cause );

    void reportMissingArtifactWhileAddingExtensionPlugin( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ArtifactNotFoundException cause );

    void reportUnresolvableArtifactWhileAddingExtensionPlugin( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ArtifactResolutionException cause );

    void reportExtensionPluginArtifactNotFound( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, PluginNotFoundException cause );

    void reportUnresolvableExtensionPluginPOM( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ArtifactMetadataRetrievalException cause );

    void handleErrorBuildingExtensionPluginPOM( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ProjectBuildingException cause );

    void reportInvalidDependencyVersionInExtensionPluginPOM( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, InvalidDependencyVersionException cause );

    void reportErrorSearchingforCompatibleExtensionPluginVersion( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, String requiredMavenVersion, String currentMavenVersion, InvalidVersionSpecificationException cause );

    void reportIncompatibleMavenVersionForExtensionPlugin( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, String requiredMavenVersion, String currentMavenVersion, PluginVersionResolutionException err );

    void reportExtensionPluginVersionNotFound( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, PluginVersionNotFoundException cause );

    void reportErrorConfiguringExtensionPluginRealm( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, PluginManagerException cause );

}
