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
package org.apache.maven.plugin.coreit;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 */
@Mojo(name = "fork")
@Execute(phase = LifecyclePhase.GENERATE_RESOURCES, lifecycle = "foo")
public class ForkLifecycleMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(defaultValue = "${executedProject}")
    private MavenProject executedProject;

    @Parameter(defaultValue = "${project.build.directory}")
    private File touchDirectory;

    public void execute() throws MojoExecutionException {
        TouchMojo.touch(touchDirectory, "fork-lifecycle.txt");

        if (!executedProject.getBuild().getFinalName().equals(TouchMojo.FINAL_NAME)) {
            throw new MojoExecutionException("Unexpected result, final name of executed project is "
                    + executedProject.getBuild().getFinalName() + " (should be \'" + TouchMojo.FINAL_NAME + "\').");
        }

        if (project.getBuild().getFinalName().equals(TouchMojo.FINAL_NAME)) {
            throw new MojoExecutionException(
                    "forked project was polluted. (should NOT be \'" + TouchMojo.FINAL_NAME + "\').");
        }
    }
}
