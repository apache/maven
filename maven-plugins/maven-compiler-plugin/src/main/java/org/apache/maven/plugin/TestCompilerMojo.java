package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @goal test:compile
 *
 * @description Compiles test sources
 *
 * @parameter
 *  name="sourceDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.unitTestSourceDirectory"
 *  description=""
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.testOutput"
 *  description=""
 * @parameter
 *  name="classpathElements"
 *  type="String[]"
 *  required="true"
 *  validator=""
 *  expression="#project.classpathElements"
 *  description=""
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo use compile source roots and not the pom.build.sourceDirectory so that any
 *       sort of preprocessing and/or source generation can be taken into consideration.
 */
 public class TestCompilerMojo
    extends CompilerMojo
{
}
