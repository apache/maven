package org.apache.maven.plugin.install;

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
import org.apache.maven.project.MavenProject;
import org.apache.maven.MavenCore;

import java.util.ArrayList;
import java.util.List;

/**
 * @goal install
 *
 * @description installs project's main artifact in local repository
 *
* @parameter
 *  name="project"
 *  type="org.apache.maven.project.MavenProject"
 *  required="true"
 *  validator=""
 *  expression="#project"
 *  description=""
 *
 * @parameter
 *  name="mavenCore"
 *  type="org.apache.maven.MavenCore"
 *  required="true"
 *  validator=""
 *  expression="#component.org.apache.maven.MavenCore"
 *  description=""""
 *
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class InstallMojo
    extends AbstractPlugin
{

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {

        MavenProject project = (MavenProject) request.getParameter( "project" );

        String type = project.getType();

        MavenCore mavenCore = ( MavenCore ) request.getParameter( "mavenCore" );

        String goal = type + ":install";

        List goals = new ArrayList( 1 );

        goals.add( goal );

        mavenCore.execute( project, goals );

    }


}
