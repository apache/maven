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
package org.apache.maven.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.SuperPomProvider;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.xml.XmlReaderRequest;

@Named
@Singleton
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
    public Model getSuperPom(String version) {
        return SUPER_MODELS.computeIfAbsent(Objects.requireNonNull(version), v -> readModel(version, v));
    }

    private Model readModel(String version, String v) {
        String resource = "/org/apache/maven/model/pom-" + v + ".xml";
        URL url = getClass().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("The super POM " + resource + " was not found"
                    + ", please verify the integrity of your Maven installation");
        }
        try (InputStream is = url.openStream()) {
            String modelId = "org.apache.maven:maven-api-impl:"
                    + this.getClass().getPackage().getImplementationVersion() + ":super-pom-" + version;
            return modelProcessor.read(XmlReaderRequest.builder()
                    .modelId(modelId)
                    .location(url.toExternalForm())
                    .inputStream(is)
                    .strict(false)
                    .build());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "The super POM " + resource + " is damaged"
                            + ", please verify the integrity of your Maven installation",
                    e);
        }
    }
}
