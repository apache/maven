package org.apache.maven.tools.plugin.extractor.java;

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

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * @author jdcasey
 */
public class JavaMojoDescriptorExtractorTest
    extends TestCase
{

    public void testShouldFindTwoMojoDescriptorsInTestSourceDirectory()
        throws Exception
    {
        JavaMojoDescriptorExtractor extractor = new JavaMojoDescriptorExtractor();

        File sourceFile = fileOf( "dir-flag.txt" );
        System.out.println( "found source file: " + sourceFile );

        File dir = sourceFile.getParentFile();

        Model model = new Model();
        model.setArtifactId( "maven-unitTesting-plugin" );

        MavenProject project = new MavenProject( model );

        project.setFile( new File( dir, "pom.xml" ) );
        project.addCompileSourceRoot( new File( dir, "source" ).getPath() );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "test" );
        List results = extractor.execute( project, pluginDescriptor );
        assertEquals( 2, results.size() );
    }

    private File fileOf( String classpathResource )
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource( classpathResource );

        File result = null;
        if ( resource != null )
        {
            result = new File( resource.getPath() );
        }

        return result;
    }

}