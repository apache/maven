package org.apache.maven.plugin.registry;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public interface MavenPluginRegistryBuilder
{
    String ROLE = MavenPluginRegistryBuilder.class.getName();
    
    String ALT_USER_PLUGIN_REG_LOCATION = "org.apache.maven.user-plugin-registry";
    String ALT_GLOBAL_PLUGIN_REG_LOCATION = "org.apache.maven.global-plugin-registry";

    PluginRegistry buildPluginRegistry()
        throws IOException, XmlPullParserException;
    
    PluginRegistry createUserPluginRegistry();
    
}
