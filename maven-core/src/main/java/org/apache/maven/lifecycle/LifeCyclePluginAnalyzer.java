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

    /**
     * @deprecated As of Maven 3.6.0, replaced by method {@link #getLifecycleModel(org.apache.maven.model.Model)}.
     */
    @Deprecated
    Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
        throws LifecycleMappingNotFoundException;

    /**
     * Gets the lifecycle {@code Model} for a given {@code Model}.
     * <p>
     * The lifecycle model for a given model is the list of default build plugins plus lifecycle plugin execution
     * management.
     * </p>
     *
     * @param model The {@code Model} to get the lifecycle {@code Model} for.
     *
     * @return The lifecycle {@code Model} for {@code model}.
     *
     * @throws NullPointerException if {@code model} is {@code null}.
     * @throws LifecycleMappingNotFoundException if {@code model} declares an unsupported packaging.
     *
     * @since 3.6.0
     */
    Model getLifecycleModel( Model model )
        throws LifecycleMappingNotFoundException;

}
