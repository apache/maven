package org.apache.maven.tools.plugin.generator;

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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.component.repository.ComponentDependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: PluginDescriptorGeneratorTest.java,v 1.1.1.1 2004/08/09
 *          18:43:10 jvanzyl Exp $
 */
public class PluginDescriptorGeneratorTest
    extends AbstractGeneratorTestCase
{
    protected void validate(File destinationDirectory)
        throws Exception
    {
        PluginDescriptorBuilder pdb = new PluginDescriptorBuilder();

        File pluginDescriptorFile = new File( destinationDirectory, "plugin.xml" );

        String pd = readFile( pluginDescriptorFile );

        PluginDescriptor pluginDescriptor = pdb.build( new StringReader( pd ) );

        assertEquals( 1, pluginDescriptor.getMojos().size() );
        
        MojoDescriptor mojoDescriptor = (MojoDescriptor) pluginDescriptor.getMojos().get( 0 );

        checkMojo( mojoDescriptor );

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        List dependencies = pluginDescriptor.getDependencies();

        checkDependency( "testGroup", "testArtifact", "0.0.0", (ComponentDependency) dependencies.get( 0 ) );

        assertEquals( 1, dependencies.size() );

        ComponentDependency dependency = (ComponentDependency) dependencies.get( 0 );
        assertEquals( "testGroup", dependency.getGroupId() );
        assertEquals( "testArtifact", dependency.getArtifactId() );
        assertEquals( "0.0.0", dependency.getVersion() );
    }

    private String readFile( File pluginDescriptorFile )
        throws IOException
    {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        BufferedReader reader = new BufferedReader( new FileReader( pluginDescriptorFile ) );

        String line = null;
        while ( ( line = reader.readLine() ) != null )
        {
            pWriter.println( line );
        }

        reader.close();

        return sWriter.toString();
    }

    private void checkMojo( MojoDescriptor mojoDescriptor )
    {
        assertEquals( "test:testGoal", mojoDescriptor.getFullGoalName() );

        assertEquals( "org.apache.maven.tools.plugin.generator.TestMojo", mojoDescriptor.getImplementation() );

        // The following should be defaults
        assertEquals( "per-lookup", mojoDescriptor.getInstantiationStrategy() );

        assertNotNull( mojoDescriptor.isDependencyResolutionRequired() );

        // check the parameter.
        checkParameter( (Parameter) mojoDescriptor.getParameters().get( 0 ) );
    }

    private void checkParameter( Parameter parameter )
    {
        assertEquals( "dir", parameter.getName() );
        assertEquals( String.class.getName(), parameter.getType() );
        assertTrue( parameter.isRequired() );
    }

    private void checkDependency( String groupId, String artifactId, String version, ComponentDependency dependency )
    {
        assertNotNull( dependency );

        assertEquals( groupId, dependency.getGroupId() );

        assertEquals( artifactId, dependency.getArtifactId() );

        assertEquals( version, dependency.getVersion() );
    }
}