package org.apache.maven.plugin.resources;

import org.apache.maven.plugin.PluginExecutionException;

import java.util.List;

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
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal testResources
 * @description copy test resources
 */
public class TestResourcesMojo
    extends ResourcesMojo
{
    /**
     * The output directory into which to copy the resources.
     * 
     * @parameter name="outputDirectory"
     * required="true"
     * expression="${project.build.testOutputDirectory}"
     */
    private String outputDirectory;

    /**
     * The list of resources we want to transfer.
     * 
     * @parameter name="resources"
     * required="true"
     * expression="${project.build.testResources}"
     */
    private List resources;

    public void execute()
        throws PluginExecutionException
    {
        copyResources( resources, outputDirectory );
    }

}
