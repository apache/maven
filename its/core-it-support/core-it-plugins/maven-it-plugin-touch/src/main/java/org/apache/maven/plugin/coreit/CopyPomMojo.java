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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo which makes a copy of the POM using MavenProject.getFile() to locate the file.
 *
 */
@Mojo(name = "copy-pom", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CopyPomMojo extends AbstractMojo {
    /**
     */
    @Parameter(defaultValue = "${project.file}")
    private File pomFile;

    /**
     */
    @Parameter(defaultValue = "${project.build.directory}/pom-copy.xml", required = true)
    private String outputFile;

    public void execute() throws MojoExecutionException {
        try {
            File dest = new File(outputFile);
            File dir = dest.getParentFile();

            if (!dir.exists()) {
                dir.mkdirs();
            }

            getLog().info("Copying POM to file: " + dest.getAbsolutePath());

            FileInputStream in = new FileInputStream(pomFile);
            FileOutputStream out = new FileOutputStream(dest);

            int read = -1;
            byte[] buf = new byte[4 * 1024];
            while ((read = in.read(buf)) > -1) {
                out.write(buf, 0, read);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying POM", e);
        }
    }
}
