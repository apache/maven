package org.apache.maven.project.build.model;

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.ProjectBuildCache;
import org.apache.maven.project.build.profile.ProfileAdvisor;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @see org.apache.maven.project.build.model.ModelLineageBuilder
 */
public class DefaultModelLineageBuilder
    implements ModelLineageBuilder, LogEnabled
{

    public static final String ROLE_HINT = "default";

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private MavenTools mavenTools;

    private ProfileAdvisor profileAdvisor;
    
    private BuildContextManager buildContextManager;

    private Logger logger;

    public DefaultModelLineageBuilder()
    {
    }

    public DefaultModelLineageBuilder( ArtifactResolver resolver, ArtifactFactory artifactFactory, BuildContextManager buildContextManager )
    {
        this.artifactResolver = resolver;
        this.artifactFactory = artifactFactory;
        this.buildContextManager = buildContextManager;
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineageBuilder#buildModelLineage(java.io.File, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List)
     */
    public ModelLineage buildModelLineage( File pom, ArtifactRepository localRepository, List remoteRepositories,
                                           ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuildCache projectBuildCache = ProjectBuildCache.read( buildContextManager );
        
        ModelLineage lineage = new DefaultModelLineage();

        File pomFile = pom;
        List currentRemoteRepositories = remoteRepositories == null ? new ArrayList()
                                                                   : new ArrayList( remoteRepositories );

        while ( pomFile != null )
        {
            Model model = readModel( pomFile, projectBuildCache );

            if ( lineage.size() == 0 )
            {
                lineage.setOrigin( model, pomFile, currentRemoteRepositories );
            }
            else
            {
                lineage.addParent( model, pomFile, currentRemoteRepositories );
            }

            currentRemoteRepositories = updateRepositorySet( model, currentRemoteRepositories, pomFile, profileManager );

            pomFile = resolveParentPom( model, currentRemoteRepositories, localRepository, pomFile,
                                        projectBuildCache );
        }

        return lineage;
    }

    public void resumeBuildingModelLineage( ModelLineage lineage, ArtifactRepository localRepository,
                                            ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuildCache projectBuildCache = ProjectBuildCache.read( buildContextManager );
        
        File pomFile = lineage.getDeepestFile();
        List currentRemoteRepositories = lineage.getDeepestArtifactRepositoryList();

        if ( currentRemoteRepositories == null )
        {
            currentRemoteRepositories = new ArrayList();
        }

        Model model = lineage.getDeepestModel();

        // use the above information to re-bootstrap the resolution chain...
        pomFile = resolveParentPom( model, currentRemoteRepositories, localRepository, pomFile, projectBuildCache );

        while ( pomFile != null )
        {
            model = readModel( pomFile, projectBuildCache );

            if ( lineage.size() == 0 )
            {
                lineage.setOrigin( model, pomFile, currentRemoteRepositories );
            }
            else
            {
                lineage.addParent( model, pomFile, currentRemoteRepositories );
            }

            currentRemoteRepositories = updateRepositorySet( model, currentRemoteRepositories, pomFile, profileManager );

            pomFile = resolveParentPom( model, currentRemoteRepositories, localRepository, pomFile,
                                        projectBuildCache );
        }
    }

    /**
     * Read the Model instance from the given POM file. Skip caching the Model on this call, since
     * it's meant for diagnostic purposes (to determine a parent match).
     */
    private Model readModel( File pomFile )
        throws ProjectBuildingException
    {
        return readModel( pomFile, null, true );
    }

    /**
     * Read the Model instance from the given POM file, and cache it in the given Map before 
     * returning it.
     */
    private Model readModel( File pomFile, ProjectBuildCache projectBuildCache )
        throws ProjectBuildingException
    {
        return readModel( pomFile, projectBuildCache, false );
    }

    /**
     * Read the Model instance from the given POM file. Optionally (in normal cases) cache the
     * Model instance in the given Map before returning it. The skipCache flag controls whether the
     * Model instance is actually cached.
     */
    private Model readModel( File pom, ProjectBuildCache projectBuildCache, boolean skipCache )
        throws ProjectBuildingException
    {
        File pomFile = pom;
        if ( pom.isDirectory() )
        {
            pomFile = new File( pom, "pom.xml" );
//            getLogger().debug( "readModel(..): POM: " + pom + " is a directory. Trying: " + pomFile + " instead." );
        }
        
        Model model;
        FileReader reader = null;

        try
        {
            reader = new FileReader( pomFile );
            model = new MavenXpp3Reader().read( reader );
        }
        catch ( IOException e )
        {
            throw new ProjectBuildingException( "unknown", "Failed to read model from: " + pomFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ProjectBuildingException( "unknown", "Failed to parse model from: " + pomFile, e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( !skipCache )
        {
            projectBuildCache.cacheModelFileForModel( pomFile, model );
            projectBuildCache.store( buildContextManager );
        }

        return model;
    }

    /**
     * Update the remote repository set used to resolve parent POMs, by adding those declared in 
     * the given model to the HEAD of a new list, then appending the old remote repositories list.
     * The specified pomFile is used for error reporting.
     * @param profileManager 
     */
    private List updateRepositorySet( Model model, List oldArtifactRepositories, File pomFile,
                                      ProfileManager externalProfileManager )
        throws ProjectBuildingException
    {
        List repositories = model.getRepositories();

        loadActiveProfileRepositories( repositories, model, externalProfileManager, pomFile.getParentFile() );

        Set artifactRepositories = null;

        if ( repositories != null )
        {
            try
            {
                List lastRemoteRepos = oldArtifactRepositories;
                List remoteRepos = mavenTools.buildArtifactRepositories( repositories );

                artifactRepositories = new LinkedHashSet( remoteRepos.size() + oldArtifactRepositories.size() );

                artifactRepositories.addAll( remoteRepos );
                artifactRepositories.addAll( lastRemoteRepos );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new ProjectBuildingException( model.getId(), "Failed to create ArtifactRepository list for: "
                    + pomFile, e );
            }
        }

        return new ArrayList( artifactRepositories );
    }

    private void loadActiveProfileRepositories( List repositories, Model model, ProfileManager profileManager,
                                                File projectDir )
        throws ProjectBuildingException
    {
        List explicitlyActive;
        List explicitlyInactive;

        if ( profileManager != null )
        {
            explicitlyActive = profileManager.getExplicitlyActivatedIds();
            explicitlyInactive = profileManager.getExplicitlyDeactivatedIds();
        }
        else
        {
            explicitlyActive = Collections.EMPTY_LIST;
            explicitlyInactive = Collections.EMPTY_LIST;
        }

        LinkedHashSet profileRepos = profileAdvisor.getArtifactRepositoriesFromActiveProfiles( model, projectDir,
                                                                                               explicitlyActive,
                                                                                               explicitlyInactive );

        if ( !profileRepos.isEmpty() )
        {
            repositories.addAll( profileRepos );
        }
    }

    /**
     * Pull the parent specification out of the given model, construct an Artifact instance, and
     * resolve that artifact...then, return the resolved POM file for the parent.
     * @param projectBuildCache 
     */
    private File resolveParentPom( Model model, List remoteRepositories, ArtifactRepository localRepository,
                                   File modelPomFile, ProjectBuildCache projectBuildCache )
        throws ProjectBuildingException
    {
        Parent modelParent = model.getParent();

        File pomFile = null;

        if ( modelParent != null )
        {
            validateParentDeclaration( modelParent, model );

//            getLogger().debug( "Looking for cached parent POM under: " + cacheKey );
            
            pomFile = (File) projectBuildCache.getCachedModelFile( modelParent );

            if ( pomFile == null )
            {
                pomFile = resolveParentWithRelativePath( modelParent, modelPomFile );
            }

            if ( pomFile == null )
            {
                pomFile = resolveParentFromRepositories( modelParent, localRepository, remoteRepositories, modelPomFile );
            }
        }

        return pomFile;
    }

    private void validateParentDeclaration( Parent modelParent, Model model )
        throws ProjectBuildingException
    {
        if ( StringUtils.isEmpty( modelParent.getGroupId() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Missing groupId element from parent element" );
        }
        else if ( StringUtils.isEmpty( modelParent.getArtifactId() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Missing artifactId element from parent element" );
        }
        else if ( modelParent.getGroupId().equals( model.getGroupId() )
            && modelParent.getArtifactId().equals( model.getArtifactId() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Parent element is a duplicate of "
                + "the current project " );
        }
        else if ( StringUtils.isEmpty( modelParent.getVersion() ) )
        {
            throw new ProjectBuildingException( model.getId(), "Missing version element from parent element" );
        }
    }

    private File resolveParentFromRepositories( Parent modelParent, ArtifactRepository localRepository,
                                                List remoteRepositories, File pomFile )
        throws ProjectBuildingException
    {
        Artifact parentPomArtifact = artifactFactory.createBuildArtifact( modelParent.getGroupId(), modelParent
            .getArtifactId(), modelParent.getVersion(), "pom" );

//        getLogger().debug( "Looking for parent: " + modelParent.getId() + " using artifact: " + parentPomArtifact );
//        getLogger().debug( "\tLocal repository: " + localRepository.getBasedir() + "\n" );
//        getLogger().debug( "\tRemote repositories:\n" + remoteRepositories.toString().replace( ',', '\n' ) + "\n" );

        try
        {
            artifactResolver.resolve( parentPomArtifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( "Parent: " + modelParent.getId(),
                                                "Failed to resolve POM for parent of: " + pomFile, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ProjectBuildingException( "Parent: " + modelParent.getId(), "Cannot find parent: "
                + parentPomArtifact.getId() + " of: " + pomFile, e );
        }

        if ( parentPomArtifact.isResolved() )
        {
            return parentPomArtifact.getFile();
        }
        else
        {
            return null;
        }
    }

    private File resolveParentWithRelativePath( Parent modelParent, File modelPomFile )
        throws ProjectBuildingException
    {
        String relativePath = modelParent.getRelativePath();
        File modelDir = modelPomFile.getParentFile();

        File parentPomFile = new File( modelDir, relativePath );

        if ( parentPomFile.isDirectory() )
        {
//            getLogger().debug( "Parent relative-path is a directory; assuming \'pom.xml\' file exists within." );
            parentPomFile = new File( parentPomFile, "pom.xml" );
        }

//        getLogger().debug( "Looking for parent: " + modelParent.getId() + " in: " + parentPomFile );

        if ( parentPomFile.exists() )
        {
            Model parentModel = readModel( parentPomFile );

            boolean groupsMatch = parentModel.getGroupId() == null
                || parentModel.getGroupId().equals( modelParent.getGroupId() );
            boolean versionsMatch = parentModel.getVersion() == null
                || parentModel.getVersion().equals( modelParent.getVersion() );

            if ( groupsMatch && versionsMatch && parentModel.getArtifactId().equals( modelParent.getArtifactId() ) )
            {
                return parentPomFile;
            }
        }

        return null;
    }

    private Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "DefaultModelLineageBuilder:internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
