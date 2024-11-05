/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package plugin;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Maven Mojo for executing nunit tests
 *
 * @goal validate
 * @phase validate
 */
public class Mojo3563 extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Model model = project.getModel();
        String property = model.getProperties().getProperty("test");
        if (property == null) {
            throw new MojoExecutionException("Could not find property.");
        }

        File testFile = new File(property.substring(property.indexOf(":") + 1));
        if (!testFile.exists()) {
            throw new MojoExecutionException(
                    "Test file does not exist: File = " + testFile.getAbsolutePath() + ", Property = " + property);
        }
        getLog().info("Property = " + property);
    }
}
