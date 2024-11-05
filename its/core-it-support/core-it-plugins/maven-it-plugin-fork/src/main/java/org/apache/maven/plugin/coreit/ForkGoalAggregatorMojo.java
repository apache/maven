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

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 */
@Mojo(name = "fork-goal-aggregator", aggregator = true)
@Execute(goal = "touch")
public class ForkGoalAggregatorMojo extends AbstractMojo {
    /**
     */
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     */
    @Parameter(defaultValue = "${reactorProjects}")
    private List<MavenProject> reactorProjects;

    public void execute() throws MojoExecutionException {
        for (MavenProject reactorProject : reactorProjects) {
            MavenProject executedProject = reactorProject.getExecutionProject();

            if (!executedProject.getBuild().getFinalName().equals(TouchMojo.FINAL_NAME)) {
                throw new MojoExecutionException(
                        "Unexpected result, final name of executed project " + executedProject + " is "
                                + executedProject.getBuild().getFinalName() + " (should be \'" + TouchMojo.FINAL_NAME
                                + "\').");
            }
        }

        if (project.getBuild().getFinalName().equals(TouchMojo.FINAL_NAME)) {
            throw new MojoExecutionException(
                    "forked project was polluted. (should NOT be \'" + TouchMojo.FINAL_NAME + "\').");
        }
    }
}
