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

/**
 * Structured build report data model.
 * <p>
 * The {@link org.apache.maven.api.build.report.BuildReport} is the root of a structured
 * representation of a Maven build execution. It is persisted to
 * {@code target/build-report.json} at the end of every build and can be consumed
 * by tools, CI systems, IDEs, and LLM agents without re-running the build or
 * parsing console output.
 * <p>
 * Build problems (warnings, errors) are represented as
 * {@link org.apache.maven.api.services.BuilderProblem} instances, deduplicated
 * by {@link org.apache.maven.api.services.BuilderProblem#getKey() key} and
 * summarized at the end of the build.
 *
 * @since 4.1.0
 */
@Experimental
package org.apache.maven.api.build.report;

import org.apache.maven.api.annotations.Experimental;
