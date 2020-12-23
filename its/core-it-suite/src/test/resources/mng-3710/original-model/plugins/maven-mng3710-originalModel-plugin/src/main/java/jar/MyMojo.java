package jar;

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

import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal check
 */
public class MyMojo
    extends AbstractMojo
{
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        Model originalModel = project.getOriginalModel();
        Build originalBuild = originalModel.getBuild();

        Map originalPluginMap = originalBuild.getPluginsAsMap();

        if ( originalPluginMap.containsKey( Plugin.constructKey( "org.apache.maven.its.mng3710", "maven-mng3710-directInvoke-plugin" ) ) )
        {
            throw new MojoExecutionException( "Project's original model has been polluted by an entry for a plugin that was invoked directly from the command line." );
        }

        if ( originalPluginMap.containsKey( Plugin.constructKey( "org.apache.maven.plugins", "maven-compiler-plugin" ) ) )
        {
            throw new MojoExecutionException( "Project's original model has been polluted by an entry for a plugin that is specified in the lifecycle mapping for this project's packaging." );
        }

        getLog().info( "Original-model verification completed successfully." );
    }
}
