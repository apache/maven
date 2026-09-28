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

import java.util.List;

import org.apache.maven.api.Service;
import org.apache.maven.api.model.Model;

public interface ModelBuilder extends Service {

    String MODEL_VERSION_4_0_0 = "4.0.0";

    String MODEL_VERSION_4_1_0 = "4.1.0";

    String MODEL_VERSION_4_2_0 = "4.2.0";

    List<String> KNOWN_MODEL_VERSIONS = List.of(MODEL_VERSION_4_0_0, MODEL_VERSION_4_1_0, MODEL_VERSION_4_2_0);

    ModelBuilderSession newSession();

    interface ModelBuilderSession {

        ModelBuilderResult build(ModelBuilderRequest request) throws ModelBuilderException;

        /**
         * Validates the model described by the request, collecting every problem instead of
         * throwing on the first one.
         * <p>
         * Unlike {@link #build(ModelBuilderRequest)} this does not throw when validation reports
         * errors: the result carries errors and warnings alike, so a caller that only wants to
         * report on a POM can see warnings that come with no error. A problem severe enough that
         * the model cannot be read at all is still thrown.
         * <p>
         * This implementation stops after the raw model, so no inheritance, interpolation or
         * profile injection happens and only the source, the file model, the raw model and the
         * problems are populated on the result. {@link ModelBuilderResult#getEffectiveModel()}
         * and the other accessors describing a fully built model are not.
         * <p>
         * Reading one model can need others: a subproject may leave out its parent version, which
         * is then taken from the parent's own file model. An implementation may therefore read
         * files besides the one the request names, up to the project root. Nothing is resolved
         * from a repository and nothing is downloaded.
         * <p>
         * The session holds what it builds for its own lifetime, so give a validation run a
         * session of its own rather than sharing one with {@link #build(ModelBuilderRequest)}.
         *
         * @param request the request containing the parameters for reading the model
         * @return the result, carrying the model that was read and the problems collected
         * @throws ModelBuilderException if the model cannot be read at all
         * @throws UnsupportedOperationException if this implementation does not support it
         * @since 4.1.0
         */
        default ModelBuilderResult validate(ModelBuilderRequest request) throws ModelBuilderException {
            throw new UnsupportedOperationException(getClass().getName() + " does not support validating a model");
        }
    }

    Model buildRawModel(ModelBuilderRequest request) throws ModelBuilderException;
}
