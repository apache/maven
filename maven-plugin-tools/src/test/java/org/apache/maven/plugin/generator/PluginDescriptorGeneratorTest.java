package org.apache.maven.plugin.generator;

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
import java.io.FileReader;
import java.util.List;

import org.apache.maven.plugin.descriptor.Dependency;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginDescriptorGeneratorTest
    extends AbstractGeneratorTestCase
{
    protected void validate()
        throws Exception
    {
        PluginDescriptorBuilder pdb = new PluginDescriptorBuilder();

        File pluginDescriptorFile = new File( basedir, "target/plugin.xml" );

        PluginDescriptor pluginDescriptor = pdb.build( new FileReader( pluginDescriptorFile ) );

        MojoDescriptor mojoDescriptor = (MojoDescriptor) pluginDescriptor.getMojos().get( 0 );

        if ( mojoDescriptor.getId().equals( "idea:ideaOne" ) )
        {
            checkMojoOne( mojoDescriptor );

            mojoDescriptor = (MojoDescriptor) pluginDescriptor.getMojos().get( 1 );

            checkMojoTwo( mojoDescriptor );
        }
        else
        {
            checkMojoTwo( mojoDescriptor );

            mojoDescriptor = (MojoDescriptor) pluginDescriptor.getMojos().get( 1 );

            checkMojoOne( mojoDescriptor );
        }

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        List parameters = mojoDescriptor.getParameters();

        assertEquals( 1, parameters.size() );

        Parameter pd = (Parameter) mojoDescriptor.getParameters().get( 0 );

        assertEquals( "project", pd.getName() );

        assertEquals( "#project", pd.getExpression() );

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        List dependencies = pluginDescriptor.getDependencies();

        checkDependency( "maven", "maven-core", "2.0-SNAPSHOT", (Dependency) dependencies.get( 0 ) );

        assertEquals( 3, dependencies.size() );
    }

    private void checkMojoOne( MojoDescriptor mojoDescriptor )
    {
        assertEquals( "idea:ideaOne", mojoDescriptor.getId() );

        assertEquals( "org.apache.maven.plugin.idea.IdeaMojoOne", mojoDescriptor.getImplementation() );

        assertEquals( "singleton", mojoDescriptor.getInstantiationStrategy() );

        assertTrue( mojoDescriptor.requiresDependencyResolution() );
    }

    private void checkMojoTwo( MojoDescriptor mojoDescriptor )
    {
        assertEquals( "idea:ideaTwo", mojoDescriptor.getId() );

        assertEquals( "org.apache.maven.plugin.idea.IdeaMojoTwo", mojoDescriptor.getImplementation() );

        assertEquals( "singleton", mojoDescriptor.getInstantiationStrategy() );

        assertFalse( mojoDescriptor.requiresDependencyResolution() );
    }

    private void checkDependency( String groupId, String artifactId, String version, Dependency dependency )
    {
        assertNotNull( dependency );

        assertEquals( groupId, dependency.getGroupId() );

        assertEquals( artifactId, dependency.getArtifactId() );

        assertEquals( version, dependency.getVersion() );
    }
}
