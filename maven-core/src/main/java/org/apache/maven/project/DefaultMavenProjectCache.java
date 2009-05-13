package org.apache.maven.project;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;

@Component(role=MavenProjectCache.class)
public class DefaultMavenProjectCache
    implements MavenProjectCache
{
    private Map<String, MavenProject> projectCache = new HashMap<String, MavenProject>();

    public MavenProject get( String key )
    {
        return projectCache.get( key );
    }

    public void put( String key, MavenProject project )
    {
        projectCache.put( key, project );        
    }
    
    public int size()
    {
        return projectCache.size();
    }
}
