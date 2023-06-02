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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Checks the general retrieval of components from active component collections.
 *
 * @author Benjamin Bentmann
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VALIDATE)
public class CheckMojo extends AbstractMojo {

    /**
     * Project base directory used for manual path alignment.
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File basedir;

    /**
     * The available components, as a map.
     */
    @Component
    private Map<String, TestComponent> componentMap;

    /**
     * The available components, as a list.
     */
    @Component
    private List<TestComponent> componentList;

    /**
     * The path to the properties file to create.
     */
    @Parameter(property = "clsldr.pluginClassLoaderOutput")
    private File outputFile;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the output file could not be created.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties componentProperties = new Properties();

        getLog().info("[MAVEN-CORE-IT-LOG] Dumping component info");

        componentProperties.setProperty("count", Integer.toString(componentList.size()));

        if (componentList.size() != componentMap.size()) {
            throw new MojoExecutionException("Inconsistent collection: " + componentList + " vs " + componentMap);
        }

        for (int i = componentList.size() - 1; i >= 0; i--) {
            Object component = componentList.get(i);

            if (component != componentList.get(i)) {
                throw new MojoExecutionException("Invalid re-lookup of component from list: " + i);
            }
        }

        int i = 0;
        for (Iterator it = componentMap.keySet().iterator(); it.hasNext(); i++) {
            String roleHint = (String) it.next();
            componentProperties.setProperty("component." + i + ".hint", roleHint);

            Object component = componentMap.get(roleHint);

            if (component != null) {
                String hash = Integer.toString(System.identityHashCode(component));
                componentProperties.setProperty("component." + i + ".hash", hash);
                componentProperties.setProperty("component." + roleHint + ".hash", hash);
            }

            if (component != componentMap.get(roleHint)) {
                throw new MojoExecutionException("Invalid re-lookup of component from map: " + roleHint);
            }

            getLog().info("[MAVEN-CORE-IT-LOG]   " + roleHint + " = " + component);
        }

        if (!outputFile.isAbsolute()) {
            outputFile = new File(basedir, outputFile.getPath());
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Creating output file " + outputFile);

        OutputStream out = null;
        try {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream(outputFile);
            componentProperties.store(out, "MAVEN-CORE-IT-LOG");
        } catch (IOException e) {
            throw new MojoExecutionException("Output file could not be created: " + outputFile, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // just ignore
                }
            }
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Created output file " + outputFile);
    }
}
