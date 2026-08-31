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
package org.apache.maven.internal.build;

import org.apache.maven.api.services.BuilderProblem;

import static java.util.Objects.requireNonNull;

/**
 * A deduplicated summary entry pairing a unique {@link BuilderProblem} with the
 * number of times it was reported across the build.
 */
record DefaultDiagnosticSummary(BuilderProblem problem, int count) {

    DefaultDiagnosticSummary {
        requireNonNull(problem, "problem");
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1");
        }
    }
}
