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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Properties;

import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * Maven default implementation of the {@link ModelVersionProcessor} to support
 * <a href="https://maven.apache.org/maven-ci-friendly.html">CI Friendly Versions</a>
 */
@Named
@Singleton
public class DefaultModelVersionProcessor implements ModelVersionProcessor {

    private static final String SHA1_PROPERTY = "sha1";

    private static final String CHANGELIST_PROPERTY = "changelist";

    private static final String REVISION_PROPERTY = "revision";

    @Override
    public boolean isValidProperty(String property) {
        return REVISION_PROPERTY.equals(property)
                || CHANGELIST_PROPERTY.equals(property)
                || SHA1_PROPERTY.equals(property);
    }

    @Override
    public void overwriteModelProperties(Properties modelProperties, ModelBuildingRequest request) {
        if (request.getUserProperties().containsKey(REVISION_PROPERTY)) {
            modelProperties.put(REVISION_PROPERTY, request.getUserProperties().get(REVISION_PROPERTY));
        }
        if (request.getUserProperties().containsKey(CHANGELIST_PROPERTY)) {
            modelProperties.put(CHANGELIST_PROPERTY, request.getUserProperties().get(CHANGELIST_PROPERTY));
        }
        if (request.getUserProperties().containsKey(SHA1_PROPERTY)) {
            modelProperties.put(SHA1_PROPERTY, request.getUserProperties().get(SHA1_PROPERTY));
        }
    }
}
