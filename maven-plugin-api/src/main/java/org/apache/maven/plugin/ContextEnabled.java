package org.apache.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Map;

/**
 * Interface to allow <code>Mojos</code> to communicate with each others <code>Mojos</code>, other than
 * project's source root and project's attachment. The plugin manager populates it into the Mojo implementing this
 * interface, before executing it. Mojos also may access other contexts using Maven Session API.<br/>
 * Word of warning: Mojos live in their own classloader that is usually destroyed after Mojo is executed, and even
 * same Mojo within two different projects will different classloader. That said, (mis) using the context to store
 * anything that was loaded up by Mojo classloader is wrong to do and will lead to errors.<br/>
 * Regarding concurrency: best practice is to "write once" (for example invoked Mojo writes to its own context),
 * while other Mojos MAY get (via Maven Session API) and inspect (read) other Mojos contexts.
 */
public interface ContextEnabled
{
    /**
     * Set a new shared context <code>Map</code> to a mojo before executing it.
     *
     * @param pluginContext a new <code>Map</code>
     */
    void setPluginContext( Map<String, Object> pluginContext );

    /**
     * Gets the shared context of the mojo.
     *
     * @return a <code>Map</code> stored in the plugin container's context.
     */
    Map<String, Object> getPluginContext();
}
