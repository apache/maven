package org.apache.maven.project.error;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Dependency;
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultProjectErrorReporter
    implements ProjectErrorReporter
{

    private static final String NEWLINE = "\n";

    private Map formattedMessages;

    private Map realCauses;

    private Map stackTraceRecommendations;

    public DefaultProjectErrorReporter( Map formattedMessageStore, Map realCauseStore, Map stackTraceRecommendationStore )
    {
        formattedMessages = formattedMessageStore;
        realCauses = realCauseStore;
        stackTraceRecommendations = stackTraceRecommendationStore;
    }

    public DefaultProjectErrorReporter()
    {
        formattedMessages = new LinkedHashMap();
        realCauses = new HashMap();
        stackTraceRecommendations = new HashMap();
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#clearErrors()
     */
    public void clearErrors()
    {
        formattedMessages.clear();
        realCauses.clear();
    }

    public List getReportedExceptions()
    {
        return new ArrayList( formattedMessages.keySet() );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#hasInformationFor(java.lang.Throwable)
     */
    public Throwable findReportedException( Throwable error )
    {
        if ( formattedMessages.containsKey( error ) )
        {
            return error;
        }
        else if ( error.getCause() != null )
        {
            return findReportedException( error.getCause() );
        }

        return null;
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#getFormattedMessage(java.lang.Throwable)
     */
    public String getFormattedMessage( Throwable error )
    {
        return (String) formattedMessages.get( error );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#getRealCause(java.lang.Throwable)
     */
    public Throwable getRealCause( Throwable error )
    {
        return (Throwable) realCauses.get( error );
    }

    public boolean isStackTraceRecommended( Throwable error )
    {
        Boolean rec = (Boolean) stackTraceRecommendations.get( error );

        if ( rec == null )
        {
            return false;
        }

        return rec.booleanValue();
    }

    protected void setStackTraceRecommendation( Throwable error, boolean recommended )
    {
        stackTraceRecommendations.put( error, Boolean.valueOf( recommended ) );
    }

    protected void registerBuildError( Throwable error,
                                            String formattedMessage,
                                            Throwable realCause )
    {
        formattedMessages.put( error, formattedMessage );
        realCauses.put( error, realCause );
    }

    protected void registerBuildError( Throwable error,
                                            String formattedMessage )
    {
        formattedMessages.put( error, formattedMessage );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#reportActivatorErrorWhileApplyingProfiles(org.apache.maven.profiles.activation.ProfileActivator, org.apache.maven.model.Model, java.io.File, org.apache.maven.model.Profile, org.apache.maven.profiles.activation.ProfileActivationContext, org.apache.maven.profiles.activation.ProfileActivationException)
     */
    public void reportActivatorError( ProfileActivator activator,
                                      Model model,
                                      File pomFile,
                                      Profile profile,
                                      ProfileActivationContext context,
                                      ProfileActivationException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Profile activator: " );
        writer.write( activator.getClass().getName() );
        writer.write( " experienced an error while processing profile:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( profile.getId() );
        writer.write( " (source: " );
        writer.write( profile.getSource() );
        writer.write( ")" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );

        addStandardInfo( model.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForActivatorError( activator,
                                                            model.getId(),
                                                            pomFile,
                                                            profile,
                                                            context,
                                                            cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#reportActivatorLookupError(java.lang.String, java.io.File, org.apache.maven.model.Profile, org.codehaus.plexus.component.repository.exception.ComponentLookupException)
     */
    public void reportActivatorLookupError( Model model,
                                            File pomFile,
                                            Profile profile,
                                            ProfileActivationContext context,
                                            ComponentLookupException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Error retrieving profile-activator component while processing profile:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( profile.getId() );
        writer.write( " (source: " );
        writer.write( profile.getSource() );
        writer.write( ")" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );

        addStandardInfo( model.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForActivatorLookupError( model.getId(),
                                                                  pomFile,
                                                                  profile,
                                                                  cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#reportErrorLoadingExternalProfilesFromFile(org.apache.maven.model.Model, java.io.File, java.io.File, java.io.IOException)
     */
    public void reportErrorLoadingExternalProfilesFromFile( Model model,
                                                            File pomFile,
                                                            File projectDir,
                                                            IOException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Failed to load external profiles from project directory: " );
        writer.write( NEWLINE );
        writer.write( String.valueOf( projectDir ) );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );

        addStandardInfo( model.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForErrorLoadingExternalProfilesFromFile( model,
                                                                                  pomFile,
                                                                                  projectDir,
                                                                                  cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#reportErrorLoadingExternalProfilesFromFile(org.apache.maven.model.Model, java.io.File, java.io.File, org.codehaus.plexus.util.xml.pull.XmlPullParserException)
     */
    public void reportErrorLoadingExternalProfilesFromFile( Model model,
                                                            File pomFile,
                                                            File projectDir,
                                                            XmlPullParserException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Failed to load external profiles from project directory: " );
        writer.write( NEWLINE );
        writer.write( String.valueOf( projectDir ) );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Line: " );
        writer.write( cause.getLineNumber() );
        writer.write( NEWLINE );
        writer.write( "Column: " );
        writer.write( cause.getColumnNumber() );

        addStandardInfo( model.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForErrorLoadingExternalProfilesFromFile( model,
                                                                                  pomFile,
                                                                                  projectDir,
                                                                                  cause ), writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    /**
     * @see org.apache.maven.project.error.ProjectErrorReporter#reportInvalidRepositoryWhileGettingRepositoriesFromProfiles(org.apache.maven.model.Repository, java.lang.String, java.io.File, org.apache.maven.artifact.InvalidRepositoryException)
     */
    public void reportInvalidRepositoryWhileGettingRepositoriesFromProfiles( Repository repo,
                                                                             Model model,
                                                                             File pomFile,
                                                                             InvalidRepositoryException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Invalid repository declaration: " );
        writer.write( repo.getId() );
        writer.write( NEWLINE );
        writer.write( "(URL: " );
        writer.write( repo.getUrl() );
        writer.write( ")" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Error message: " );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );

        addStandardInfo( model.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForInvalidRepositorySpec( repo, model.getId(), pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString(), cause.getCause() );
    }

    private void addStandardInfo( String projectId,
                                  File pomFile,
                                  StringWriter writer )
    {
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project Id: " );
        writer.write( projectId );
        writer.write( NEWLINE );
        writer.write( "From file: " );
        writer.write( String.valueOf( pomFile ) );
    }

    private void addTips( List tips,
                          StringWriter writer )
    {
        if ( ( tips != null ) && !tips.isEmpty() )
        {
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Some tips:" );
            for ( Iterator it = tips.iterator(); it.hasNext(); )
            {
                String tip = (String) it.next();

                writer.write( NEWLINE );
                writer.write( "\t- " );
                writer.write( tip );
            }
        }
    }

    public void reportErrorCreatingArtifactRepository( String projectId,
                                                       File pomFile,
                                                       Repository repo,
                                                       InvalidRepositoryException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "You have an invalid repository/pluginRepository declaration in your POM:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Repository-Id: " );
        writer.write( cause.getRepositoryId() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Reason: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        addStandardInfo( projectId, pomFile, writer );
        addTips( ProjectErrorTips.getTipsForInvalidRepositorySpec( repo, projectId, pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportErrorCreatingDeploymentArtifactRepository( MavenProject project,
                                                                 File pomFile,
                                                                 DeploymentRepository repo,
                                                                 InvalidRepositoryException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "You have an invalid repository/snapshotRepository declaration in the <distributionManagement/> section of your POM:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Repository-Id: " );
        writer.write( cause.getRepositoryId() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Reason: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        addStandardInfo( project.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForInvalidRepositorySpec( repo, project.getId(), pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportBadNonDependencyProjectArtifactVersion( MavenProject project,
                                                              File pomFile,
                                                              InvalidProjectVersionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "You have an invalid version in your POM:" );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Location: " );
        writer.write( cause.getLocationInPom() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Reason: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        addStandardInfo( project.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForBadNonDependencyArtifactSpec( project, pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportErrorInterpolatingModel( MavenProject project,
                                               File pomFile,
                                               ModelInterpolationException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "You have an invalid expression in your POM (interpolation failed):" );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );

        addStandardInfo( project.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForProjectInterpolationError( project, pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportProjectValidationFailure( MavenProject project,
                                                File pomFile,
                                                InvalidProjectModelException error )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "The following POM validation errors were detected:" );
        writer.write( NEWLINE );

        for ( Iterator it = error.getValidationResult().getMessages().iterator(); it.hasNext(); )
        {
            String message = (String) it.next();
            writer.write( NEWLINE );
            writer.write( " - " );
            writer.write( message );
        }

        writer.write( NEWLINE );
        writer.write( NEWLINE );

        addStandardInfo( project.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForProjectValidationFailure( project, pomFile, error.getValidationResult() ),
                 writer );

        registerBuildError( error, writer.toString() );
    }

    public void reportBadManagedDependencyVersion( MavenProject project,
                                            File pomFile,
                                            InvalidDependencyVersionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Your project declares a dependency with an invalid version inside its <dependencyManagement/> section." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        Dependency dep = cause.getDependency();
        writer.write( "Dependency:" );
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( dep.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( dep.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( dep.getVersion() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Reason: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        addStandardInfo( project.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForBadDependencySpec( project, pomFile, dep ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportBadDependencyVersion( MavenProject project,
                                            File pomFile,
                                            InvalidDependencyVersionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Your project declares a dependency with an invalid version." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        Dependency dep = cause.getDependency();
        writer.write( "Dependency:" );
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( dep.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( dep.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( dep.getVersion() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );

        writer.write( "Reason: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        addStandardInfo( project.getId(), pomFile, writer );
        addTips( ProjectErrorTips.getTipsForBadDependencySpec( project, pomFile, dep ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportErrorParsingProjectModel( String projectId,
                                                File pomFile,
                                                XmlPullParserException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        if ( pomFile == null )
        {
            writer.write( "Error parsing built-in super POM!" );
        }
        else
        {
            writer.write( "Error parsing POM." );
        }

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Line: " );
        writer.write( "" + ( cause ).getLineNumber() );
        writer.write( NEWLINE );
        writer.write( "Column: " );
        writer.write( "" + ( cause ).getColumnNumber() );
        writer.write( NEWLINE );

        addStandardInfo( projectId, pomFile, writer );
        addTips( ProjectErrorTips.getTipsForPomParsingError( projectId, pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportErrorParsingParentProjectModel( ModelAndFile childInfo,
                                                      File parentPomFile,
                                                      XmlPullParserException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        if ( parentPomFile == null )
        {
            writer.write( "Error parsing built-in super POM!" );
        }
        else
        {
            writer.write( "Error parsing parent-POM." );
        }

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Line: " );
        writer.write( "" + cause.getLineNumber() );
        writer.write( NEWLINE );
        writer.write( "Column: " );
        writer.write( "" + cause.getColumnNumber() );
        writer.write( NEWLINE );

        String projectId = childInfo.getModel().getParent().getId();
        String childId = childInfo.getModel().getId();

        addStandardInfo( projectId, parentPomFile, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Child-Project Id: " );
        writer.write( childId );

        addTips( ProjectErrorTips.getTipsForPomParsingError( projectId, parentPomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportErrorParsingProjectModel( String projectId,
                                                File pomFile,
                                                IOException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        if ( pomFile == null )
        {
            writer.write( "Error reading built-in super POM!" );
        }
        else
        {
            writer.write( "Error reading POM." );
        }

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        addStandardInfo( projectId, pomFile, writer );
        addTips( ProjectErrorTips.getTipsForPomParsingError( projectId, pomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportErrorParsingParentProjectModel( ModelAndFile childInfo,
                                                      File parentPomFile,
                                                      IOException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        if ( parentPomFile == null )
        {
            writer.write( "Error reading built-in super POM!" );
        }
        else
        {
            writer.write( "Error reading parent-POM." );
        }

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        String projectId = childInfo.getModel().getParent().getId();
        String childId = childInfo.getModel().getId();

        addStandardInfo( projectId, parentPomFile, writer );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Child-Project Id: " );
        writer.write( childId );

        addTips( ProjectErrorTips.getTipsForPomParsingError( projectId, parentPomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportParentPomArtifactNotFound( Parent parentRef,
                                                 ProjectBuilderConfiguration config,
                                                 List remoteRepos,
                                                 String childId,
                                                 File childPomFile,
                                                 ArtifactNotFoundException cause )
    {
        reportArtifactError( parentRef, config, remoteRepos, childId, childPomFile, cause );
    }

    public void reportParentPomArtifactUnresolvable( Parent parentRef,
                                                     ProjectBuilderConfiguration config,
                                                     List remoteRepos,
                                                     String childId,
                                                     File childPomFile,
                                                     ArtifactResolutionException cause )
    {
        reportArtifactError( parentRef, config, remoteRepos, childId, childPomFile, cause );
    }

    private void reportArtifactError( Parent parentRef,
                                      ProjectBuilderConfiguration config,
                                      List remoteRepos,
                                      String childId,
                                      File childPomFile,
                                      AbstractArtifactResolutionException cause )
    {
        StringWriter writer = new StringWriter();

        writer.write( NEWLINE );
        writer.write( "Failed to resolve parent-POM from repository." );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Parent POM Information: " );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Group-Id: " );
        writer.write( parentRef.getGroupId() );
        writer.write( NEWLINE );
        writer.write( "Artifact-Id: " );
        writer.write( parentRef.getArtifactId() );
        writer.write( NEWLINE );
        writer.write( "Version: " );
        writer.write( parentRef.getVersion() );

        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Local Repository: " );
        writer.write( config.getLocalRepository().getBasedir() );

        if ( ( remoteRepos != null ) && !remoteRepos.isEmpty() )
        {
            writer.write( NEWLINE );
            writer.write( NEWLINE );
            writer.write( "Remote Repositories: " );

            for ( Iterator it = remoteRepos.iterator(); it.hasNext(); )
            {
                ArtifactRepository remoteRepo = (ArtifactRepository) it.next();
                writer.write( NEWLINE );
                writer.write( remoteRepo.getId() );
                writer.write( " -> " );
                writer.write( remoteRepo.getUrl() );
                // TODO: Get mirrors!!
            }
        }


        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Reason: " );
        writer.write( cause.getMessage() );
        writer.write( NEWLINE );

        addStandardInfo( childId, childPomFile, writer );
        addTips( ProjectErrorTips.getTipsForPomParsingError( childId, childPomFile, cause ),
                 writer );

        registerBuildError( cause, writer.toString() );
    }

    public void reportProjectCollision( List allProjectInstances,
                                        DuplicateProjectException err )
    {

        File existing = err.getExistingProjectFile();
        File conflicting = err.getConflictingProjectFile();
        String projectId = err.getProjectId();

        StringWriter writer = new StringWriter();

        writer.write( "Duplicated project detected." );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "Project: " + projectId );
        writer.write( NEWLINE );
        writer.write( "File: " );
        writer.write( String.valueOf( existing ) );
        writer.write( NEWLINE );
        writer.write( "File: " );
        writer.write( String.valueOf( conflicting ) );
        writer.write( NEWLINE );
        writer.write( NEWLINE );
        writer.write( "NOTE: Each project in a Maven build must have a unique combination of groupId and artifactId." );

        addTips( ProjectErrorTips.getTipsForDuplicateProjectError( allProjectInstances, err ),
                 writer );

        registerBuildError( err, writer.toString() );
    }
}
