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
package org.apache.maven.model.building;

import java.util.function.Supplier;

import org.apache.maven.building.Source;

/**
 * Caches auxiliary data used during model building like already processed raw/effective models. The data in the cache
 * is meant for exclusive consumption by the model builder and is opaque to the cache implementation. The cache key is
 * formed by a combination of group id, artifact id, version and tag. The first three components generally refer to the
 * identity of a model. The tag allows for further classification of the associated data on the sole discretion of the
 * model builder.
 *
 */
public interface ModelCache {

    <T> T computeIfAbsent(String groupId, String artifactId, String version, String tag, Supplier<T> data);

    <T> T computeIfAbsent(Source path, String tag, Supplier<T> data);
}
