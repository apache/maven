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
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo which touches a file without requiring a project.
 */
@Mojo(name = "light-touch", requiresProject = false)
public class NoProjectMojo extends AbstractMojo {
    /**
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private String outputDirectory;

    /**
     */
    @Parameter(defaultValue = "target/test-basedir-alignment")
    private File basedirAlignmentDirectory;

    public void execute() throws MojoExecutionException {
        touch(new File(outputDirectory), "touch.txt");
    }

    private void touch(File dir, String file) throws MojoExecutionException {
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File touch = new File(dir, file);

            getLog().info("Touching: " + touch);

            FileWriter w = new FileWriter(touch);

            w.write(file);

            w.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Error touching file", e);
        }
    }
}
