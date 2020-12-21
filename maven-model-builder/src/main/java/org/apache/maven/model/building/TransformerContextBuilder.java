package org.apache.maven.model.building;

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

/**
 * The transformerContextBuilder is responsible for initializing the TransformerContext.
 * In case rawModels are missing, it could do new buildingRequests on the ModelBuilder.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public interface TransformerContextBuilder
{
    /**
     * This method is used to initialize the TransformerContext
     *
     * @param request the modelBuildingRequest
     * @param problems the problemCollector
     * @return the mutable transformerContext
     */
    TransformerContext initialize( ModelBuildingRequest request, ModelProblemCollector problems );

    /**
     * The immutable transformerContext, can be used after the buildplan is finished.
     *
     * @return the immutable transformerContext
     */
    TransformerContext build();
}
