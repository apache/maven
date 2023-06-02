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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;

/**
 */
@Mojo(name = "toolchain", defaultPhase = LifecyclePhase.VALIDATE)
public class CoreItMojo extends AbstractMojo {

    /**
     */
    @Component
    private ToolchainManagerPrivate toolchainManager;

    /**
     * The current Maven session holding the selected toolchain.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The path to the output file for the properties.
     */
    @Parameter(property = "toolchain.outputFile", defaultValue = "${project.build.directory}/toolchains.properties")
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

    /**
     * The zero-based index of the toolchain to select and store in the build context.
     */
    @Parameter(property = "toolchain.selected")
    private int selected;

    public void execute() throws MojoExecutionException {
        ToolchainPrivate[] tcs = getToolchains();

        getLog().info("[MAVEN-CORE-IT-LOG] Toolchains in plugin: " + Arrays.asList(tcs));

        if (selected >= 0) {
            if (selected < tcs.length) {
                ToolchainPrivate toolchain = tcs[selected];
                toolchainManager.storeToolchainToBuildContext(toolchain, session);
            } else {
                getLog().warn("[MAVEN-CORE-IT-LOG] Toolchain #" + selected + " can't be selected, found only "
                        + tcs.length);
            }
        }

        Properties properties = new Properties();

        int count = 1;
        for (Iterator<ToolchainPrivate> i = Arrays.<ToolchainPrivate>asList(tcs).iterator(); i.hasNext(); count++) {
            ToolchainPrivate toolchain = i.next();

            String foundTool = toolchain.findTool(tool);
            if (foundTool != null) {
                properties.setProperty("tool." + count, foundTool);
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

    private ToolchainPrivate[] getToolchains() throws MojoExecutionException {
        Class<? extends ToolchainManagerPrivate> managerClass = toolchainManager.getClass();

        try {
            try {
                // try 2.x style API
                Method oldMethod = managerClass.getMethod("getToolchainsForType", new Class[] {String.class});

                return (ToolchainPrivate[]) oldMethod.invoke(toolchainManager, new Object[] {type});
            } catch (NoSuchMethodException e) {
                // try 3.x style API
                Method newMethod =
                        managerClass.getMethod("getToolchainsForType", new Class[] {String.class, MavenSession.class});

                return (ToolchainPrivate[]) newMethod.invoke(toolchainManager, new Object[] {type, session});
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new MojoExecutionException("Incompatible toolchain API", e);
        }
    }
}
