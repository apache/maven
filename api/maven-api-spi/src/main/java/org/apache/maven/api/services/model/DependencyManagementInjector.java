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
 * Handles injection of dependency management into the model.
 *
 * @since 4.0.0
 */
public interface DependencyManagementInjector {

    /**
     * Merges default values from the dependency management section of the given model into itself.
     *
     * @param model The model into which to merge the values specified by its dependency management sections, must not
     *            be <code>null</code>.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    Model injectManagement(Model model, ModelBuilderRequest request, ModelProblemCollector problems);

    /**
     * Builder-accepting variant that operates on a {@link Model.Builder} directly,
     * avoiding an intermediate {@code Model.build()} between pipeline stages.
     * <p>
     * The default implementation bridges to {@link #injectManagement(Model, ModelBuilderRequest, ModelProblemCollector)}
     * by building the model, processing it, and resetting the builder to the result.
     *
     * @param builder The model builder to modify in place, must not be {@code null}.
     * @param request The model building request, must not be {@code null}.
     * @param problems The container used to collect problems, must not be {@code null}.
     * @since 4.0.0
     */
    default void injectManagement(Model.Builder builder, ModelBuilderRequest request, ModelProblemCollector problems) {
        Model built = builder.build();
        Model result = injectManagement(built, request, problems);
        if (result != built) {
            builder.reset(result);
        }
    }
}
