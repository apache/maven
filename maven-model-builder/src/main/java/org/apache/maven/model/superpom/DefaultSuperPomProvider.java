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
package org.apache.maven.model.superpom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;

/**
 * Provides the super POM that all models implicitly inherit from.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultSuperPomProvider implements SuperPomProvider {

    private final ModelProcessor modelProcessor;

    /**
     * The cached super POM, lazily created.
     */
    private static final Map<String, Model> SUPER_MODELS = new ConcurrentHashMap<>();

    @Inject
    public DefaultSuperPomProvider(ModelProcessor modelProcessor) {
        this.modelProcessor = modelProcessor;
    }

    @Override
    public Model getSuperModel(String version) {
        return SUPER_MODELS.computeIfAbsent(Objects.requireNonNull(version), v -> {
            String resource = "/org/apache/maven/model/pom-" + v + ".xml";

            InputStream is = getClass().getResourceAsStream(resource);

            if (is == null) {
                throw new IllegalStateException("The super POM " + resource + " was not found"
                        + ", please verify the integrity of your Maven installation");
            }

            try {
                Map<String, Object> options = new HashMap<>(2);
                options.put("xml:" + version, "xml:" + version);

                String modelId = "org.apache.maven:maven-model-builder:" + version + "-"
                        + this.getClass().getPackage().getImplementationVersion() + ":super-pom";
                InputSource inputSource = new InputSource(
                        modelId, getClass().getResource(resource).toExternalForm());
                options.put(ModelProcessor.INPUT_SOURCE, new org.apache.maven.model.InputSource(inputSource));

                return modelProcessor.read(is, options);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "The super POM " + resource + " is damaged"
                                + ", please verify the integrity of your Maven installation",
                        e);
            }
        });
    }
}
