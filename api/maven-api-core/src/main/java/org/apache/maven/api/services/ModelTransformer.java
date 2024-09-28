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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Model;

/**
 * A model transformer.
 *
 * @since 4.0.0
 */
@Experimental
public interface ModelTransformer {

    /**
     * Apply a transformation on the file model.
     *
     * @param model the input model
     * @param problems the problem collector to report any issues encountered during transformation
     * @return the transformed model, or the input model if no transformation is needed
     */
    @Nonnull
    Model transform(
            @Nonnull Model model, @Nonnull ModelBuilderRequest request, @Nonnull ModelProblemCollector problems);
}
