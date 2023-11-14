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
package org.apache.maven.internal.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.DependencyProperties;
import org.apache.maven.api.Type;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultType implements Type, ArtifactType {
    private final String extension;

    private final String classifier;

    private final DependencyProperties dependencyProperties;

    public DefaultType(String id, String extension, String classifier, DependencyProperties dependencyProperties) {
        nonNull(id, "null id");
        this.extension = nonNull(extension, "null extension");
        this.classifier = classifier;
        nonNull(dependencyProperties, "null dependencyProperties");
        HashMap<String, String> props = new HashMap<>(dependencyProperties.asMap());
        props.put(ArtifactProperties.TYPE, id);
        this.dependencyProperties = new DefaultDependencyProperties(props);
    }

    @Override
    public String getId() {
        return dependencyProperties.asMap().get(ArtifactProperties.TYPE);
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public DependencyProperties getDependencyProperties() {
        return dependencyProperties;
    }

    @Override
    public Map<String, String> getProperties() {
        return getDependencyProperties().asMap();
    }

    public static ArtifactType wrap(Type type) {
        return new DefaultType(type.getId(), type.getExtension(), type.getClassifier(), type.getDependencyProperties());
    }
}
