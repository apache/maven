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
package org.apache.maven.cling.event;

import java.time.Duration;

/**
 * Sink for receiving synchronized project durations and overall session build results.
 *
 * @since 4.1.0
 */
public interface BuildStatsSink {

    /**
     * Record the execution duration of a project by its project key ("groupId:artifactId").
     *
     * @param key the project key
     * @param duration the project duration
     */
    void recordProjectDuration(String key, Duration duration);

    /**
     * Synchronize the session result summary.
     *
     * @param hasExceptions whether the build had exceptions
     * @param passed number of passed projects
     * @param failed number of failed projects
     * @param skipped number of skipped projects
     */
    void syncResult(boolean hasExceptions, int passed, int failed, int skipped);
}
