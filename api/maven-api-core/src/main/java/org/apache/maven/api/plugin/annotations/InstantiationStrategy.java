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
package org.apache.maven.api.plugin.annotations;

import org.apache.maven.api.annotations.Experimental;

/**
 * Component instantiation strategy.
 *
 * @since 4.0
 */
@Experimental
public enum InstantiationStrategy {
    PER_LOOKUP("per-lookup"),
    SINGLETON("singleton"),
    KEEP_ALIVE("keep-alive"),
    POOLABLE("poolable");

    private final String id;

    InstantiationStrategy(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
}
