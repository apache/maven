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

import java.util.List;
import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal testCompile
 * @phase test-compile
 * @description Compiles test sources
 * @requiresDependencyResolution test
 */
public class TestCompilerMojo
    extends AbstractCompilerMojo
{
    /**
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    private List compileSourceRoots;

    /**
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    protected List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    protected List getClasspathElements()
    {
        return classpathElements;
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }

}
