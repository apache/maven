package org.apache.maven.project.canonical;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.AbstractMavenProjectTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class CanonicalProjectBuilderTest
    extends AbstractMavenProjectTestCase
{
    public void testProjectBuilder()
        throws Exception
    {
        File f = getFileForClasspathResource( "canonical-pom.xml" );

        MavenProject project = getProject( f );

        // ----------------------------------------------------------------------
        // Top-level elements
        // ----------------------------------------------------------------------

        assertEquals( "4.0.0", project.getModelVersion() );

        // ----------------------------------------------------------------------
        // Plugins
        // ----------------------------------------------------------------------

        List plugins = project.getBuildPlugins();

        // Plugin0 [plexus]

        String key = "org.apache.maven.plugins:maven-plexus-plugin";

        Plugin plugin = null;
        for ( Iterator it = plugins.iterator(); it.hasNext(); )
        {
            Plugin check = (Plugin) it.next();

            if ( key.equals( check.getKey() ) )
            {
                plugin = check;
                break;
            }
        }

        assertNotNull( plugin );

        assertEquals( "1.0", plugin.getVersion() );

        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

        assertEquals( "src/conf/plexus.conf", configuration.getChild( "plexusConfiguration" ).getValue() );

        assertEquals( "src/conf/plexus.properties",
                      configuration.getChild( "plexusConfigurationPropertiesFile" ).getValue() );

        assertEquals( "Continuum", configuration.getChild( "plexusApplicationName" ).getValue() );

        // ----------------------------------------------------------------------
        // Goal specific configuration
        // ----------------------------------------------------------------------

        List executions = plugin.getExecutions();

        PluginExecution execution = (PluginExecution) executions.get( 0 );

        String g0 = (String) execution.getGoals().get( 0 );

        assertEquals( "plexus:runtime", g0 );

        configuration = (Xpp3Dom) execution.getConfiguration();

        assertEquals( "ContinuumPro", configuration.getChild( "plexusApplicationName" ).getValue() );

        // Plugin1 [antlr]
    }
}
