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
package org.apache.maven.plugin;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

/**
 * MojoNotFoundException
 */
public class MojoNotFoundException extends Exception {
    private String goal;

    private PluginDescriptor pluginDescriptor;

    public MojoNotFoundException(String goal, PluginDescriptor pluginDescriptor) {
        super(toMessage(goal, pluginDescriptor));

        this.goal = goal;
        this.pluginDescriptor = pluginDescriptor;
    }

    public String getGoal() {
        return goal;
    }

    public PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    private static String toMessage(String goal, PluginDescriptor pluginDescriptor) {
        StringBuilder buffer = new StringBuilder(256);

        buffer.append("Could not find goal '").append(goal).append('\'');

        if (pluginDescriptor != null) {
            buffer.append(" in plugin ").append(pluginDescriptor.getId());

            buffer.append(" among available goals ");
            List<MojoDescriptor> mojos = pluginDescriptor.getMojos();
            if (mojos != null) {
                for (Iterator<MojoDescriptor> it = mojos.iterator(); it.hasNext(); ) {
                    MojoDescriptor mojo = it.next();
                    if (mojo != null) {
                        buffer.append(mojo.getGoal());
                    }
                    if (it.hasNext()) {
                        buffer.append(", ");
                    }
                }
            }
        }

        return buffer.toString();
    }
}
