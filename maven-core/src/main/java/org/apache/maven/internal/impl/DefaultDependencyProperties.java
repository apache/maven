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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.DependencyProperties;
import org.apache.maven.api.annotations.Nonnull;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * Default implementation of artifact properties.
 */
public class DefaultDependencyProperties implements DependencyProperties {
    private final Map<String, String> properties;

    public DefaultDependencyProperties(String... flags) {
        this(Arrays.asList(flags));
    }

    public DefaultDependencyProperties(Collection<String> flags) {
        HashMap<String, String> map = new HashMap<>();
        for (String flag : flags) {
            map.put(flag, Boolean.TRUE.toString());
        }
        this.properties = Collections.unmodifiableMap(map);
    }

    public DefaultDependencyProperties(@Nonnull Map<String, String> properties) {
        this.properties = nonNull(properties, "properties can not be null");
    }

    @Override
    public Map<String, String> asMap() {
        return properties;
    }

    @Override
    public boolean checkFlag(String flag) {
        nonNull(flag, "null flag");
        return Boolean.parseBoolean(properties.getOrDefault(flag, ""));
    }
}
