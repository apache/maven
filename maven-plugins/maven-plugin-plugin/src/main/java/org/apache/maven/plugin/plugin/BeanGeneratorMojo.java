package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.generator.BeanGenerator;

/**
 * @goal bean
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
 *  expression="#project.build.directory/generated-sources"
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
public class BeanGeneratorMojo
    extends AbstractGeneratorMojo
{
    protected void generate( String sourceDirectory, String outputDirectory, String pom )
        throws Exception
    {
        BeanGenerator generator = new BeanGenerator();

        generator.execute( sourceDirectory, outputDirectory, pom );
    }
}
