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
package org.apache.maven.plugin.internal;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;

class PluginConfigurationModule implements Module {

    private final Plugin plugin;

    PluginConfigurationModule(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void configure(Binder binder) {
        if (plugin.getKey() != null) {
            XmlNode configuration = plugin.getConfiguration();
            if (configuration == null) {
                configuration = new XmlNodeImpl("configuration");
            }
            binder.bind(XmlNode.class)
                    .annotatedWith(Names.named(plugin.getKey()))
                    .toInstance(configuration);
            binder.bind(PlexusConfiguration.class)
                    .annotatedWith(Names.named(plugin.getKey()))
                    .toInstance(XmlPlexusConfiguration.toPlexusConfiguration(configuration));
        }
    }
}
