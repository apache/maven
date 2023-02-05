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
package org.apache.maven.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Centralized class for feature information
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public final class Features {
    private Features() {}

    public static Feature buildConsumer(Properties userProperties) {
        return buildConsumer(toMap(userProperties));
    }

    public static Feature buildConsumer(Map<String, String> userProperties) {
        return new Feature(userProperties, "maven.experimental.buildconsumer", "true");
    }

    private static Map<String, String> toMap(Properties properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next,
                        HashMap::new));
    }

    /**
     * Represents some feature
     *
     * @author Robert Scholte
     * @since 4.0.0
     */
    public static class Feature {
        private final boolean active;

        private final String name;

        Feature(Map<String, String> userProperties, String name, String defaultValue) {
            this.name = name;
            this.active = "true".equals(userProperties.getOrDefault(name, defaultValue));
        }

        public boolean isActive() {
            return active;
        }

        public String propertyName() {
            return name;
        }
    }
}
