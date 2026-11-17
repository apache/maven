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
package org.apache.maven.its.mng13763;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

@Mojo(name = "aggregate", aggregator = true, defaultPhase = LifecyclePhase.PACKAGE)
public class AggregateMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(property = "expectChildAggregator", defaultValue = "false")
    private boolean expectChildAggregator;

    @Parameter(property = "requireOwnTouch", defaultValue = "false")
    private boolean requireOwnTouch;

    @Parameter
    private String[] expectedAggregates;

    @Parameter(defaultValue = "false")
    private boolean attachArtifact;

    @Parameter(defaultValue = "aggregate-observed.txt")
    private String markerName;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException {
        Path root = basedir.toPath();
        Path output = root.resolve("target").resolve(markerName);
        boolean a = Files.exists(root.resolve("a/target/touched.txt"));
        boolean b = Files.exists(root.resolve("b/target/touched.txt"));
        boolean aa = Files.exists(root.resolve(
                root.getFileName().toString().equals("a") ? "aa/target/touched.txt" : "a/aa/target/touched.txt"));
        if (requireOwnTouch && !Files.exists(root.resolve("target/touched.txt"))) {
            throw new MojoExecutionException("The module's package goal has not completed");
        }
        if (expectChildAggregator
                && root.getFileName().toString().equals("reactor")
                && !Files.exists(root.resolve("a/target/aggregate-observed.txt"))) {
            throw new MojoExecutionException("Child aggregator has not completed");
        }
        try {
            if (expectedAggregates != null) {
                for (String expected : expectedAggregates) {
                    if (!Files.exists(root.resolve(expected))) {
                        throw new MojoExecutionException("Missing subproject aggregate: " + expected);
                    }
                }
            }
            Files.createDirectories(output.getParent());
            Files.write(output, ("a=" + a + " b=" + b + " aa=" + aa).getBytes(StandardCharsets.UTF_8));
            if (attachArtifact) {
                File artifact = root.resolve("target/aggregate.jar").toFile();
                try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(artifact.toPath()))) {
                    jar.finish();
                }
                projectHelper.attachArtifact(project, "jar", "aggregate", artifact);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write " + output, e);
        }
    }
}
