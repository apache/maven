package org.apache.maven.plugin.plugin;

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

import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.generator.PluginDescriptorGenerator;

import java.util.Set;

/**
 * Generate a plugin descriptor.
 * <p/>
 * Note: Phase is after the "compilation" of any scripts
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal descriptor
 * @phase process-classes
 * @description Goal for generating a plugin descriptor.
 * @parameter name="mojoScanner"
 * type="org.apache.maven.tools.plugin.scanner.MojoScanner"
 * required="true"
 * validator=""
 * expression="${component.org.apache.maven.tools.plugin.scanner.MojoScanner}"
 * description="Scanner used to discover mojo descriptors from this project"
 * @parameter name="project"
 * type="org.apache.maven.project.MavenProject"
 * required="true"
 * validator=""
 * expression="${project}"
 * description=""
 * @parameter name="outputDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="${project.build.outputDirectory}/META-INF/maven"
 * description=""
 */
public class DescriptorGeneratorMojo
    extends AbstractGeneratorMojo
{
    protected void generate( String outputDirectory, Set mavenMojoDescriptors, MavenProject project )
        throws Exception
    {
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator();

        generator.execute( outputDirectory, mavenMojoDescriptors, project );

    }
}
