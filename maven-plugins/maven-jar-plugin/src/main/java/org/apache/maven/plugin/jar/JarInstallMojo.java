package org.apache.maven.plugin.jar;

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

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;


/**
 * @goal install
 *
 * @description install a jar in local repository
 * 
 * @prereq jar:jar
 *
 * @parameter name=jarName type=String required=true validator= expression=#maven.final.name
 * @parameter name=outputDirectory type=String required=true validator= expression=#project.build.directory
 * @parameter name=basedir type=String required=true validator= expression=#project.build.directory
 * @parameter name=groupId type=String required=true validator= expression=#project.groupId
 * @parameter name=artifactId type=String required=true validator= expression=#project.artifactId
 * @parameter name=version type=String required=true validator= expression=#project.version
 * @parameter name=localRepository type=String required=true validator= expression=#project.localRepository
 * @parameter name=pomFile type=java.io.File required=true validator= expression=#project.file
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class JarInstallMojo
        extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
            throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------


        String outputDirectory = ( String ) request.getParameter( "outputDirectory" );

        String jarName = ( String ) request.getParameter( "jarName" );

        String groupId = ( String ) request.getParameter( "groupId" );

        String artifactId = ( String ) request.getParameter( "artifactId" );

        //@todo if we have SNAPSHOT version should we do something special?

        String version = ( String ) request.getParameter( "version" );

        File pomFile = ( File ) request.getParameter( "pomFile" );

        String localRepository = ( String ) request.getParameter( "localRepository" );

        File jarFile = new File( outputDirectory, jarName + ".jar" );

        try
        {
            String jarPath = groupId + "/poms/" + artifactId + "-" + version + ".pom";

            //here I imagine that also something like this can be made
            //
            //  Dependecy = new Dependecy();
            //  dependecy.setGroupid( groupId )
            //     ....
            // MavenArtifact artifact = artifactFactory.createArtifact( dependecy )
            //
            // so maven artifact factory will be centralized service for creating
            // repository paths
            //
            // I am not sure if this is good option but it is something which might be considered  
            installArtifact( jarPath, jarFile, localRepository );

        }
        catch ( Exception e )
        {
            response.setException( e );

            return;
        }

        try
        {
            String pomPath = groupId + "/poms/" + artifactId + "-" + version + ".pom";

            installArtifact( pomPath, pomFile, localRepository );
        }
        catch ( Exception e )
        {
            // @todo what shall we do when jar was installed but we failed to install pom?

            // response.setException ( e );

        }


    }

    //@todo do we need to crate md5 checksums in local repsitory?
    //I think it would be nice if any local repository could be
    // and at any moment in time used as remote repository
    // so content of both repositories should be symetrical
    private void installArtifact( String path, File source, String localRepository ) throws Exception
    {

        File destination = new File( localRepository, path );


        // @todo should we use plexus classes?
        FileUtils.fileCopy( source.getPath(), destination.getPath() );

        // @todo we can use as well file wagon here.

//        FileWagon wagon = new FileWagon();
//
//        TransferObserver observer = new ChecksumObserver()
//
//        wagon.addTransferObserver( observer );
//
//        Repository repository = new Repository( "file://xxxx" );
//
//        wagon.connect( repository );
//
//        wagon.put( path, file );

        // and wagon has also built-in support for <<artifacts>> which is not used
    }


}
