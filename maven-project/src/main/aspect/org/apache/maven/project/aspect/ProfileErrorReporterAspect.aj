package org.apache.maven.project.aspect;

import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.profiles.activation.ProfileActivator;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.build.DefaultProfileAdvisor;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.DefaultProfileManager;
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

    protected pointcut componentLookupException( ComponentLookupException cause ):
        handler( ComponentLookupException )
        && args( cause );

    private pointcut pMgr_isActiveExec( Profile profile, ProfileActivationContext context ):
        execution( boolean DefaultProfileManager.isActive( Profile, ProfileActivationContext ) )
        && args( profile, context );

    private pointcut pAdv_applyActivatedProfiles( Model model, File pomFile ):
        execution( private List DefaultProfileAdvisor.applyActivatedProfiles( Model, File, .. ) )
        && args( model, pomFile, .. );

    private pointcut applyActivatedProfiles_ComponentLookupException( Model model,
                                                                      File pomFile,
                                                                      Profile profile ):
        call( List PlexusContainer+.lookupList( .. ) )
        && cflow( pAdv_applyActivatedProfiles( model, pomFile ) )
        && cflow( pMgr_isActiveExec( profile, ProfileActivationContext ) );

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
    after( Model model, File pomFile, Profile profile )
        throwing( ComponentLookupException cause ):
            applyActivatedProfiles_ComponentLookupException( model, pomFile, profile )
    {
        getReporter().reportActivatorLookupErrorWhileApplyingProfiles( model, pomFile, profile, cause );
    }

    protected pointcut profileActivatorCall( ProfileActivator activator ):
        call( * ProfileActivator+.*( .. ) )
        && target( activator );

    private pointcut applyActivatedProfiles_ActivatorThrown( ProfileActivator activator,
                                                                      Model model,
                                                                      File pomFile,
                                                                      Profile profile,
                                                                      ProfileActivationContext context ):
        profileActivatorCall( activator )
        && cflow( pAdv_applyActivatedProfiles( model, pomFile ) )
        && cflow( pMgr_isActiveExec( profile, context ) );

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
            applyActivatedProfiles_ActivatorThrown( activator, model, pomFile, profile, context )
    {
        getReporter().reportActivatorErrorWhileApplyingProfiles( activator, model, pomFile, profile, context, cause );
    }

    private pointcut pAdv_loadExternalProjectProfiles( Model model, File pomFile ):
        execution( private void DefaultProfileAdvisor.loadExternalProjectProfiles( *, Model, File ) )
        && args( *, model, pomFile );

    private pointcut loadExternalProfiles_profileBuilding( Model model,
                                                       File pomFile,
                                                       File projectDir ):
        call( ProfilesRoot MavenProfilesBuilder+.buildProfiles( File ) )
        && cflow( pAdv_loadExternalProjectProfiles( model, pomFile ) )
        && args( projectDir );

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
           loadExternalProfiles_profileBuilding( model, pomFile, projectDir )
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
           loadExternalProfiles_profileBuilding( model, pomFile, projectDir )
    {
        getReporter().reportErrorLoadingExternalProfilesFromFile( model, pomFile, projectDir, cause );
    }

    private pointcut pAdv_getArtifactRepositoriesFromActiveProfiles( String projectId, File pomFile ):
        execution( LinkedHashSet DefaultProfileAdvisor.getArtifactRepositoriesFromActiveProfiles( *, File, String ) )
        && args( *, pomFile, projectId );

    private pointcut getArtifactRepositoriesFromActiveProfiles_ComponentLookupException( String projectId,
                                                                                          File pomFile,
                                                                                          Profile profile ):
        call( List PlexusContainer+.lookupList( .. ) )
        && cflow( pAdv_getArtifactRepositoriesFromActiveProfiles( projectId, pomFile ) )
        && cflow( pMgr_isActiveExec( profile, ProfileActivationContext ) );

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
    after( String projectId, File pomFile, Profile profile )
        throwing( ComponentLookupException cause ):
            getArtifactRepositoriesFromActiveProfiles_ComponentLookupException( projectId, pomFile, profile )
    {
        getReporter().reportActivatorLookupErrorWhileGettingRepositoriesFromProfiles( projectId, pomFile, profile, cause );
    }

    private pointcut getArtifactRepositoriesFromActiveProfiles_ActivatorThrown( ProfileActivator activator,
                                                                      String projectId,
                                                                      File pomFile,
                                                                      Profile profile,
                                                                      ProfileActivationContext context ):
        profileActivatorCall( activator )
        && cflow( pAdv_getArtifactRepositoriesFromActiveProfiles( projectId, pomFile ) )
        && cflow( pMgr_isActiveExec( profile, context ) );

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
    after( ProfileActivator activator, String projectId, File pomFile, Profile profile, ProfileActivationContext context )
        throwing( ProfileActivationException cause ):
            getArtifactRepositoriesFromActiveProfiles_ActivatorThrown( activator, projectId, pomFile, profile, context )
    {
        getReporter().reportActivatorErrorWhileGettingRepositoriesFromProfiles( activator, projectId, pomFile, profile, context, cause );
    }

    private pointcut getArtifactRepositoriesFromActiveProfiles_InvalidRepository( Repository repo,
                                                                                String projectId,
                                                                                File pomFile ):
        call( ArtifactRepository MavenTools+.buildArtifactRepository( Repository ) )
        && args( repo )
        && cflow( pAdv_getArtifactRepositoriesFromActiveProfiles( projectId, pomFile ) );

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
    after( Repository repo, String projectId, File pomFile )
        throwing( InvalidRepositoryException cause ):
            getArtifactRepositoriesFromActiveProfiles_InvalidRepository( repo, projectId, pomFile )
    {
        getReporter().reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( repo, projectId, pomFile, cause );
    }

}
