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

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

/**
 * @since 3.0
 * @author Kristian Rosenvold
 */
public interface LifeCyclePluginAnalyzer
{

    @Deprecated
    Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
        throws LifecycleMappingNotFoundException;

    /**
     * Gets a set of default build {@code Plugin}s for a given {@code Model} and a Maven execution with the given goals.
     *
     * @param model The model to get the default build {@code Plugin}s for.
     * @param goals A set of goals of the current Maven invokation.
     *
     * @return A set of default build {@code Plugin}s for {@code Model}.
     *
     * @throws LifecycleMappingNotFoundException if {@code model} does not declare a supported packaging.
     * @since 3.4
     */
    Set<Plugin> getDefaultBuildPlugins( Model model, Set<String> goals )
        throws LifecycleMappingNotFoundException;

}
