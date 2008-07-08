package org.apache.maven.errors;

import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.DuplicateArtifactAttachmentException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.realm.RealmManagementException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

// NOTE: The strange String[] syntax is a backward adaptation from java5 stuff, where
// I was using varargs in listOf(..). I'm not moving them to constants because I'd like
// to go back to this someday...

// TODO: Fill these out!!
public final class CoreErrorTips
{

    private static final List NO_GOALS_TIPS = Arrays.asList( new String[] {
        "Introduction to the Build Lifecycle", "\t(http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)",
        "Maven in 5 Minutes guide (http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)",
        "Maven User's documentation (http://maven.apache.org/users/)",
        "Maven Plugins page (http://maven.apache.org/plugins/)",
        "CodeHaus Mojos Project page (http://mojo.codehaus.org/plugins.html)"
    } );

    private CoreErrorTips()
    {
    }

    public static List getNoGoalsTips()
    {
        return NO_GOALS_TIPS;
    }

    public static List getMojoFailureTips( MojoBinding binding )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getProjectCycleTips( ProjectCycleException error )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTaskValidationTips( String task, Exception cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getMissingPluginDescriptorTips( MojoBinding binding,
                                                       MavenProject project )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getInvalidExecutionEnvironmentTips( MojoBinding binding,
                                                           MavenProject project,
                                                           PluginExecutionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getMojoExecutionExceptionTips( MojoBinding binding,
                                                      MavenProject project,
                                                      MojoExecutionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getMojoLookupErrorTips( MojoBinding binding,
                                               MavenProject project,
                                               ComponentLookupException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getUneditableMojoParameterTips( Parameter currentParameter,
                                                       MojoBinding binding,
                                                       MavenProject project,
                                                       PluginConfigurationException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getBannedParameterExpressionTips( Parameter currentParameter,
                                                         MojoBinding binding,
                                                         MavenProject project )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getReflectionErrorInParameterExpressionTips( String expression,
                                                                    Parameter currentParameter,
                                                                    MojoBinding binding,
                                                                    MavenProject project )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getMissingRequiredParameterTips( List invalidParameters,
                                                        MojoBinding binding,
                                                        MavenProject project )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getMojoConfigurationErrorTips( MojoBinding binding,
                                                      MavenProject project,
                                                      PlexusConfiguration config,
                                                      PluginConfigurationException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getBuildPlanningErrorTips( List tasks,
                                                  MavenProject configuringProject,
                                                  LifecycleException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorLoadingPluginTips( MojoBinding binding,
                                                  MavenProject project,
                                                  PluginLoaderException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getDependencyArtifactResolutionTips( MavenProject project,
                                                            Artifact depArtifact,
                                                            AbstractArtifactResolutionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getDependencyArtifactResolutionTips( MavenProject project,
                                                            String scope,
                                                            AbstractArtifactResolutionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTipsForModelInterpolationError( Model model,
                                                          File pomFile,
                                                          ModelInterpolationException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getIncompatibleProjectMavenVersionPrereqTips( MavenProject project,
                                                                     ArtifactVersion mavenVersion )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getPomFileScanningErrorTips( File basedir,
                                                    String includes,
                                                    String excludes )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorResolvingExtensionDirectDepsTips( Artifact extensionArtifact,
                                                                 Artifact projectArtifact,
                                                                 ArtifactMetadataRetrievalException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorResolvingExtensionArtifactsTips( Artifact extensionArtifact,
                                                                Artifact projectArtifact,
                                                                ArtifactResolutionResult resolutionResult )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorManagingExtensionRealmTips( Artifact extensionArtifact,
                                                           Artifact projectArtifact,
                                                           RealmManagementException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorManagingExtensionPluginRealmTips( Plugin plugin,
                                                                 Model originModel,
                                                                 RealmManagementException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorResolvingExtensionPluginArtifactsTips( Plugin plugin,
                                                             Model originModel,
                                                             AbstractArtifactResolutionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorResolvingExtensionPluginVersionTips( Plugin plugin,
                                                                    Model originModel,
                                                                    PluginVersionResolutionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getInvalidDependencyVersionForExtensionPluginTips( Plugin plugin,
                                                                          Model originModel,
                                                                          InvalidDependencyVersionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getInvalidPluginVersionRangeForExtensionPluginTips( Plugin plugin,
                                                                           Model originModel,
                                                                           String requiredMavenVersion,
                                                                           String currentMavenVersion )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getInvalidPluginVersionRangeForExtensionPluginTips( Plugin plugin,
                                                                           Model originModel,
                                                                           String requiredMavenVersion,
                                                                           String currentMavenVersion,
                                                                           Exception cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getUnresolvableExtensionPluginPOMTips( Plugin plugin,
                                                              Model originModel,
                                                              ArtifactMetadataRetrievalException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getExtensionPluginVersionNotFoundTips( Plugin plugin,
                                                              Model originModel,
                                                              PluginVersionNotFoundException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getErrorManagingExtensionPluginRealmTips( Plugin plugin,
                                                                 Model originModel,
                                                                 PluginManagerException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getMissingModuleTips( File pomFile,
                                             File moduleFile,
                                             String moduleName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getInvalidPluginForDirectInvocationTips( String task,
                                                                MavenSession session,
                                                                MavenProject project,
                                                                InvalidPluginException err )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getDuplicateAttachmentTips( MojoBinding binding,
                                                   MavenProject project,
                                                   DuplicateArtifactAttachmentException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
