package org.apache.maven.converter;

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

import org.apache.maven.model.Model;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l </a>
 * @version $Id$
 */
public class RepoReaper
{
    MavenRepository inRepository = new Maven1Repository();

    MavenRepository outRepository = new Maven2Repository();

    public static void main( String[] args )
        throws Exception
    {
        if ( args.length != 2 )
        {
            System.err.println( "Usage: repoReaper <input repo> <output repo>" );

            return;
        }

        File inbase = new File( args[0] );

        File outbase = new File( args[1] );

        if ( !inbase.exists() )
        {
            System.err.println( "input repo doesn't exist: " + args[0] );

            return;
        }

        if ( !outbase.exists() )
        {
            System.err.println( "output repo doesn't exist: " + args[0] );

            return;
        }

        new RepoReaper().work( inbase, outbase );
    }

    public void work( File inbase, File outbase )
        throws Exception
    {
        inRepository.setRepository( inbase );

        System.out.println( "Input basedir: " + inbase.getAbsolutePath() );

        outRepository.setRepository( outbase );

        System.out.println( "Output basedir: " + outbase.getAbsolutePath() );

        Iterator files = inRepository.getArtifactsByType( "jar" );

        while( files.hasNext() )
        {
            File file = (File) files.next();

            String filePath = file.getAbsolutePath().substring( inbase.getAbsolutePath().length() + 1 );

            Model project;

            String pomPath = inRepository.getPomForArtifact( filePath );

            if ( pomPath == null )
            {
                warning( "Missing pom for artifact: " + filePath );

                continue;
            }

            try
            {
//                project = convertPom( pomPath, new File( inRepository.getRepository(), pomPath ) );

                PomV3ToV4Converter converter = new PomV3ToV4Converter();

                project = converter.convertFile( new File( inRepository.getRepository(), pomPath ) );
            }
            catch( Exception ex )
            {
                warning( "Could not parse: '" + pomPath + "'.");

                Throwable t = ex;

                while ( t != null )
                {
                    warning( "  " + ex.getMessage() );

                    t = t.getCause();
                }

                continue;
            }

            if ( project != null )
            {
/*
                File tmp = File.createTempFile( "maven-repo-reaper", "tmp" );

                write( filePath, project );
*/
                outRepository.installArtifact( new File( inbase, filePath ), project );
            }
        }
    }
/*
    private Model convertPom( String fileName, File file )
        throws Exception
    {
        System.out.println( "Processing " + fileName );

        SAXReader r = new SAXReader();

        Model project = new Model();

        Document pom = r.read( new FileReader( file ) );

        Element root = pom.getRootElement();

        Element modelVersion = root.element( "modelVersion" );

        if ( modelVersion != null )
        {
            if ( modelVersion.getText().equals( "4.0.0") )
            {
                System.out.println( "This pom is already v4.0.0: " + fileName );

                return null;
            }

            fatal( "Invalid pom, <modelVersion> exists but isn't '4.0.0': " + fileName );
        }

        project.setModelVersion( "4.0.0" );

        // Fixing <id> => <groupId>+<artifactId>
        Element id = root.element( "id" );

        Element groupId = root.element( "groupId" );

        Element artifactId = root.element( "artifactId" );

        if ( groupId != null )
        {
            project.setGroupId( groupId.getText() );
        }
        else
        {
            if ( id == null )
            {
                fatal( "Missing both <id> and <groupId>" );
            }

            project.setGroupId( id.getText() );
        }

        if ( artifactId != null )
        {
            project.setArtifactId( artifactId.getText() );
        }
        else
        {
            if ( id == null )
            {
                fatal( "Missing both <id> and <artifactId>" );
            }

            project.setArtifactId( id.getText() );
        }

        Element currentVersion = root.element( "currentVersion" );

        if ( currentVersion == null )
        {
            int start = -1;

            for ( int i = 0; i < fileName.length(); i++ )
            {
                if ( Character.isDigit( fileName.charAt( i ) ) )
                {
                    start = i;

                    break;
                }
            }

            if ( start == -1 )
            {
                fatal( "Missing <currentVersion>." );
            }

            String ver = fileName.substring( start, fileName.length() - 4  );

            project.setVersion( ver );

            warning( "Missing <currentVersion>, found the version from the filename, version: " + ver );
        }
        else
        {
            project.setVersion( currentVersion.getText() );
        }

        Element dependencies = root.element( "dependencies" );

        if ( dependencies != null )
        {
            Iterator it = dependencies.elementIterator( "dependency" );

            while( it.hasNext() )
            {
                Dependency d = new Dependency();

                Element dependency = (Element) it.next();

                id = dependency.element( "id" );

                groupId = dependency.element( "groupId" );

                artifactId = dependency.element( "artifactId" );

                Element type = dependency.element( "type" );

                Element version = dependency.element( "version" );

                if ( groupId == null )
                {
                    if ( id == null )
                    {
                        warning( "Missing both dependency.id and dependency.groupId." );

                        continue;
                    }
                    else
                    {
                        d.setGroupId( id.getText() );
                    }
                }
                else
                {
                    d.setGroupId( groupId.getText() );
                }

                if ( artifactId == null )
                {
                    if ( id == null )
                    {
                        warning( "Missing both dependency.id and dependency.artifactId. groupId: " + groupId.getText() );

                        continue;
                    }
                    else
                    {
                        d.setArtifactId( id.getText() );
                    }
                }
                else
                {
                    d.setArtifactId( artifactId.getText() );
                }

                if ( type != null )
                {
                    d.setType( type.getText() );
                }

                if ( version == null )
                {
                    warning( "Missing dependency.version for " + d.getGroupId() + ":" + d.getArtifactId() );

                    continue;
                }

                d.setVersion( version.getText() );

                if ( d.getGroupId().trim().equals( "${pom.groupId}" ) )
                {
                    d.setGroupId( project.getGroupId() );
                }

                if ( d.getArtifactId().trim().equals( "${pom.artifactId}" ) )
                {
                    d.setArtifactId( project.getArtifactId() );
                }

                if ( d.getType().trim().equals( "${pom.type}" ) )
                {
                    d.setType( project.getType() );
                }

                if ( d.getVersion().trim().equals( "${pom.version}" ) )
                {
                    d.setVersion( project.getVersion() );
                }

                project.getDependencies().add( d );
            }
        }

        return project;
    }*/
/*
    private static void write( File output, Model project )
        throws Exception
    {
        output.getParentFile().mkdirs();

        XStream xstream = new XStream();

        xstream.alias( "project", Model.class );
        xstream.alias( "dependency", Dependency.class );

//        String xml = xstream.toXML( project );

//        System.out.println( xml );

        FileWriter writer = new FileWriter( output );

        xstream.toXML( project, new PrettyPrintXMLWriter( writer ) );

        writer.close();
    }
*/
    private void fatal( String msg )
        throws Exception
    {
        throw new Exception( msg );
    }

    private void warning( String msg )
    {
        System.err.println( msg );
    }
}
