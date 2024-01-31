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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Packaging;
import org.apache.maven.api.Type;
import org.apache.maven.api.services.PackagingRegistry;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;

/**
 * TODO: this is session scoped as SPI can contribute.
 */
@Named
@Singleton
public class DefaultPackagingRegistry implements PackagingRegistry {
    private final Map<String, LifecycleMapping> lifecycleMappings;

    private final TypeRegistry typeRegistry;

    @Inject
    public DefaultPackagingRegistry(Map<String, LifecycleMapping> lifecycleMappings, TypeRegistry typeRegistry) {
        this.lifecycleMappings = lifecycleMappings;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public Optional<Packaging> lookup(String id) {
        LifecycleMapping lifecycleMapping = lifecycleMappings.get(id);
        if (lifecycleMapping == null) {
            return Optional.empty();
        }
        Type type = typeRegistry.lookup(id).orElse(null);
        if (type == null) {
            return Optional.empty();
        }

        return Optional.of(new Packaging() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Type getType() {
                return type;
            }
        });
    }
}
