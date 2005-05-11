package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Iterator;
import java.util.List;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArtifactResolutionException
    extends Exception
{
    private String groupId;

    private String artifactId;

    private String version;

    private String type;

    private List remoteRepositories;

    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, Throwable t )
    {
        super( constructMessage( message, groupId, artifactId, version, type, remoteRepositories ), t );

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.remoteRepositories = remoteRepositories;
    }

    private static final String LS = System.getProperty( "line.separator" );

    private static String constructMessage( String message, String groupId, String artifactId, String version,
                                            String type, List remoteRepositories )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( message );
        sb.append( LS );
        sb.append( LS );
        sb.append( groupId + ":" + artifactId + ":" + version + ":" + type );
        sb.append( LS );
        sb.append( LS );
        sb.append( "from the specified remote repositories:" );
        sb.append( LS );
        sb.append( LS );

        for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository remoteRepository = (ArtifactRepository) i.next();

            sb.append( remoteRepository.getUrl() );
            if ( i.hasNext() )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }

    public ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories, Throwable t )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
              remoteRepositories, t );
    }

    public ArtifactResolutionException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getType()
    {
        return type;
    }

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }
}
