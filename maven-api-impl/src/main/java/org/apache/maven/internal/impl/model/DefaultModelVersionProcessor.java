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
package org.apache.maven.internal.impl.model;

import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.model.ModelVersionProcessor;

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
    public void overwriteModelProperties(Properties modelProperties, ModelBuilderRequest request) {
        Map<String, String> props = request.getUserProperties();
        if (props.containsKey(REVISION_PROPERTY)) {
            modelProperties.put(REVISION_PROPERTY, props.get(REVISION_PROPERTY));
        }
        if (props.containsKey(CHANGELIST_PROPERTY)) {
            modelProperties.put(CHANGELIST_PROPERTY, props.get(CHANGELIST_PROPERTY));
        }
        if (props.containsKey(SHA1_PROPERTY)) {
            modelProperties.put(SHA1_PROPERTY, props.get(SHA1_PROPERTY));
        }
    }
}
