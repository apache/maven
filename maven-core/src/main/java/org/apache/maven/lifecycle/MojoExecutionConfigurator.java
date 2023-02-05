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
package org.apache.maven.lifecycle;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * A MojoExecutionConfigurator is responsible for creating the configuration for Mojo based on configuration for a Mojo
 * in the MavenProject and the default configuration for the Mojo from the containing plugin's plugin.xml descriptor.
 * </p>
 * <strong>Note:</strong> This interface is part of work in progress and can be changed or removed without notice.
 * @author Jason van Zyl
 * @since 3.3.1, MNG-5753
 */
public interface MojoExecutionConfigurator {
    /**
     * Create the MojoExecution configuration based on configuration for a Mojo in the MavenProject and the
     * default configuration for the Mojo from the containing plugin's plugin.xml descriptor.
     *
     * @param project
     * @param mojoExecution
     * @param allowPluginLevelConfig
     */
    void configure(MavenProject project, MojoExecution mojoExecution, boolean allowPluginLevelConfig);
}
