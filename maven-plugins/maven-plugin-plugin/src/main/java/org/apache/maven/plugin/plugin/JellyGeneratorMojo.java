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
import org.apache.maven.tools.plugin.generator.jelly.JellyHarnessGenerator;
import org.apache.maven.tools.plugin.generator.Generator;

import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal jelly
 */
public class JellyGeneratorMojo
    extends AbstractGeneratorMojo
{
    /**
     * @parameter expression="${project.build.directory}/generated-sources"
     * @required
     */
    protected String outputDirectory;

    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    protected Generator createGenerator()
    {
        return new JellyHarnessGenerator();
    }
}
