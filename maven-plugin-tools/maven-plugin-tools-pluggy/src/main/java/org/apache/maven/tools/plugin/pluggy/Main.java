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
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.java.JavaMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.generator.BeanGenerator;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.generator.PluginDescriptorGenerator;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;
import org.apache.maven.tools.plugin.generator.jelly.JellyHarnessGenerator;
import org.apache.maven.tools.plugin.scanner.DefaultMojoScanner;
import org.apache.maven.tools.plugin.scanner.MojoScanner;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

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
            System.err.println( "Usage: pluggy <mode> <source directory> <output directory> <pom> <local-repo>" );

            System.exit( 1 );
        }

        // Make sense of the args.
        String mode = args[0];

        String sourceDirectory = args[1];

        String outputDirectory = args[2];

        String pom = args[3];
        
        String localRepo = args[4];
        
        // Massage the local-repo path into an ArtifactRepository.
        File repoPath = new File(localRepo);
        
        URL repoUrl = repoPath.toURL();
        
        MavenXpp3Reader modelReader = new MavenXpp3Reader();
        FileReader reader = new FileReader(pom);
        
        Model model = modelReader.read(reader);
        
        MavenProject project = new MavenProject(model);
        project.setFile(new File(pom));
        
        // Lookup the mojo scanner instance, and use it to scan for mojo's, and
        // extract their descriptors.
        MojoScanner scanner = new DefaultMojoScanner(Collections.singletonMap("java", new JavaMojoDescriptorExtractor()));
        
        Set descriptors = scanner.execute(project);
        
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
        generator.execute( outputDirectory, descriptors, project );
    }
}
