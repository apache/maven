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
package org.apache.maven.lifecycle.providers;

import javax.inject.Provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

/**
 * Base lifecycle provider.
 */
public abstract class AbstractLifecycleProvider implements Provider<Lifecycle> {
    private final Lifecycle lifecycle;

    protected AbstractLifecycleProvider(String id, String[] phases, String[] pluginBindings) {
        HashMap<String, LifecyclePhase> defaultBindings = null;
        if (pluginBindings != null) {
            final int len = pluginBindings.length;

            if (len < 2 || len % 2 != 0) {
                throw new IllegalArgumentException("Plugin bindings must have more than 0, even count of elements");
            }

            defaultBindings = new LinkedHashMap<>(len / 2);

            for (int i = 0; i < len; i += 2) {
                defaultBindings.put(pluginBindings[i], new LifecyclePhase(pluginBindings[i + 1]));
            }
        }

        this.lifecycle = new Lifecycle(
                id,
                Collections.unmodifiableList(Arrays.asList(phases)),
                defaultBindings == null ? null : Collections.unmodifiableMap(defaultBindings));
    }

    @Override
    public Lifecycle get() {
        return lifecycle;
    }
}
