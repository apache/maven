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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * Finds a tool from a previously selected toolchain. This tests the public API just like toolchain-enabled plugins
 * would do.
 *
 */
@Mojo(name = "find-tool", defaultPhase = LifecyclePhase.VALIDATE)
public class FindToolMojo extends AbstractMojo {

    /**
     */
    @Component
    private ToolchainManager toolchainManager;

    /**
     * The current Maven session holding the selected toolchain.
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    /**
     * The path to the output file for the properties.
     */
    @Parameter(property = "toolchain.outputFile", defaultValue = "${project.build.directory}/tool.properties")
    private File outputFile;

    /**
     * The type identifier of the toolchain, e.g. "jdk".
     */
    @Parameter(property = "toolchain.type")
    private String type;

    /**
     * The name of the tool, e.g. "javac".
     */
    @Parameter(property = "toolchain.tool")
    private String tool;

    public void execute() throws MojoExecutionException {
        Toolchain toolchain = toolchainManager.getToolchainFromBuildContext(type, session);

        getLog().info("[MAVEN-CORE-IT-LOG] Toolchain in session: " + toolchain);

        Properties properties = new Properties();

        if (toolchain != null) {
            properties.setProperty("toolchain.type", toolchain.getType());

            String path = toolchain.findTool(tool);
            if (path != null) {
                properties.setProperty("tool." + tool, path);
            }
        }

        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream(outputFile);
            properties.store(out, "MAVEN-CORE-IT-LOG");
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
