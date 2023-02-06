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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;

/**
 * A simple model problem collector for testing the model building components.
 *
 * @author Benjamin Bentmann
 */
public class SimpleProblemCollector implements ModelProblemCollector {
    private Model model;

    private List<String> warnings = new ArrayList<>();

    private List<String> errors = new ArrayList<>();

    private List<String> fatals = new ArrayList<>();

    public SimpleProblemCollector() {}

    public SimpleProblemCollector(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getFatals() {
        return fatals;
    }

    public void add(ModelProblemCollectorRequest req) {
        switch (req.getSeverity()) {
            case FATAL:
                fatals.add(req.getMessage());
                break;
            case ERROR:
                errors.add(req.getMessage());
                break;
            case WARNING:
                warnings.add(req.getMessage());
                break;
        }
    }
}
