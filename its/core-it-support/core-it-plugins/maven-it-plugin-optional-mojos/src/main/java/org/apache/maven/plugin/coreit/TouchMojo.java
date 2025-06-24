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
 */
@Mojo(name = "touch")
public class TouchMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/touch.txt")
    private File touchFile;

    public void execute() throws MojoExecutionException {
        getLog().info("[MAVEN-CORE-IT-LOG] Creating touch file: " + touchFile);

        try {
            touchFile.getParentFile().mkdirs();
            touchFile.createNewFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating touch file: " + e.getMessage(), e);
        }
    }
}
