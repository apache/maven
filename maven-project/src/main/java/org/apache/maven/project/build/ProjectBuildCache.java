package org.apache.maven.project.build;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This cache is meant to provide a reference of the project instances that are in the current build
 * in order to avoid storing a hidden caching mechanism in the project builder, and avoid the need
 * to store inter-project references as such in MavenProject instances. An ArtifactResolver instance
 * will be used that can utilize this cache, but will also wrap the "default" ArtifactResolver
 * instance, so that can be used as a target for replacement implementations.
 * 
 * To retrieve from the build context: buildContext.retrieve( new ProjectBuildCache( false ) );
 * 
 * @author jdcasey
 */
public class ProjectBuildCache
    implements ManagedBuildData
{
    
    private static final String BUILD_CONTEXT_KEY = ProjectBuildCache.class.getName();
    
    private static final String PROJECT_CACHE = "project-cache";
    
    private static final String POM_FILE_CACHE = "pom-file-cache";
    
    private Map projectCache;
    
    private Map pomFileCache;
    
    public ProjectBuildCache()
    {
        this( true );
    }
    
    /**
     * @param liveInstance If false, this instance's state is meant to be retrieved from the build
     *   context. If true, this instance can serve as the authoritative instance where the cache is
     *   established.
     */
    public ProjectBuildCache( boolean liveInstance )
    {
        if ( liveInstance )
        {
            projectCache = new HashMap();
            pomFileCache = new HashMap();
        }
    }
    
    public void cacheProject( MavenProject project )
    {
        projectCache.put( generateCacheKey( project ), project );
    }
    
    public MavenProject getCachedProject( String groupId, String artifactId, String version )
    {
        return (MavenProject) projectCache.get( generateCacheKey( groupId, artifactId, version ) );
    }
    
    public MavenProject getCachedProject( Artifact artifact )
    {
        return (MavenProject) projectCache.get( generateCacheKey( artifact ) );
    }
    
    public MavenProject getCachedProject( MavenProject exampleInstance )
    {
        return (MavenProject) projectCache.get( generateCacheKey( exampleInstance ) );
    }

    public void cacheModelFileForModel( File modelFile, Model model )
    {
        pomFileCache.put( generateCacheKey( model ), modelFile );
    }
    
    public File getCachedModelFile( Artifact artifact )
    {
        return (File) pomFileCache.get( generateCacheKey( artifact ) );
    }

    public File getCachedModelFile( Parent parent )
    {
        return (File) pomFileCache.get( generateCacheKey( parent ) );
    }

    public File getCachedModelFile( String groupId, String artifactId, String version )
    {
        return (File) pomFileCache.get( generateCacheKey( groupId, artifactId, version ) );
    }

    public Map getData()
    {
        Map data = new HashMap( 2 );
        
        data.put( PROJECT_CACHE, projectCache );
        data.put( POM_FILE_CACHE, pomFileCache );
        
        return data;
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }

    public void setData( Map data )
    {
        this.projectCache = (Map) data.get( PROJECT_CACHE );
        this.pomFileCache = (Map) data.get( POM_FILE_CACHE );
    }

    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        buildContext.store( this );
        buildContextManager.storeBuildContext( buildContext );
    }

    public static ProjectBuildCache read( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        ProjectBuildCache cache = new ProjectBuildCache();
        
        buildContext.retrieve( cache );
        
        return cache;
    }

    private static String generateCacheKey( Model model )
    {
        Parent modelParent = model.getParent();

        String groupId = model.getGroupId();

        if ( groupId == null && modelParent != null )
        {
            groupId = modelParent.getGroupId();
        }

        String artifactId = model.getArtifactId();

        String version = model.getVersion();

        if ( version == null && modelParent != null )
        {
            version = modelParent.getVersion();
        }

        return generateCacheKey( groupId, artifactId, version );
    }
    
    private static String generateCacheKey( Parent parent )
    {
        return generateCacheKey( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
    }
    
    private static String generateCacheKey( Artifact artifact )
    {
        return generateCacheKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
    }
    
    private static String generateCacheKey( MavenProject project )
    {
        return generateCacheKey( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }
    
    private static String generateCacheKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

}
