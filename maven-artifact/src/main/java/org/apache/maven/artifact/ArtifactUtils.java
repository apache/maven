package org.apache.maven.artifact;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ArtifactUtils
{
    
    private ArtifactUtils()
    {
    }
    
    public static String versionlessKey( Artifact artifact )
    {
        return versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );
    }

    public static String versionlessKey( String groupId, String artifactId )
    {
        return groupId + ":" + artifactId;
    }
    
    public static String artifactId( String groupId, String artifactId, String type, String version )
    {
        return artifactId( groupId, artifactId, type, version, null, version );
    }
    
    public static String artifactId( String groupId, String artifactId, String type, String version, String classifier, String baseVersion )
    {
        return groupId + ":" + artifactId + ":" + type + ( ( classifier != null ) ? ( ":" + classifier ) : ( "" ) ) + ":" + baseVersion;
    }
    
    public static Map artifactMapByVersionlessId( Collection artifacts )
    {
        Map artifactMap = new HashMap();
        
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            
            artifactMap.put( versionlessKey( artifact ), artifact );
        }
        
        return artifactMap;
    }

    public static Map artifactMapByArtifactId( Collection artifacts )
    {
        Map artifactMap = new HashMap();
        
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            
            artifactMap.put( artifact.getId(), artifact );
        }
        
        return artifactMap;
    }

}
