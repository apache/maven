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
package org.apache.maven.its.mng5581.lifecyclemappingdelegate;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import javax.inject.Named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.MavenProject;

@Named("test-only")
public class TestLifecycleMappingDelegate implements LifecycleMappingDelegate {

    public Map<String, List<MojoExecution>> calculateLifecycleMappings(
            MavenSession session, MavenProject project, Lifecycle lifecycle, String lifecyclePhase)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException {

        Map<String, List<MojoExecution>> pluginExecutions = new LinkedHashMap<String, List<MojoExecution>>();

        for (Plugin plugin : project.getBuild().getPlugins()) {
            for (PluginExecution execution : plugin.getExecutions()) {
                List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();
                for (String goal : execution.getGoals()) {
                    MojoExecution mojoExecution = new MojoExecution(plugin, goal, execution.getId());
                    mojoExecution.setLifecyclePhase(execution.getPhase());
                    mojoExecutions.add(mojoExecution);
                }
                if (!mojoExecutions.isEmpty()) {
                    pluginExecutions.put(getExecutionKey(plugin, execution), mojoExecutions);
                }
            }
        }

        List<MojoExecution> result = new ArrayList<MojoExecution>();

        List<MojoExecution> mojoExecutions =
                pluginExecutions.get("org.apache.maven.plugins:maven-surefire-plugin:default-test");
        if (mojoExecutions != null) {
            result.addAll(mojoExecutions);
        }

        return Collections.singletonMap("test-only", result);
    }

    private String getExecutionKey(Plugin plugin, PluginExecution execution) {
        return plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + execution.getId();
    }
}
