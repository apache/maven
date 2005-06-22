package org.apache.maven.artifact.resolver;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Iterator;
import java.util.List;

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

    private String downloadUrl;

    private List remoteRepositories;

    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, String downloadUrl, Throwable t )
    {
        super( constructMessage( message, groupId, artifactId, version, type, remoteRepositories, downloadUrl ), t );

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.remoteRepositories = remoteRepositories;
        this.downloadUrl = downloadUrl;
    }

    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, String downloadUrl )
    {
        super( constructMessage( message, groupId, artifactId, version, type, remoteRepositories, downloadUrl ) );

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.remoteRepositories = remoteRepositories;
        this.downloadUrl = downloadUrl;
    }

    private static final String LS = System.getProperty( "line.separator" );

    private static String constructMessage( String message, String groupId, String artifactId, String version,
                                            String type, List remoteRepositories, String downloadUrl )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( message );
        sb.append( LS );
        sb.append( "  " + groupId + ":" + artifactId + ":" + version + ":" + type );
        sb.append( LS );
        if ( remoteRepositories != null && !remoteRepositories.isEmpty() )
        {
            sb.append( LS );
            sb.append( "from the specified remote repositories:" );
            sb.append( LS + "  " );

            for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
            {
                ArtifactRepository remoteRepository = (ArtifactRepository) i.next();

                sb.append( remoteRepository.getUrl() );
                if ( i.hasNext() )
                {
                    sb.append( ", " );
                }
            }
        }

        if ( downloadUrl != null && !type.equals( "pom" ) )
        {
            sb.append( LS );
            sb.append( LS );
            sb.append( "Try downloading the file manually from" );
            sb.append( LS );
            sb.append( "  " + downloadUrl );
            sb.append( LS );
            sb.append( "and install it using the command: " );
            sb.append( LS );
            sb.append( "  m2 install:install-file -DgroupId=" );
            sb.append( groupId );
            sb.append( " -DartifactId=" );
            sb.append( artifactId );
            sb.append( " -Dversion=" );
            sb.append( version );
            sb.append( " -Dpackaging=" );
            sb.append( type );
            sb.append( " -Dfile=/path/to/file" );
        }

        return sb.toString();
    }

    public ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories, Throwable t )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
              remoteRepositories, artifact.getDownloadUrl(), t );
    }

    public ArtifactResolutionException( String message, Artifact artifact )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), null,
              artifact.getDownloadUrl() );
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

    public String getDownloadUrl()
    {
        return downloadUrl;
    }
}
