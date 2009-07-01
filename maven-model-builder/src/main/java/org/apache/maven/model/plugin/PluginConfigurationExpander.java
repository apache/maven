package org.apache.maven.model.plugin;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * Handles expansion of general plugin configuration into individual executions and report sets.
 * 
 * @author Benjamin Bentmann
 */
public interface PluginConfigurationExpander
{

    /**
     * Merges values from general plugin configuration into the individual plugin executions and reports sets of the
     * given model.
     * 
     * @param model The model whose plugin configuration should be expanded, must not be <code>null</code>.
     * @param request The model building request that holds further settings, must not be {@code null}.
     */
    void expandPluginConfiguration( Model model, ModelBuildingRequest request );

}
