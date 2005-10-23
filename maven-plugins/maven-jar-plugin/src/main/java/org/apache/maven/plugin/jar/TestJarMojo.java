package org.apache.maven.plugin.jar;

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

import java.io.File;

/**
 * Build a JAR of the test classes for the current project.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal test-jar
 * @phase package
 * @requiresProject
 */
public class TestJarMojo
    extends AbstractJarMojo
{
    /**
     * Directory containing the test classes.
     *
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File testOutputDirectory;

    protected String getClassifier()
    {
        return "tests";
    }

    /**
     * Return the test-classes directory, to serve as the root of the tests jar.
     */
    protected File getOutputDirectory()
    {
        return testOutputDirectory;
    }
}
