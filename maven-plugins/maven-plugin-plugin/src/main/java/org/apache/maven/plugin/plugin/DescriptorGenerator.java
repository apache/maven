package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.generator.PluginDescriptorGenerator;

/**
 * @goal descriptor
 *
 * @description Goal for generating a plugin descriptor.
 *
 * @parameter
 *  name="sourceDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.sourceDirectory"
 *  description=""
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true" 
 *  validator="" 
 *  expression="#project.build.directory/classes/META-INF/maven"
 *  description=""
 * @parameter
 *  name="pom"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.file.path"
 *  description=""
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DescriptorGenerator
    extends AbstractGeneratorMojo
{
    protected void generate( String sourceDirectory, String outputDirectory, String pom )
        throws Exception
    {
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator();

        generator.execute( sourceDirectory, outputDirectory, pom );
    }
}
