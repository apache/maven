package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.generator.PluginDescriptorGenerator;

/**
 * @maven.plugin.id plugin
 * @maven.plugin.description A maven2 mojo for generating a plugin descriptor.
 *
 * @parameter sourceDirectory String true validator description
 * @parameter outputDirectory String true validator description
 * @parameter pom String true validator description
 *
 * @goal.name descriptor
 * @goal.descriptor.description Goal for generating a plugin descriptor.
 * @goal.descriptor.parameter sourceDirectory #project.build.sourceDirectory
 * @goal.descriptor.parameter outputDirectory #maven.build.dest/META-INF/maven
 * @goal.descriptor.parameter pom #project.getFile().getPath()

 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DescriptorGenerator
    extends AbstractPluginMojo
{
    protected void generate( String sourceDirectory, String outputDirectory, String pom )
        throws Exception
    {
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator();

        generator.execute( sourceDirectory, outputDirectory, pom );
    }
}
