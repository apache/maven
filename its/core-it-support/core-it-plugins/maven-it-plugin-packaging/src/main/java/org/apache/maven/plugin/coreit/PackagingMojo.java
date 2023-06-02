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
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Creates an empty file to prove this goal was executed.
 *
 * @author <a href="brett@apache.org">Brett Porter</a>
 *
 */
@Mojo(name = "package")
public class PackagingMojo extends AbstractMojo {

    /**
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    /**
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File outputDirectory;

    public void execute() throws MojoExecutionException {
        File jarFile = new File(outputDirectory, finalName + "-it.jar");

        getLog().info("[MAVEN-CORE-IT-LOG] Creating artifact file: " + jarFile);

        try {
            jarFile.getParentFile().mkdirs();
            jarFile.createNewFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }

        /*
         * NOTE: Normal packaging plugins would set the main artifact's file path now but that's beyond the purpose of
         * this test plugin. Hence there's no need to introduce further coupling with the Maven Artifact API.
         */
    }
}
