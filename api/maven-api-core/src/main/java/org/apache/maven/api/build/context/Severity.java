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
package org.apache.maven.api.build.context;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;

/**
 * Severity level for build messages attached to resources.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public enum Severity {
    /** An error that should fail the build (unless fail-on-error is disabled). */
    ERROR,
    /** A warning that does not fail the build. */
    WARNING,
    /** An informational message. */
    INFO
}
