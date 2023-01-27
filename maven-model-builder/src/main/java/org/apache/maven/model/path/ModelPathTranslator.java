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
package org.apache.maven.model.path;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * Resolves relative paths of a model against a specific base directory.
 *
 * @author Jason van Zyl
 */
public interface ModelPathTranslator {

    /**
     * Resolves the well-known paths of the specified model against the given base directory. Paths within plugin
     * configuration are not processed.
     *
     * @param model The model whose paths should be resolved, may be {@code null}.
     * @param basedir The base directory to resolve relative paths against, may be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     */
    void alignToBaseDirectory(Model model, File basedir, ModelBuildingRequest request);
}
