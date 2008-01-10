package org.apache.maven.errors;

import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.RealmManagementException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.extension.DefaultBuildExtensionScanner;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.extension.ExtensionManagerException;
import org.apache.maven.extension.DefaultExtensionManager;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.plugin.version.DefaultPluginVersionManager;
import org.apache.maven.execution.RuntimeInformation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public privileged aspect ExtensionErrorReporterAspect
    extends AbstractCoreReporterAspect
{

    before( ProjectBuildingException cause ):
        withincode( List DefaultBuildExtensionScanner.getInitialRemoteRepositories() )
        && call( ExtensionScanningException.new( String, ProjectBuildingException ) )
        && args( *, cause )
    {
        getReporter().handleSuperPomBuildingError( cause );
    }

    private pointcut within_dbes_checkModulesForExtensions():
        withincode( * DefaultBuildExtensionScanner.checkModulesForExtensions( File, Model, MavenExecutionRequest, List, List, List ) );

    before( File pomFile, IOException cause ):
        within_dbes_checkModulesForExtensions()
        && call( ExtensionScanningException.new( String, File, String, IOException ) )
        && args( *, pomFile, *, cause )
    {
        getReporter().reportPomFileCanonicalizationError( pomFile, cause );
    }

    private pointcut dbes_scanInternal( File pomFile, MavenExecutionRequest request ):
        execution( void DefaultBuildExtensionScanner.scanInternal( File, MavenExecutionRequest, .. ) )
        && args( pomFile, request, .. );

    after( File pomFile, MavenExecutionRequest request, Model model, Map inheritedValues )
    throwing( ModelInterpolationException cause ):
        cflow( dbes_scanInternal( pomFile, request ) )
        && within( DefaultBuildExtensionScanner )
        && call( Model ModelInterpolator+.interpolate( Model, Map, .. ) )
        && args( model, inheritedValues, .. )
    {
        getReporter().reportErrorInterpolatingModel( model, inheritedValues, pomFile, request, cause );
    }

    private pointcut dem_addExtension( Artifact extensionArtifact, Artifact projectArtifact, List remoteRepos, MavenExecutionRequest request ):
        execution( * DefaultExtensionManager.addExtension( Artifact, Artifact, List, MavenExecutionRequest, .. ) )
        && args( extensionArtifact, projectArtifact, remoteRepos, request, .. );

    before( Artifact extensionArtifact, Artifact projectArtifact, List remoteRepos, MavenExecutionRequest request, ArtifactMetadataRetrievalException cause ):
        cflow( dem_addExtension( extensionArtifact, projectArtifact, remoteRepos, request ) )
        && call( ExtensionManagerException.new( .., ArtifactMetadataRetrievalException ) )
        && args( .., cause )
    {
        getReporter().reportErrorResolvingExtensionDirectDependencies( extensionArtifact, projectArtifact, remoteRepos, request, cause );
    }

    before( Artifact extensionArtifact, Artifact projectArtifact, List remoteRepos, MavenExecutionRequest request, ArtifactResolutionResult resolutionResult, ExtensionManagerException err ):
        cflow( dem_addExtension( extensionArtifact, projectArtifact, remoteRepos, request ) )
        && execution( ExtensionManagerException.new( .., ArtifactResolutionResult ) )
        && args( .., resolutionResult )
        && this( err )
    {
        getReporter().reportErrorResolvingExtensionDependencies( extensionArtifact, projectArtifact, remoteRepos, request, resolutionResult, err );
    }

    private pointcut call_eme_ctor_RealmManagementException( RealmManagementException cause ):
        call( ExtensionManagerException.new( .., RealmManagementException ) )
        && args( .., cause );

    private pointcut within_dem_addExtension():
        withincode( void DefaultExtensionManager.addExtension( Artifact, Artifact, List, MavenExecutionRequest, .. ) );

    before( Artifact extensionArtifact, Artifact projectArtifact, List remoteRepos, MavenExecutionRequest request, RealmManagementException cause ):
        cflow( dem_addExtension( extensionArtifact, projectArtifact, remoteRepos, request ) )
        && within_dem_addExtension()
        && call_eme_ctor_RealmManagementException( cause )
    {
        getReporter().reportErrorManagingRealmForExtension( extensionArtifact, projectArtifact, remoteRepos, request, cause );
    }

    private pointcut dem_addPluginAsExtension( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request ):
        execution( * DefaultExtensionManager.addPluginAsExtension( Plugin, Model, List, MavenExecutionRequest ) )
        && args( plugin, originModel, remoteRepos, request );

    private pointcut within_dem_addPluginAsExtension():
        withincode( void DefaultExtensionManager.addPluginAsExtension( Plugin, Model, List, MavenExecutionRequest, .. ) );

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, RealmManagementException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && within_dem_addPluginAsExtension()
        && call_eme_ctor_RealmManagementException( cause )
    {
        getReporter().reportErrorManagingRealmForExtensionPlugin( plugin, originModel, remoteRepos, request, cause );
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ArtifactNotFoundException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && within_dem_addPluginAsExtension()
        && call( ExtensionManagerException.new( .., ArtifactNotFoundException ) )
        && args( .., cause )
    {
        getReporter().reportMissingArtifactWhileAddingExtensionPlugin( plugin, originModel, remoteRepos, request, cause );
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ArtifactResolutionException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && within_dem_addPluginAsExtension()
        && call( ExtensionManagerException.new( .., ArtifactResolutionException ) )
        && args( .., cause )
    {
        getReporter().reportUnresolvableArtifactWhileAddingExtensionPlugin( plugin, originModel, remoteRepos, request, cause );
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, PluginNotFoundException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && within_dem_addPluginAsExtension()
        && call( ExtensionManagerException.new( .., PluginNotFoundException ) )
        && args( .., cause )
    {
        getReporter().reportExtensionPluginArtifactNotFound( plugin, originModel, remoteRepos, request, cause );
    }

    private pointcut within_dpvm_resolveMetaVersion():
        withincode( * DefaultPluginVersionManager.resolveMetaVersion( .. ) );

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ArtifactMetadataRetrievalException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && cflow( execution( * PluginManager+.verifyPlugin( .. ) ) )
        && within_dpvm_resolveMetaVersion()
        && call( PluginVersionResolutionException.new( .., ArtifactMetadataRetrievalException ) )
        && args( .., cause )
    {
        getReporter().reportUnresolvableExtensionPluginPOM( plugin, originModel, remoteRepos, request, cause );
    }

    String requiredVersion = null;
    String currentVersion = null;

    after() returning( String mavenVersion ):
        ( withincode( * DefaultPluginManager.checkRequiredMavenVersion( .. ) )
          || within_dpvm_resolveMetaVersion() )
        && call( * Prerequisites.getMaven() )
    {
        requiredVersion = mavenVersion;
    }

    after() returning( ArtifactVersion mavenVersion ):
        ( withincode( * DefaultPluginManager.checkRequiredMavenVersion( .. ) )
          || within_dpvm_resolveMetaVersion() )
        && call( * RuntimeInformation+.getApplicationVersion() )
    {
        currentVersion = mavenVersion.toString();
    }

    after():
        execution( * DefaultPluginManager.checkRequiredMavenVersion( .. ) )
    {
        requiredVersion = null;
        currentVersion = null;
    }

    after():
        execution( * DefaultPluginVersionManager.resolveMetaVersion( .. ) )
    {
        requiredVersion = null;
        currentVersion = null;
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, InvalidVersionSpecificationException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && cflow( execution( * PluginManager+.verifyPlugin( .. ) ) )
        && withincode( * DefaultPluginVersionManager.resolveMetaVersion( .. ) )
        && call( PluginVersionResolutionException.new( .., InvalidVersionSpecificationException ) )
        && args( .., cause )
    {
        getReporter().reportErrorSearchingforCompatibleExtensionPluginVersion( plugin, originModel, remoteRepos, request, requiredVersion, currentVersion, cause );
    }

    private pointcut dpm_verifyVersionedPlugin( Plugin plugin ):
        execution( * DefaultPluginManager.verifyVersionedPlugin( Plugin, .. ) )
        && args( plugin, .. );

    after( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, PluginVersionResolutionException err ):
        cflow( dem_addPluginAsExtension( Plugin, originModel, remoteRepos, request ) )
        && cflow( execution( * PluginManager+.verifyPlugin( .. ) ) )
        && cflow( dpm_verifyVersionedPlugin( plugin ) )
        && execution( PluginVersionResolutionException.new( .., String ) )
        && this( err )
    {
        getReporter().reportIncompatibleMavenVersionForExtensionPlugin( plugin, originModel, remoteRepos, request, requiredVersion, currentVersion, err );
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, ProjectBuildingException cause ):
        cflow( dem_addPluginAsExtension( Plugin, originModel, remoteRepos, request ) )
        && cflow( execution( * PluginManager+.verifyPlugin( .. ) ) )
        && cflow( dpm_verifyVersionedPlugin( plugin ) )
        && execution( InvalidPluginException.new( .., ProjectBuildingException ) )
        && args( .., cause )
    {
        getReporter().handleErrorBuildingExtensionPluginPOM( plugin, originModel, remoteRepos, request, cause );
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, InvalidDependencyVersionException cause ):
        cflow( dem_addPluginAsExtension( Plugin, originModel, remoteRepos, request ) )
        && cflow( execution( * PluginManager+.verifyPlugin( .. ) ) )
        && cflow( dpm_verifyVersionedPlugin( plugin ) )
        && execution( InvalidPluginException.new( .., InvalidDependencyVersionException ) )
        && args( .., cause )
    {
        getReporter().reportInvalidDependencyVersionInExtensionPluginPOM( plugin, originModel, remoteRepos, request, cause );
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, PluginVersionNotFoundException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && within_dem_addPluginAsExtension()
        && call( ExtensionManagerException.new( .., PluginVersionNotFoundException ) )
        && args( .., cause )
    {
        getReporter().reportExtensionPluginVersionNotFound( plugin, originModel, remoteRepos, request, cause );
    }

    before( Plugin plugin, Model originModel, List remoteRepos, MavenExecutionRequest request, PluginManagerException cause ):
        cflow( dem_addPluginAsExtension( plugin, originModel, remoteRepos, request ) )
        && within_dem_addPluginAsExtension()
        && call( ExtensionManagerException.new( .., PluginManagerException+ ) )
        && args( .., cause )
    {
        getReporter().reportErrorConfiguringExtensionPluginRealm( plugin, originModel, remoteRepos, request, cause );
    }

}
