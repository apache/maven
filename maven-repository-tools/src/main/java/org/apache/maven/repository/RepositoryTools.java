package org.apache.maven.repository;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Tools to help repository management
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez </a>
 * @version $Id$
 */

public class RepositoryTools
{

    /**
     * Files with these extensions will be ignored
     */
    private static final String[] IGNORE_EXTENSIONS = { "md5" };

    /**
     * Get all artifacts present in a file repository
     * 
     * @todo make this layout independent
     * 
     * @param artifactRepository
     *            all files in this repository basedir will be returned as
     *            artifacts
     * @return all the artifacts available in the repository
     */
    public static List getAllArtifacts( ArtifactRepository artifactRepository )
    {
        List list = new LinkedList();

        File basedir = new File( artifactRepository.getBasedir() );

        File[] groups = basedir.listFiles();

        for ( int i = 0; (groups != null) && (i < groups.length); i++ )
        {
            File[] types = groups[i].listFiles();

            for ( int j = 0; (types != null) && (j < types.length); j++ )
            {

                File[] artifacts = types[j].listFiles( new FilenameFilter() {

                    public boolean accept( File dir, String name )
                    {
                        int x = name.lastIndexOf( "." );

                        String extension = name.substring( x, name.length() );

                        for ( int y = 0; y < IGNORE_EXTENSIONS.length; y++ )
                        {

                            if ( extension.equals( IGNORE_EXTENSIONS[y] ) )

                                return false;
                        }

                        return true;
                    }
                } );

                for ( int k = 0; (artifacts != null) && (k < artifacts.length); k++ )
                {
                    Artifact artifact = getArtifact( groups[i].getName(), types[j].getName().substring( 0,
                        types[j].getName().length() - 1 ), artifacts[k] );

                    if ( artifact != null )

                        list.add( artifact );
                }
            }
        }

        return list;
    }

    /**
     * Construct an artifact object from a file
     * 
     * @todo make this layout independent
     * 
     * @param groupId
     * @param type
     * @param artifactFile
     *            artifactId, version and path will be get from this file name
     * @return
     */
    public static Artifact getArtifact( String groupId, String type, File artifactFile )
    {
        String name = artifactFile.getName();

        int i = name.lastIndexOf( "-" );

        int j = name.lastIndexOf( "." );

        if ( (i < 0) || (j < 0) )
        {
            return null;
        }

        String version = name.substring( i + 1, j );

        String artifactId = name.substring( 0, i );

        Artifact artifact = new DefaultArtifact( groupId, artifactId, version, type );

        artifact.setPath( artifactFile.getPath() );

        return artifact;
    }

}
