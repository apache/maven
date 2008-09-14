package org.apache.maven.shared.model;

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

import java.io.IOException;
import java.util.List;

/**
 * Provides services for transforming domain models to property lists and vice versa.
 * ModelTransformer.transformToDomainModel == ModelTransformer.transformToModelProperties if list of model
 * properties specified in transformToDomainModel contains only one property with a uri of http://apache.org/model/project.
 */
public interface ModelTransformer
{

    String getBaseUri();

    /**
     * Transforms specified list of model properties into a single domain model. The list may contain a hierarchy (inheritance) of
     * model information.
     *
     * @param properties list of model properties to transform into domain model. List may not be null.
     * @return domain model
     */
    DomainModel transformToDomainModel( List<ModelProperty> properties )
        throws IOException;

    /**
     * Transforms specified list of domain models to a property list. The list of domain models should be in order of
     * most specialized to least specialized model.
     *
     * @param domainModels list of domain models to transform to a list of model properties. List may not be null.
     * @return list of model properties
     */
    List<ModelProperty> transformToModelProperties(List<DomainModel> domainModels )
        throws IOException;

    /**
     *
     * @param modelProperties
     * @param interpolatorProperties
     * @param domainModel
     * @throws IOException
     */
    void interpolateModelProperties(List<ModelProperty> modelProperties,
                                    List<InterpolatorProperty> interpolatorProperties,
                                    DomainModel domainModel)
        throws IOException;
}
