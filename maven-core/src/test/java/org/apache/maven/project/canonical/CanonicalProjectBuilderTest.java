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

import junit.framework.TestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Site;
import org.apache.maven.model.UnitTest;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.MavenTestCase;

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
        File f  = new File( basedir, "src/test/resources/canonical-pom.xml" );

        MavenProject project = projectBuilder.build( f );

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

        assertEquals( "plexus", plugin.getId() );

        assertEquals( "1.0", plugin.getVersion() );

        Properties properties = plugin.getConfiguration();

        assertEquals( "src/conf/plexus.conf", properties.getProperty( "plexusConfiguration" ) );

        assertEquals( "src/conf/plexus.properties", properties.getProperty( "plexusConfigurationPropertiesFile" ) );

        assertEquals( "Continuum", properties.getProperty( "plexusApplicationName" ) );

        // Plugin1 [antlr]
    }
}
