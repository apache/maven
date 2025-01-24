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
package org.apache.maven.impl.model;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelSource;

/**
 * Holds a model along with some auxiliary information. This internal utility class assists the model builder during POM
 * processing by providing a means to transport information that cannot be (easily) extracted from the model itself.
 */
record ModelData(ModelSource source, Model model) {

    /**
     * Gets unique identifier of the model
     *
     * @return The effective identifier of the model, never {@code null}.
     */
    public String id() {
        // TODO: this should be model.getId() but it fails for some reason
        // if source is null, it is the super model, which can be accessed via empty string
        return source != null ? source.getLocation() : "";
    }

    @Override
    public String toString() {
        return String.valueOf(model);
    }
}
