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
package org.apache.maven.api.spi;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelTransformerException;

/**
 * Marker interface for model transformers.
 *
 * @since 4.0.0
 */
@Experimental
@Consumer
@Named
public interface ModelTransformer extends SpiService {

    /**
     * Apply a transformation on the file model.
     *
     * This method will be called on each file model being loaded,
     * just before validation.
     *
     * @param model the input model
     * @return the transformed model, or the input model if no transformation is needed
     * @throws ModelTransformerException
     */
    @Nonnull
    default Model transformFileModel(@Nonnull Model model) throws ModelTransformerException {
        return model;
    }

    /**
     * Apply a transformation on the raw models.
     *
     * This method will be called on each raw model being loaded,
     * just before validation.
     *
     * @param model the input model
     * @return the transformed model, or the input model if no transformation is needed
     * @throws ModelTransformerException
     */
    @Nonnull
    default Model transformRawModel(@Nonnull Model model) throws ModelTransformerException {
        return model;
    }

    /**
     * Apply a transformation on the effective models.
     *
     * This method will be called on each effective model being loaded,
     * just before validation.
     *
     * @param model the input model
     * @return the transformed model, or the input model if no transformation is needed
     * @throws ModelTransformerException
     */
    @Nonnull
    default Model transformEffectiveModel(@Nonnull Model model) throws ModelTransformerException {
        return model;
    }
}
