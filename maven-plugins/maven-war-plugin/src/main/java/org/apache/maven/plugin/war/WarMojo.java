package org.apache.maven.plugin.war;

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

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.project.MavenProject;

/**
 * @goal war
 * @phase package
 *
 * @description build a jar
 *
 * @parameter
 *  name="jarName"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.finalName"
 *  description=""
 * @parameter
 *  name="compress"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.jar.compress"
 *  default="true"
 *  description=""
 * @parameter
 *  name="index"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.jar.index"
 *  default="false"
 *  description=""
 * @parameter
 *  name="manifest"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.jar.manifest"
 *  description=""
 * @parameter
 *  name="mainClass"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.jar.mainClass"
 *  description=""
 * @parameter
 *  name="addClasspath"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.jar.addClasspath"
 *  default="false"
 *  description=""
 * @parameter
 *  name="addExtensions"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.jar.addExtensions"
 *  default="false"
 *  description=""
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.output"
 *  description=""
 * @parameter
 *  name="basedir"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory"
 *  description=""
 * @parameter
 *  name="project"
 *  type="org.apache.maven.project.MavenProject"
 *  required="true"
 *  validator=""
 *  expression="#project"
 *  description="current MavenProject instance"
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class WarMojo
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------
System.out.println("war");

    }
}
