package org.apache.maven.tools.plugin.pluggy;

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
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.java.JavaMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.generator.BeanGenerator;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.generator.PluginDescriptorGenerator;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;
import org.apache.maven.tools.plugin.generator.jelly.JellyHarnessGenerator;
import org.apache.maven.tools.plugin.scanner.DefaultMojoScanner;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.apache.maven.tools.plugin.util.PluginUtils;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class Main
{
    public static void main( String[] args )
        throws Exception
    {
        if ( args.length != 5 )
        {
            System.err.println( "Usage: pluggy <mode> <source directory> <output directory> <pom>" );

            System.exit( 1 );
        }

        // Make sense of the args.
        String mode = args[0];

        String sourceDirectory = args[1];

        String outputDirectory = args[2];

        String pom = args[3];

        // Massage the local-repo path into an ArtifactRepository.


        MavenXpp3Reader modelReader = new MavenXpp3Reader();
        FileReader reader = new FileReader( pom );

        Model model = modelReader.read( reader );

        // Not doing inheritence, except for groupId and version
        if ( model.getGroupId() == null )
        {
            model.setGroupId( model.getParent().getGroupId() );
        }
        if ( model.getVersion() == null )
        {
            model.setVersion( model.getParent().getVersion() );
        }

        MavenProject project = new MavenProject( model );
        project.setFile( new File( pom ) );
        project.addCompileSourceRoot( sourceDirectory );

        // Lookup the mojo scanner instance, and use it to scan for mojo's, and
        // extract their descriptors.
        MojoScanner scanner = new DefaultMojoScanner(
            Collections.singletonMap( "java", new JavaMojoDescriptorExtractor() ) );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        
        pluginDescriptor.setGroupId(project.getGroupId());
        
        pluginDescriptor.setArtifactId(project.getArtifactId());
        
        // TODO: should read this from the pom...
        pluginDescriptor.setGoalPrefix( PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() ) );
        
        pluginDescriptor.setDependencies( PluginUtils.toComponentDependencies( project.getDependencies() ) );
        
        scanner.populatePluginDescriptor( project, pluginDescriptor );
        
        // Create the generator.
        Generator generator = null;

        if ( mode.equals( "descriptor" ) )
        {
            generator = new PluginDescriptorGenerator();
        }
        else if ( mode.equals( "xdoc" ) )
        {
            generator = new PluginXdocGenerator();
        }
        else if ( mode.equals( "jelly" ) )
        {
            generator = new JellyHarnessGenerator();
        }
        else if ( mode.equals( "bean" ) )
        {
            generator = new BeanGenerator();
        }

        // Use the generator to process the discovered descriptors and produce
        // something with them.
        generator.execute( outputDirectory, pluginDescriptor );
    }
}
