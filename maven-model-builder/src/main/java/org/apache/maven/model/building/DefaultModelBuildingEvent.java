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
package org.apache.maven.model.building;

import org.apache.maven.model.Model;

/**
 * Holds data relevant for a model building event.
 *
 * @author Benjamin Bentmann
 */
class DefaultModelBuildingEvent implements ModelBuildingEvent {

    private final Model model;

    private final ModelBuildingRequest request;

    private final ModelProblemCollector problems;

    DefaultModelBuildingEvent(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        this.model = model;
        this.request = request;
        this.problems = problems;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public ModelBuildingRequest getRequest() {
        return request;
    }

    @Override
    public ModelProblemCollector getProblems() {
        return problems;
    }
}
