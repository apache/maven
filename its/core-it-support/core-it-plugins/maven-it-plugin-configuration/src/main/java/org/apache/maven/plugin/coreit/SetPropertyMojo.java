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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Sets a property on the current project at runtime. This simulates the behavior
 * of plugins like GMaven/gmavenplus that dynamically set project properties via
 * {@code project.getProperties().setProperty(...)} during the build lifecycle.
 *
 * <p>Properties set this way are NOT available during model interpolation (which
 * runs before the build lifecycle), but ARE available to the expression evaluator
 * at mojo execution time.</p>
 */
@Mojo(name = "set-property", defaultPhase = LifecyclePhase.INITIALIZE)
public class SetPropertyMojo extends AbstractMojo {

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * The property key to set.
     */
    @Parameter(required = true)
    private String propertyName;

    /**
     * The property value to set.
     */
    @Parameter(required = true)
    private String propertyValue;

    public void execute() throws MojoExecutionException {
        getLog().info("[MAVEN-CORE-IT-LOG] Setting project property: " + propertyName + " = " + propertyValue);
        project.getProperties().setProperty(propertyName, propertyValue);
    }
}
