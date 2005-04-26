package org.apache.maven.project.canonical;

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

import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class CanonicalProjectBuilderTest
    extends MavenProjectTestCase
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

        assertEquals( "org.apache.maven.plugins", plugin.getGroupId() );

        assertEquals( "maven-plexus-plugin", plugin.getArtifactId() );

        assertEquals( "1.0", plugin.getVersion() );

        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

        assertEquals( "src/conf/plexus.conf", configuration.getChild( "plexusConfiguration" ).getValue() );

        assertEquals( "src/conf/plexus.properties",
                      configuration.getChild( "plexusConfigurationPropertiesFile" ).getValue() );

        assertEquals( "Continuum", configuration.getChild( "plexusApplicationName" ).getValue() );

        // ----------------------------------------------------------------------
        // Goal specific configuration
        // ----------------------------------------------------------------------

        List goals = plugin.getGoals();

        Goal g0 = (Goal) goals.get( 0 );

        assertEquals( "plexus:runtime", g0.getId() );

        configuration = (Xpp3Dom) g0.getConfiguration();

        assertEquals( "ContinuumPro", configuration.getChild( "plexusApplicationName" ).getValue() );

        // Plugin1 [antlr]
    }
}
