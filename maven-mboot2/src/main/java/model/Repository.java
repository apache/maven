package model;

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

import java.io.File;

/**
 * Repository path management.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class Repository
{
    public static final String LAYOUT_LEGACY = "legacy";

    public static final String LAYOUT_DEFAULT = "default";

    private String basedir;

    private String layout;

    private String id;

    private boolean releases;

    private boolean snapshots;

    public Repository()
    {
    }

    public Repository( String id, String basedir, String layout, boolean snapshots, boolean releases )
    {
        this.id = id;
        this.basedir = basedir;
        this.layout = layout;
        this.snapshots = snapshots;
        this.releases = releases;
    }

    public File getArtifactFile( String groupId, String artifactId, String version, String type )
    {
        Dependency d = new Dependency( groupId, artifactId, version, type );

        return getArtifactFile( d );

    }

    public File getArtifactFile( Dependency dependency )
    {
        String repositoryPath = getArtifactPath( dependency );

        return new File( basedir, repositoryPath );
    }

    public String getArtifactPath( Dependency dependency )
    {
        String repositoryPath;
        if ( LAYOUT_LEGACY.equals( layout ) )
        {
            repositoryPath = dependency.getArtifactDirectory() + "/" + dependency.getType() + "s/" +
                dependency.getArtifact();
        }
        else if ( LAYOUT_DEFAULT.equals( layout ) )
        {
            repositoryPath = dependency.getGroupId().replace( '.', '/' );
//            if ( !dependency.getType().equals( "pom" ) )
//            {
            repositoryPath = repositoryPath + "/" + dependency.getArtifactId() + "/" + dependency.getVersion();
//            }
            repositoryPath = repositoryPath + "/" + dependency.getArtifact();
        }
        else
        {
            throw new IllegalStateException( "Unknown layout: " + layout );
        }
        return repositoryPath;
    }

    public File getMetadataFile( String groupId, String artifactId, String version, String type, String filename )
    {
        String repositoryPath = getMetadataPath( groupId, artifactId, version, type, filename );

        return new File( basedir, repositoryPath );
    }

    public String getMetadataPath( String groupId, String artifactId, String version, String type, String filename )
    {
        Dependency dependency = new Dependency( groupId, artifactId, version, type );

        String repositoryPath;
        if ( LAYOUT_LEGACY.equals( layout ) )
        {
            repositoryPath = dependency.getArtifactDirectory() + "/poms/" + filename;
        }
        else if ( LAYOUT_DEFAULT.equals( layout ) )
        {
            repositoryPath = dependency.getGroupId().replace( '.', '/' );
//            if ( !dependency.getType().equals( "pom" ) )
//            {
            repositoryPath = repositoryPath + "/" + dependency.getArtifactId();
            if ( version != null )
            {
                repositoryPath = repositoryPath + "/" + dependency.getVersion();
            }
//            }
            repositoryPath = repositoryPath + "/" + filename;
        }
        else
        {
            throw new IllegalStateException( "Unknown layout: " + layout );
        }
        return repositoryPath;
    }

    public String toString()
    {
        return basedir;
    }

    public String getBasedir()
    {
        return basedir;
    }

    public void setBasedir( String basedir )
    {
        this.basedir = basedir;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getLayout()
    {
        return layout;
    }

    public void setReleases( boolean releases )
    {
        this.releases = releases;
    }

    public void setSnapshots( boolean snapshots )
    {
        this.snapshots = snapshots;
    }

    public boolean isReleases()
    {
        return releases;
    }

    public boolean isSnapshots()
    {
        return snapshots;
    }
}
