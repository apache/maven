package org.apache.maven.plugin.jar;

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

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal ejb
 * @phase package
 * @description build an ejb
 * @parameter name="jarName"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.finalName"
 * description=""
 * @parameter name="compress"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.compress"
 * default="true"
 * description=""
 * @parameter name="index"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.index"
 * default="false"
 * description=""
 * @parameter name="package"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.package"
 * description=""
 * @parameter name="manifest"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.manifest"
 * description=""
 * @parameter name="mainClass"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.mainClass"
 * description=""
 * @parameter name="addClasspath"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.addClasspath"
 * default="false"
 * description=""
 * @parameter name="addExtensions"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.addExtensions"
 * default="false"
 * description=""
 * @parameter name="generateClient"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.ejb.generateclient"
 * default="false"
 * description=""
 * @parameter name="outputDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.outputDirectory"
 * description=""
 * @parameter name="basedir"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.directory"
 * description=""
 * @parameter name="project"
 * type="org.apache.maven.project.MavenProject"
 * required="true"
 * validator=""
 * expression="#project"
 * description="current MavenProject instance"
 */
public class EjbMojo
    extends AbstractPlugin
{
    /**
     * @todo Add license files in META-INF directory.
     */
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        File basedir = new File( (String) request.getParameter( "basedir" ) );

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String jarName = (String) request.getParameter( "jarName" );

        boolean generateClient = new Boolean( (String) request.getParameter( "generateClient" ) ).booleanValue();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        getLog().info( "Building ejb " + jarName );

        File jarFile = new File( basedir, jarName + ".jar" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setOutputFile( jarFile );

        String ejbJarXmlFile = "META-INF/ejb-jar.xml";

        archiver.getArchiver().addDirectory( new File( outputDirectory ), new String[]{"**/**"},
                                             new String[]{ejbJarXmlFile, "**/package.html"} );

        archiver.getArchiver().addFile( new File( outputDirectory, ejbJarXmlFile ), ejbJarXmlFile );

        // create archive
        archiver.createArchive( request );

        if ( generateClient )
        {
            getLog().info( "Building ejb client " + jarName + "-client" );

            File clientJarFile = new File( basedir, jarName + "-client.jar" );

            MavenArchiver clientArchiver = new MavenArchiver();

            clientArchiver.setOutputFile( jarFile );

            clientArchiver.getArchiver().addDirectory( new File( outputDirectory ), new String[]{"**/**"},
                                                 new String[]{"**/*Bean.class", "**/*CMP.class",
                                                              "**/*Session.class", "**/package.html"} );

            // create archive
            clientArchiver.createArchive( request );
        }
    }
}
