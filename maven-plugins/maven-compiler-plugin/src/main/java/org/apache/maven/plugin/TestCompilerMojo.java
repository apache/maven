package org.apache.maven.plugin;



/**
 * @goal testCompile
 *
 * @description Compiles test sources
 *
 * @requiresDependencyResolution
 *
 * @parameter
 *  name="compileSourceRoots"
 *  type="java.util.List"
 *  required="true"
 *  validator=""
 *  expression="#project.testCompileSourceRoots"
 *  description=""
 *
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.testOutput"
 *  description=""
 * @parameter
 *  name="classpathElements"
 *  type="List"
 *  required="true"
 *  validator=""
 *  expression="#project.testClasspathElements"
 *  description=""
 * @parameter
 *  name="debug"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.compiler.debug"
 *  description="Whether to include debugging information in the compiled class files; the default value is false"
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
 public class TestCompilerMojo
    extends CompilerMojo
{
}
