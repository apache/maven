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
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelTransformerContext;

import java.nio.file.Path;

/**
 * The {@code ModelTransformer} interface is used to transform {@link Model}s parsed (currently in early stage).
 * This allows plugging in additional functionalities like enriching model or inlining.
 *
 * @since 4.0.0
 */
@Experimental
@Consumer
public interface ModelTransformer extends SpiService {
    /**
     * Transform the raw model obtained previously by a previous call to model parser (just parsed from input file or stream).
     *
     * @param context the transformer context, never {@code null}
     * @param model the model, never {@code null}
     * @param path the POM path being transformed, never {@code null}
     * @param builder the model builder, never {@code null}
     * @throws ModelTransformerException if the model cannot be transformed
     */
    @Nonnull
    void transformFileModel(ModelTransformerContext context, Model model, Path path, Model.Builder builder) throws ModelTransformerException;
}
