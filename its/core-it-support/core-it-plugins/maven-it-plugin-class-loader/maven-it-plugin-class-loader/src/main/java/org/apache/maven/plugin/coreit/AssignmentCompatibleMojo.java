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
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Checks whether API classes exported by the Maven core are assignment-compatible with types loaded from the plugin
 * class loader. In other words, checks that types shared with the core realm are imported into the plugin realm.
 *
 * @author Benjamin Bentmann
 */
@org.apache.maven.plugins.annotations.Mojo(name = "assignment-compatible", defaultPhase = LifecyclePhase.INITIALIZE)
public class AssignmentCompatibleMojo extends AbstractMojo {

    /**
     * The path to the properties file used to track the results of the assignment compatibility tests.
     */
    @Parameter(property = "clsldr.assigncompatPropertiesFile")
    private File assigncompatPropertiesFile;

    /**
     * The qualified names of the types to check.
     *
     */
    @Parameter
    private String[] classNames;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute() throws MojoExecutionException {
        ClassLoader coreRealm = Mojo.class.getClassLoader();
        ClassLoader pluginRealm = getClass().getClassLoader();

        if (coreRealm == pluginRealm) {
            throw new MojoExecutionException("Unexpected class loader hierarchy, could not detect core realm");
        }

        Properties properties = new Properties();

        if (classNames != null) {
            for (String className : classNames) {
                String result;

                getLog().info("[MAVEN-CORE-IT-LOG] Loading class " + className);

                try {
                    Class type = pluginRealm.loadClass(className);
                    result = getKey(type);
                } catch (ClassNotFoundException e) {
                    result = "";
                }
                properties.setProperty("plugin." + className, result);
                getLog().info("[MAVEN-CORE-IT-LOG]   plugin: " + result);

                try {
                    Class type = coreRealm.loadClass(className);
                    result = getKey(type);
                } catch (ClassNotFoundException e) {
                    result = "";
                }
                properties.setProperty("core." + className, result);
                getLog().info("[MAVEN-CORE-IT-LOG]   core  : " + result);
            }
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Creating output file " + assigncompatPropertiesFile);

        PropertiesUtil.write(assigncompatPropertiesFile, properties);

        getLog().info("[MAVEN-CORE-IT-LOG] Created output file " + assigncompatPropertiesFile);
    }

    private String getKey(Class type) {
        return type.hashCode() + "@" + type.getClassLoader().hashCode();
    }
}
