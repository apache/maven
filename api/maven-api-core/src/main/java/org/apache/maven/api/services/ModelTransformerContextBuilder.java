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
package org.apache.maven.api.services;

/**
 * The transformerContextBuilder is responsible for initializing the TransformerContext.
 * In case rawModels are missing, it could do new buildingRequests on the ModelBuilder.
 *
 * @since 4.0.0
 * @deprecated this should not be exposed
 * TODO refactor in MNG-8120
 */
@Deprecated
public interface ModelTransformerContextBuilder {
    /**
     * This method is used to initialize the TransformerContext
     *
     * @param request the modelBuildingRequest
     * @param problems the problemCollector
     * @return the mutable transformerContext
     */
    ModelTransformerContext initialize(ModelBuilderRequest request, ModelProblemCollector problems);
}
