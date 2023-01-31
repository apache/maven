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
package org.apache.maven.model.interpolation;

import java.util.Properties;

import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * Allows a fixed set of properties that are valid inside a version and that could be overwritten for example on the
 * commandline
 */
public interface ModelVersionProcessor {

    /**
     * @param property the property to check
     * @return <code>true</code> if this is a valid property for this processor
     */
    boolean isValidProperty(String property);

    /**
     * This method is responsible for examining the request and possibly overwrite of the valid properties in the model
     *
     * @param modelProperties
     * @param request
     */
    void overwriteModelProperties(Properties modelProperties, ModelBuildingRequest request);
}
