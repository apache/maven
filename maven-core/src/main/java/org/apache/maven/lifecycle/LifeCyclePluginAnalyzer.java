package org.apache.maven.lifecycle;

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

import java.util.Set;
import org.apache.maven.model.Plugin;

/**
 * @since 3.0
 * @author Kristian Rosenvold
 */
public interface LifeCyclePluginAnalyzer
{

    @Deprecated
    Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging );

    /**
     * Gets the lifecycle {@code Plugin}s for a given packaging and set of phases.
     *
     * @param packaging The packaging to get plugins for.
     * @param phases The phases to get plugins for.
     *
     * @return All lifecycle {@code Plugin}s for the given {@code packaging} and {@code phases}
     * or {@code null}, if {@code packaging} does not identify a supported packaging.
     *
     * @since 3.4
     */
    Set<Plugin> getPlugins( String packaging, Set<String> phases );

}
