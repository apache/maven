package org.apache.maven.repository.checker;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;

public class CheckRepo
{
    public static void main( String[] args )
    {
        if ( args.length != 1 )
        {
            System.out.println( "Usage: " + CheckRepo.class.getName() + " <repository path>" );
            System.exit( 1 );
        }

        File rootDir = new File( args[0] );
        if ( !rootDir.exists() )
        {
            System.out.println( rootDir.getAbsolutePath() + " doesn't exist." );

            System.exit( 1 );
        }

        String[] extensions = { "pom" };

        String[] files = FileUtils.getFilesFromExtension( rootDir.getAbsolutePath(), extensions );

        for ( int i = 0; i < files.length; i++ )
        {
            //System.out.println( files[i] );
            parseFile( new File( files[i] ) );
        }
    }

    private static void parseFile( File file )
    {
        try
        {
            FileReader fileReader = new FileReader( file );

            MavenXpp3Reader reader = new MavenXpp3Reader();

            reader.read( fileReader );
        }
        catch ( Exception e )
        {
            System.out.println( "================================================" );

            System.out.println( file.getAbsolutePath() );

            e.printStackTrace();

            System.out.println( "================================================" );
        }
    }
}
