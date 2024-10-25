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
package org.apache.maven.execution.scope.internal;

import com.google.inject.AbstractModule;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * MojoExecutionScopeModule
 */
public class MojoExecutionScopeModule extends AbstractModule {
    protected final MojoExecutionScope scope;

    public MojoExecutionScopeModule(MojoExecutionScope scope) {
        this.scope = scope;
    }

    @Override
    protected void configure() {
        bindScope(MojoExecutionScoped.class, scope);
        // bindScope(org.apache.maven.api.di.MojoExecutionScoped.class, scope);
        bind(MojoExecutionScope.class).toInstance(scope);
        bind(MavenProject.class)
                .toProvider(MojoExecutionScope.seededKeyProvider(MavenProject.class))
                .in(scope);
        bind(MojoExecution.class)
                .toProvider(MojoExecutionScope.seededKeyProvider(MojoExecution.class))
                .in(scope);
        bind(Log.class)
                .toProvider(MojoExecutionScope.seededKeyProvider(Log.class))
                .in(scope);
        bind(org.apache.maven.api.Project.class)
                .toProvider(MojoExecutionScope.seededKeyProvider(org.apache.maven.api.Project.class))
                .in(scope);
        bind(org.apache.maven.api.MojoExecution.class)
                .toProvider(MojoExecutionScope.seededKeyProvider(org.apache.maven.api.MojoExecution.class))
                .in(scope);
    }
}
