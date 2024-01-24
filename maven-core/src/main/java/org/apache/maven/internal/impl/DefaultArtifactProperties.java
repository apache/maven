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

import java.util.*;

import org.apache.maven.api.ArtifactProperties;
import org.apache.maven.api.annotations.Nonnull;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * Default implementation of artifact properties.
 */
public class DefaultArtifactProperties implements ArtifactProperties {
    private final Map<String, String> properties;

    public DefaultArtifactProperties(String... flags) {
        this(Arrays.asList(flags));
    }

    public DefaultArtifactProperties(@Nonnull Collection<String> flags) {
        nonNull(flags, "flags");
        HashMap<String, String> map = new HashMap<>();
        for (String flag : flags) {
            map.put(flag, Boolean.TRUE.toString());
        }
        this.properties = Collections.unmodifiableMap(map);
    }

    public DefaultArtifactProperties(@Nonnull Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(nonNull(properties, "properties"));
    }

    @Nonnull
    @Override
    public Map<String, String> asMap() {
        return properties;
    }

    @Override
    public boolean checkFlag(@Nonnull String flag) {
        nonNull(flag, "flag");
        return Boolean.parseBoolean(properties.getOrDefault(flag, ""));
    }
}
