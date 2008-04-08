package org.apache.maven.project.workspace;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.build.model.ModelAndFile;
import org.apache.maven.workspace.MavenWorkspaceStore;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Map;

public class DefaultProjectWorkspace
    implements ProjectWorkspace, LogEnabled
{

    private MavenWorkspaceStore workspaceStore;
    private Logger logger;

    public DefaultProjectWorkspace()
    {
    }

    protected DefaultProjectWorkspace( MavenWorkspaceStore workspaceStore, Logger logger )
    {
        this.workspaceStore = workspaceStore;
        this.logger = logger;
    }

    public ModelAndFile getModelAndFile( String groupId,
                                         String artifactId,
                                         String version )
    {
        Map cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.MODEL_AND_FILE_BYGAV_KEY );

        String key = createCacheKey( groupId, artifactId, version );

//        getLogger().debug( "Retrieving ModelAndFile instance for: " + key + " from workspace." );
        return (ModelAndFile) cache.get( key );
    }

    public ModelAndFile getModelAndFile( File modelFile )
    {
        Map cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.MODEL_AND_FILE_BYFILE_KEY );

        Object pathKey = resolvePathKey( modelFile );

//        getLogger().debug( "Retrieving ModelAndFile instance for: " + pathKey + " from workspace." );
        return (ModelAndFile) cache.get( pathKey );
    }

    private Object resolvePathKey( File file )
    {
        if ( file == null )
        {
            return null;
        }

        return file.toURI().normalize().toString();
    }

    public MavenProject getProject( File projectFile )
    {
        Map cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.PROJECT_INSTANCE_BYFILE_KEY );

        Object pathKey = resolvePathKey( projectFile );

//        getLogger().debug( "Retrieving MavenProject instance for: " + pathKey + " from workspace." );
        return (MavenProject) cache.get( pathKey );
    }

    public MavenProject getProject( String groupId,
                                    String artifactId,
                                    String version )
    {
        Map cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.PROJECT_INSTANCE_BYGAV_KEY );

        String key = createCacheKey( groupId, artifactId, version );

//        getLogger().debug( "Retrieving MavenProject instance for: " + key + " from workspace." );
        return (MavenProject) cache.get( key );
    }

    public void storeModelAndFile( ModelAndFile modelAndFile )
    {
        Map cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.MODEL_AND_FILE_BYFILE_KEY );

        Object pathKey = resolvePathKey( modelAndFile.getFile() );

//        getLogger().debug( "Storing ModelAndFile instance under: " + pathKey + " in workspace." );
        cache.put( pathKey, modelAndFile );

        cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.MODEL_AND_FILE_BYGAV_KEY );

        Model model = modelAndFile.getModel();
        String key = createCacheKey( model.getGroupId(), model.getArtifactId(), model.getVersion() );
        String keyWithParent = createCacheKeyUsingParent( model );

//        getLogger().debug( "Storing ModelAndFile instance under: " + key + " in workspace." );
        cache.put( key, modelAndFile );

        if ( !key.equals( keyWithParent ) )
        {
//            getLogger().debug( "Also Storing ModelAndFile instance using groupId/version information from parent, under: " + keyWithParent + " in workspace." );
            cache.put( keyWithParent, modelAndFile );
        }
    }

    private String createCacheKeyUsingParent( Model model )
    {
        String groupId = model.getGroupId();
        String version = model.getVersion();

        Parent parent = model.getParent();
        if ( parent != null )
        {
            if ( groupId == null )
            {
                groupId = parent.getGroupId();
            }

            if ( version == null )
            {
                version = parent.getVersion();
            }
        }

        return createCacheKey( groupId, model.getArtifactId(), version );
    }

    public void storeProjectByFile( MavenProject project )
    {
        if ( project.getFile() == null ){
            return;
        }

        Map cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.PROJECT_INSTANCE_BYFILE_KEY );

        Object pathKey = resolvePathKey( project.getFile() );

//        getLogger().debug( "Storing MavenProject instance under: " + pathKey + " in workspace." );
        cache.put( pathKey, project );
    }

    public void storeProjectByCoordinate( MavenProject project )
    {
        Map cache = workspaceStore.getWorkspaceCache( ProjectWorkspace.PROJECT_INSTANCE_BYGAV_KEY );

        String key = createCacheKey( project.getGroupId(), project.getArtifactId(), project.getVersion() );

//        getLogger().debug( "Storing MavenProject instance under: " + key + " in workspace." );
        cache.put( key, project );
    }

    private String createCacheKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    protected Logger getLogger()
    {
//        if ( logger == null )
//        {
//            logger = new ConsoleLogger( Logger.LEVEL_INFO, "internal" );
//        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
