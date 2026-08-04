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
package org.apache.maven.internal.build.incremental.impl.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.build.incremental.spi.CommittableIncrementalContext;
import org.apache.maven.api.build.incremental.spi.IncrementalContextFinalizer;
import org.apache.maven.api.di.MojoExecutionScoped;
import org.apache.maven.api.di.Named;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;

@Named
@MojoExecutionScoped
public class MavenIncrementalContextFinalizer implements WeakMojoExecutionListener, IncrementalContextFinalizer {

    private final List<CommittableIncrementalContext> contexts = new ArrayList<>();

    public void registerContext(CommittableIncrementalContext context) {
        contexts.add(context);
    }

    @Override
    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        if (contexts.isEmpty()) {
            return;
        }
        try {
            for (CommittableIncrementalContext context : contexts) {
                context.commit();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not maintain incremental build state", e);
        }
    }

    @Override
    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {}

    @Override
    public void afterExecutionFailure(MojoExecutionEvent event) {}
}
