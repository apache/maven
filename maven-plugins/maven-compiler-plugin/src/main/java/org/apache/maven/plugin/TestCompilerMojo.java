package org.apache.maven.plugin;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal testCompile
 * @description Compiles test sources
 * @requiresDependencyResolution
 * @parameter name="compileSourceRoots"
 * type="java.util.List"
 * required="true"
 * validator=""
 * expression="#project.testCompileSourceRoots"
 * description=""
 * @parameter name="outputDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.testOutput"
 * description=""
 * @parameter name="classpathElements"
 * type="List"
 * required="true"
 * validator=""
 * expression="#project.testClasspathElements"
 * description=""
 * @parameter name="debug"
 * type="String"
 * required="false"
 * validator=""
 * expression="#maven.compiler.debug"
 * description="Whether to include debugging information in the compiled class files; the default value is false"
 */
public class TestCompilerMojo extends CompilerMojo
{
}
