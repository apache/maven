package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.generator.jelly.JellyHarnessGenerator;

/**
 * @goal jelly
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
 *  expression="#project.build.output
 *  description=""
 * @parameter
 *  name="pom"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.getFile().getPath()"
 *  description=""
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class JellyGeneratorMojo
    extends AbstractPluginMojo
{
    protected void generate( String sourceDirectory, String outputDirectory, String pom )
        throws Exception
    {
        JellyHarnessGenerator generator = new JellyHarnessGenerator();

        generator.execute( sourceDirectory, outputDirectory, pom );
    }
}
