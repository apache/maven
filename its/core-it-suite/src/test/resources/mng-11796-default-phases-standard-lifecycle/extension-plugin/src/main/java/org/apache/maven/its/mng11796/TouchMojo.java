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
package org.apache.maven.its.mng11796;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Creates a marker file to prove the goal was executed.
 * @goal touch
 */
public class TouchMojo extends AbstractMojo {
    /**
     * @parameter default-value="${project.build.directory}"
     */
    private File outputDirectory;

    public void execute() throws MojoExecutionException {
        getLog().info("MNG-11796 touch goal executed");
        File touchFile = new File(outputDirectory, "touch.txt");
        touchFile.getParentFile().mkdirs();
        try {
            touchFile.createNewFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create touch file", e);
        }
    }
}
