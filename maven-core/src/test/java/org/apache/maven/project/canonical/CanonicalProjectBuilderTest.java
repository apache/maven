package org.apache.maven.project.canonical;

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

import org.apache.maven.MavenTestCase;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class CanonicalProjectBuilderTest
    extends MavenTestCase
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

        List plugins = project.getPlugins();

        // Plugin0 [plexus]

        Plugin plugin = (Plugin) plugins.get( 0 );

        assertEquals( "maven", plugin.getGroupId() );

        assertEquals( "maven-plexus-plugin", plugin.getArtifactId() );

        assertEquals( "1.0", plugin.getVersion() );

        Properties properties = plugin.getConfiguration();

        assertEquals( "src/conf/plexus.conf", properties.getProperty( "plexusConfiguration" ) );

        assertEquals( "src/conf/plexus.properties", properties.getProperty( "plexusConfigurationPropertiesFile" ) );

        assertEquals( "Continuum", properties.getProperty( "plexusApplicationName" ) );

        // ----------------------------------------------------------------------
        // Goal specific configuration
        // ----------------------------------------------------------------------

        List goals = plugin.getGoals();

        Goal g0 = (Goal) goals.get( 0 );

        assertEquals( "plexus:runtime", g0.getId() );

        Properties goalProperties = g0.getConfiguration();

        assertEquals( "ContinuumPro", goalProperties.getProperty( "plexusApplicationName" ) );

        // Plugin1 [antlr]
    }
}
