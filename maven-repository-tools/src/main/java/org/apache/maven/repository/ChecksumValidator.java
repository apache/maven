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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Prints all artifacts without checksum file
 * 
 * @todo generate checksums for those files without it
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez </a>
 * @version $Id$
 */

public class ChecksumValidator
{

    public static void main( String[] args )
    {
        if ( args.length != 1 )
        {
            System.out.println( "Usage: " + ChecksumValidator.class.getName() + " path.to.local.repository" );

            return;
        }

        ArtifactRepository localRepository = new ArtifactRepository();

        String path = args[0];

        File f = new File( path );

        localRepository.setUrl( "file://" + f.getPath() );

        List artifacts = RepositoryTools.getAllArtifacts( localRepository );

        Iterator it = artifacts.iterator();

        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();

            if ( !artifact.getChecksumFile().exists() )
            {
                System.out.println( artifact );
            }
        }
    }

}
