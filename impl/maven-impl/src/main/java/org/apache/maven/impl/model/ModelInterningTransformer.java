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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.xml.XmlReaderRequest;

/**
 * A {@link XmlReaderRequest.Transformer} that interns frequently repeated XML text values
 * to reduce memory allocation and garbage collection pressure in large multi-module builds.
 *
 * <p>The set of XML contexts to intern is configurable via the
 * {@link Constants#MAVEN_MODEL_BUILDER_INTERNS} session property. If the property is absent
 * or empty, {@link #DEFAULT_CONTEXTS} is used.
 */
class ModelInterningTransformer implements XmlReaderRequest.Transformer {

    static final Set<String> DEFAULT_CONTEXTS = Set.of(
            // Core Maven coordinates
            "groupId",
            "artifactId",
            "version",
            "namespaceUri",
            "packaging",

            // Dependency-related fields
            "scope",
            "type",
            "classifier",

            // Build and plugin-related fields
            "phase",
            "goal",
            "execution",

            // Repository-related fields
            "layout",
            "policy",
            "checksumPolicy",
            "updatePolicy",

            // Common metadata fields
            "modelVersion",
            "name",
            "url",
            "system",
            "distribution",
            "status",

            // SCM fields
            "connection",
            "developerConnection",
            "tag",

            // Common enum-like values that appear frequently
            "id",
            "inherited",
            "optional");

    private final Set<String> contexts;

    /**
     * Creates a transformer with default contexts.
     */
    ModelInterningTransformer() {
        this.contexts = DEFAULT_CONTEXTS;
    }

    /**
     * Creates a transformer with contexts from session properties.
     * Reads {@link Constants#MAVEN_MODEL_BUILDER_INTERNS} from user then system properties.
     *
     * @param session the Maven session to read properties from
     */
    ModelInterningTransformer(Session session) {
        this.contexts = parseContextsFromSession(session);
    }

    private static Set<String> parseContextsFromSession(Session session) {
        String contextsProperty = session.getUserProperties().get(Constants.MAVEN_MODEL_BUILDER_INTERNS);
        if (contextsProperty == null) {
            contextsProperty = session.getSystemProperties().get(Constants.MAVEN_MODEL_BUILDER_INTERNS);
        }
        if (contextsProperty == null || contextsProperty.trim().isEmpty()) {
            return DEFAULT_CONTEXTS;
        }
        return Arrays.stream(contextsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public String transform(String input, String context) {
        return input != null && contexts.contains(context) ? input.intern() : input;
    }

    /**
     * Returns the contexts that will be interned by this transformer.
     * Used for testing purposes.
     */
    Set<String> getContexts() {
        return contexts;
    }
}
