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
package org.apache.maven.lifecycle.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Wraps individual MojoExecutions, containing information about completion status and scheduling.
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Kristian Rosenvold
 */
public class ExecutionPlanItem {
    private final MojoExecution mojoExecution;

    public ExecutionPlanItem(MojoExecution mojoExecution) {
        this.mojoExecution = mojoExecution;
    }

    public static List<ExecutionPlanItem> createExecutionPlanItems(
            MavenProject mavenProject, List<MojoExecution> executions) {
        BuilderCommon.attachToThread(mavenProject);

        List<ExecutionPlanItem> result = new ArrayList<>();
        for (MojoExecution mojoExecution : executions) {
            result.add(new ExecutionPlanItem(mojoExecution));
        }
        return result;
    }

    public MojoExecution getMojoExecution() {
        return mojoExecution;
    }

    public String getLifecyclePhase() {
        return mojoExecution.getLifecyclePhase();
    }

    public Plugin getPlugin() {
        final MojoDescriptor mojoDescriptor = getMojoExecution().getMojoDescriptor();
        return mojoDescriptor.getPluginDescriptor().getPlugin();
    }

    @Override
    public String toString() {
        return "ExecutionPlanItem{" + ", mojoExecution=" + mojoExecution + '}' + super.toString();
    }
}
