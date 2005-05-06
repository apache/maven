package org.apache.maven.tools.plugin.extractor.marmalade;

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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * @author jdcasey
 */
public class MarmaladeMojoDescriptorExtractorTest
    extends PlexusTestCase
{

    public void testShouldFindOneMojo()
        throws Exception
    {
        File basedir = dirname( "testMojo.mmld" );

        Model model = new Model();
        model.setArtifactId( "testArtifactId" );

        MavenProject project = new MavenProject( model );

        project.setFile( new File( basedir, "pom.xml" ) );

        System.out.println( "Basedir: " + basedir );
        project.addScriptSourceRoot( basedir.getPath() );

        MarmaladeMojoDescriptorExtractor extractor = (MarmaladeMojoDescriptorExtractor) lookup(
            MojoDescriptorExtractor.ROLE, "marmalade" );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "test" );
        List descriptors = extractor.execute( project, pluginDescriptor );

        assertEquals( 1, descriptors.size() );

        MojoDescriptor descriptor = (MojoDescriptor) descriptors.iterator().next();
        assertEquals( pluginDescriptor, descriptor.getPluginDescriptor() );
        assertEquals( "marmalade", descriptor.getLanguage() );
        assertEquals( "testGoal", descriptor.getGoal() );
        assertEquals( 1, descriptor.getParameters().size() );
    }

    //    public void testShouldFindMojoDescriptorViaContainerDrivenExtractor()
    // throws Exception
    //    {
    //        Embedder embedder = new Embedder();
    //        embedder.start(new ClassWorld("test",
    // Thread.currentThread().getContextClassLoader()));
    //        
    //        MojoScanner scanner = (MojoScanner) embedder.lookup(MojoScanner.ROLE);
    //        
    //        Build build = new Build();
    //        build.setSourceDirectory(".");
    //        
    //        Model model = new Model();
    //        model.setBuild(build);
    //        
    //        MavenProject project = new MavenProject(model);
    //        project.setFile(new File(dirname("testMojo.mmld"), "pom.xml"));
    //        
    //        Set descriptors = scanner.execute(project);
    //        assertEquals(1, descriptors.size());
    //        
    //        MavenMojoDescriptor mmDesc =
    // (MavenMojoDescriptor)descriptors.iterator().next();
    //        assertEquals("marmalade", mmDesc.getComponentFactory());
    //        assertEquals("testId", mmDesc.getMojoDescriptor().getId());
    //        assertEquals("testGoal", mmDesc.getMojoDescriptor().getGoal());
    //        assertEquals(1, mmDesc.getMojoDescriptor().getParameters().size());
    //    }
    //    
    private File dirname( String resourceName )
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL resource = cloader.getResource( resourceName );

        File dir = null;
        if ( resource != null )
        {
            File file = new File( resource.getPath() );
            dir = file.getParentFile();
        }

        return dir;
    }

}