package org.apache.maven.artifact;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
    
    public static Map artifactMap( Collection artifacts )
    {
        Map artifactMap = new HashMap();
        
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            
            artifactMap.put( versionlessKey( artifact ), artifact );
        }
        
        return artifactMap;
    }

}
