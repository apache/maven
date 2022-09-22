package org.apache.maven.api;

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

import org.apache.maven.api.annotations.Experimental;

/**
 * The possible types of execution events.
 *
 * @since 4.0
 */
@Experimental
public enum EventType
{
    PROJECT_DISCOVERY_STARTED,
    SESSION_STARTED,
    SESSION_ENDED,
    PROJECT_SKIPPED,
    PROJECT_STARTED,
    PROJECT_SUCCEEDED,
    PROJECT_FAILED,
    MOJO_SKIPPED,
    MOJO_STARTED,
    MOJO_SUCCEEDED,
    MOJO_FAILED,
    FORK_STARTED,
    FORK_SUCCEEDED,
    FORK_FAILED,
    FORKED_PROJECT_STARTED,
    FORKED_PROJECT_SUCCEEDED,
    FORKED_PROJECT_FAILED,
}
