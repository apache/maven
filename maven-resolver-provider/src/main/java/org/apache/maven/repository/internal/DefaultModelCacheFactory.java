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
package org.apache.maven.repository.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.building.ModelCache;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Default implementation of {@link ModelCacheFactory}.
 */
@Singleton
@Named
public class DefaultModelCacheFactory implements ModelCacheFactory {
    /**
     * Flag that makes possible to shut down model cache use, mostly useful
     * for debugging and/or thread dumps. Default value: {@code true}.
     */
    private static final String USE_MODEL_CACHE = "maven.modelBuilder.useModelCache";

    @Override
    public ModelCache createCache(RepositorySystemSession session) {
        if (ConfigUtils.getBoolean(session, true, USE_MODEL_CACHE)) {
            return DefaultModelCache.newInstance(session);
        }
        return null;
    }
}
