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
package org.apache.maven.api.services.model;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;

/**
 * Handles normalization of a model. In this context, normalization is the process of producing a canonical
 * representation for models that physically look different but are semantically equivalent.
 *
 */
public interface ModelNormalizer {

    /**
     * Merges duplicate elements like multiple declarations of the same build plugin in the specified model.
     *
     * @param model The model whose duplicate elements should be merged, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    Model mergeDuplicates(Model model, ModelBuilderRequest request, ModelProblemCollector problems);

    /**
     * Sets default values in the specified model that for technical reasons cannot be set directly in the Modello
     * definition.
     *
     * @param model The model in which to set the default values, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    Model injectDefaultValues(Model model, ModelBuilderRequest request, ModelProblemCollector problems);
}
