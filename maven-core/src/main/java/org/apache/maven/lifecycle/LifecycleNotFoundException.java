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
package org.apache.maven.lifecycle;

/**
 * Signals a failure to locate a lifecycle.
 *
 * @author Benjamin Bentmann
 */
public class LifecycleNotFoundException extends Exception {

    private final String lifecycleId;

    /**
     * Creates a new exception to indicate that the specified lifecycle is unknown.
     *
     * @param lifecycleId The identifier of the lifecycle that could not be located, may be {@code null}.
     */
    public LifecycleNotFoundException(String lifecycleId) {
        super("Unknown lifecycle " + lifecycleId);
        this.lifecycleId = (lifecycleId != null) ? lifecycleId : "";
    }

    /**
     * Gets the identifier of the lifecycle that was not found.
     *
     * @return The identifier of the lifecycle that was not found, never {@code null}.
     */
    public String getLifecycleId() {
        return lifecycleId;
    }
}
