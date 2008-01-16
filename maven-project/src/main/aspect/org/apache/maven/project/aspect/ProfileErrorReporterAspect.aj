package org.apache.maven.project.aspect;

import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.profiles.activation.ProfileActivator;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.build.DefaultProfileAdvisor;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfilesRoot;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.MavenTools;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.PlexusContainer;

import java.util.List;
import java.util.LinkedHashSet;
import java.io.File;
import java.io.IOException;

/**
 * Error reporting advice to handle {@link ProjectBuildingException} instances
 * coming from {@link DefaultProfileAdvisor}.
 */
public privileged aspect ProfileErrorReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    private pointcut within_pMgr_isActiveExec( Profile profile, ProfileActivationContext context ):
        withincode( boolean DefaultProfileManager.isActive( Profile, ProfileActivationContext ) )
        && args( profile, context );

    private pointcut pMgr_isActiveExec( Profile profile, ProfileActivationContext context ):
        execution( boolean DefaultProfileManager.isActive( Profile, ProfileActivationContext ) )
        && args( profile, context );

    private pointcut pAdv_applyActivatedProfiles( Model model, File pomFile ):
        execution( private List DefaultProfileAdvisor.applyActivatedProfiles( Model, File, .. ) )
        && args( model, pomFile, .. )
        && notWithinAspect();

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // DefaultProfileAdvisor.applyActivatedProfiles(..)
    // DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
    // --> DefaultProfileAdvisor.applyActivatedProfiles(..) (private)
    //     --> DefaultProfileManager.getActiveProfiles(..)
    //         --> DefaultProfileManager.isActive(..) (private)
    //             --> PlexusContainer.lookupList(..)
    //             <-- ComponentLookupException
    //         <-- ProfileActivationException
    // <------ ProjectBuildingException
    // =========================================================================
    after( Model model, File pomFile, Profile profile, ProfileActivationContext context )
        throwing( ComponentLookupException cause ):
            call( List PlexusContainer+.lookupList( .. ) )
            && cflow( pAdv_applyActivatedProfiles( model, pomFile ) )
            && cflow( pMgr_isActiveExec( profile, context ) )
    {
        getReporter().reportActivatorLookupError( model, pomFile, profile, context, cause );
    }

    private pointcut pAdv_getArtifactRepositoriesFromActiveProfiles_1( Model model, File pomFile ):
        execution( LinkedHashSet ProfileAdvisor+.getArtifactRepositoriesFromActiveProfiles( Model, File, ProfileManager ) )
        && args( model, pomFile, * );

    private pointcut pAdv_getArtifactRepositoriesFromActiveProfiles_2( Model model, File pomFile ):
        execution( LinkedHashSet ProfileAdvisor+.getArtifactRepositoriesFromActiveProfiles( Model, File, boolean, ProfileActivationContext ) )
        && args( model, pomFile, *, * );

    private pointcut pAdv_getArtifactRepos( Model model, File pomFile ):
        pAdv_getArtifactRepositoriesFromActiveProfiles_1( model, pomFile )
        || pAdv_getArtifactRepositoriesFromActiveProfiles_2( model, pomFile );

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // DefaultProfileAdvisor.applyActivatedProfiles(..)
    // DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
    // --> DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
    //     --> DefaultProfileManager.getActiveProfiles(..)
    //         --> DefaultProfileManager.isActive(..) (private)
    //             --> PlexusContainer.lookupList(..)
    //             <-- ComponentLookupException
    //         <-- ProfileActivationException
    // <------ ProjectBuildingException
    // =========================================================================
    after( Model model, File pomFile, Profile profile, ProfileActivationContext context )
        throwing( ComponentLookupException cause ):
            call( List PlexusContainer+.lookupList( .. ) )
            && cflow( pAdv_getArtifactRepos( model, pomFile ) )
            && cflow( pMgr_isActiveExec( profile, context ) )
    {
        getReporter().reportActivatorLookupError( model, pomFile, profile, context, cause );
    }

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // DefaultProfileAdvisor.applyActivatedProfiles(..)
    // DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
    // --> DefaultProfileAdvisor.applyActivatedProfiles(..) (private)
    //     --> DefaultProfileManager.getActiveProfiles(..)
    //         --> DefaultProfileManager.isActive(..) (private)
    //             --> ProfileActivator.canDetermineActivation(..)
    //             --> ProfileActivator.isActive(..)
    //         <------ ProfileActivationException
    // <------ ProjectBuildingException
    // =========================================================================
    after( ProfileActivator activator, Model model, File pomFile, Profile profile, ProfileActivationContext context )
        throwing( ProfileActivationException cause ):
            call( * ProfileActivator+.*( .. ) )
            && target( activator )
            && cflow( pAdv_applyActivatedProfiles( model, pomFile ) )
            && cflow( pMgr_isActiveExec( profile, context ) )
    {
        getReporter().reportActivatorError( activator, model, pomFile, profile, context, cause );
    }

    private pointcut pAdv_loadExternalProjectProfiles( Model model, File pomFile ):
        execution( private void DefaultProfileAdvisor.loadExternalProjectProfiles( *, Model, File ) )
        && args( *, model, pomFile )
        && notWithinAspect();

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // DefaultProfileAdvisor.applyActivatedProfiles(..)
    // DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
    // --> DefaultProfileAdvisor.buildProfileManager(..) (private)
    //     --> DefaultProfileAdvisor.loadExternalProjectProfiles(..) (private)
    //         --> MavenProfilesBuilder.buildProfiles(..)
    //         <-- IOException
    // <------ ProjectBuildingException
    // =========================================================================
    after( Model model, File pomFile, File projectDir )
        throwing( IOException cause ):
            call( ProfilesRoot MavenProfilesBuilder+.buildProfiles( File ) )
            && cflow( pAdv_loadExternalProjectProfiles( model, pomFile ) )
            && args( projectDir )
    {
        getReporter().reportErrorLoadingExternalProfilesFromFile( model, pomFile, projectDir, cause );
    }

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // DefaultProfileAdvisor.applyActivatedProfiles(..)
    // DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
    // --> DefaultProfileAdvisor.buildProfileManager(..) (private)
    //     --> DefaultProfileAdvisor.loadExternalProjectProfiles(..) (private)
    //         --> MavenProfilesBuilder.buildProfiles(..)
    //         <-- XmlPullParserException
    // <------ ProjectBuildingException
    // =========================================================================
    after( Model model, File pomFile, File projectDir )
        throwing( XmlPullParserException cause ):
            call( ProfilesRoot MavenProfilesBuilder+.buildProfiles( File ) )
            && cflow( pAdv_loadExternalProjectProfiles( model, pomFile ) )
            && args( projectDir )
    {
        getReporter().reportErrorLoadingExternalProfilesFromFile( model, pomFile, projectDir, cause );
    }

    private pointcut pAdv_getArtifactRepositoriesFromActiveProfiles( String projectId, File pomFile ):
        execution( LinkedHashSet DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles( *, File, String ) )
        && args( *, pomFile, projectId )
        && notWithinAspect();

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // DefaultProfileAdvisor.applyActivatedProfiles(..)
    // DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
    // --> DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
    //     --> DefaultProfileManager.getActiveProfiles(..)
    //         --> DefaultProfileManager.isActive(..) (private)
    //             --> ProfileActivator.canDetermineActivation(..)
    //             --> ProfileActivator.isActive(..)
    //         <------ ProfileActivationException
    // <------ ProjectBuildingException
    // =========================================================================
    after( ProfileActivator activator, Model model, File pomFile, Profile profile, ProfileActivationContext context )
        throwing( ProfileActivationException cause ):
            call( * ProfileActivator+.*( .. ) )
            && target( activator )
            && cflow( pAdv_getArtifactRepos( model, pomFile ) )
            && cflow( pMgr_isActiveExec( profile, context ) )
            && within( DefaultProfileManager )
    {
        getReporter().reportActivatorError( activator, model, pomFile, profile, context, cause );
    }

    // =========================================================================
    // Call Stack:
    // =========================================================================
    // DefaultProfileAdvisor.applyActivatedProfiles(..)
    // DefaultProfileAdvisor.applyActivatedExternalProfiles(..)
    // --> DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles(..)
    //     --> MavenTools.buildArtifactRepository(..)
    //     <-- InvalidRepositoryException
    // <-- ProjectBuildingException
    // =========================================================================
    after( Repository repo, Model model, File pomFile )
        throwing( InvalidRepositoryException cause ):
            call( ArtifactRepository MavenTools+.buildArtifactRepository( Repository ) )
            && args( repo )
            && cflow( pAdv_getArtifactRepos( model, pomFile ) )
            && within( DefaultProfileAdvisor )
    {
        getReporter().reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( repo, model, pomFile, cause );
    }

}
