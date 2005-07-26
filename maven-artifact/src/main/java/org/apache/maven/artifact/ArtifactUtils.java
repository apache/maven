package org.apache.maven.artifact;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        return artifactId( groupId, artifactId, type, null, version );
    }

    public static String artifactId( String groupId, String artifactId, String type, String classifier,
                                     String baseVersion )
    {
        return groupId + ":" + artifactId + ":" + type + ( classifier != null ? ":" + classifier : "" ) + ":" +
            baseVersion;
    }

    public static Map artifactMapByVersionlessId( Collection artifacts )
    {
        Map artifactMap = new HashMap();
        
        if ( artifacts != null )
        {
            for ( Iterator it = artifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                artifactMap.put( versionlessKey( artifact ), artifact );
            }
        }

        return artifactMap;
    }

    public static Map artifactMapByArtifactId( Collection artifacts )
    {
        Map artifactMap = new HashMap();

        if ( artifacts != null )
        {
            for ( Iterator it = artifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                artifactMap.put( artifact.getId(), artifact );
            }
        }

        return artifactMap;
    }

}
