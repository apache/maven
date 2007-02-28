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
 * project's source root and project's attachment.
 * <br/>
 * The plugin manager would pull the context out of the plugin container context, and populate it into the Mojo.
 *
 * @author jdcasey
 * @version $Id$
 */
public interface ContextEnabled
{
    /**
     * Set a new shared context <code>Map</code> to a mojo before executing it.
     *
     * @param pluginContext a new <code>Map</code>
     */
    void setPluginContext( Map pluginContext );

    /**
     * @return a <code>Map</code> stored in the plugin container's context.
     */
    Map getPluginContext();
}
