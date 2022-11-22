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
package org.apache.maven.api.model;

import java.io.Serializable;

/**
 * Class InputSource.
 */
public class InputSource implements Serializable {

    private final String modelId;
    private final String location;

    public InputSource(String modelId, String location) {
        this.modelId = modelId;
        this.location = location;
    }

    /**
     * Get the path/URL of the POM or {@code null} if unknown.
     *
     * @return the location
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Get the identifier of the POM in the format {@code <groupId>:<artifactId>:<version>}.
     *
     * @return the model id
     */
    public String getModelId() {
        return this.modelId;
    }

    @Override
    public String toString() {
        return getModelId() + " " + getLocation();
    }
}
