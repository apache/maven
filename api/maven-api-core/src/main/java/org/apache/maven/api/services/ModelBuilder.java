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

    List<String> VALID_MODEL_VERSIONS = List.of(MODEL_VERSION_4_0_0, MODEL_VERSION_4_1_0);

    ModelBuilderSession newSession();

    interface ModelBuilderSession {

        ModelBuilderResult build(ModelBuilderRequest request) throws ModelBuilderException;
    }

    Model buildRawModel(ModelBuilderRequest request);
}
