package org.apache.maven.execution;

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

import java.util.Optional;

/**
 * Instances of this class are responsible for determining whether it makes sense to "resume" a build (i.e., using
 * the {@code --resume} flag.
 */
public interface BuildResumptionAnalyzer
{
    /**
     * Construct an instance of {@link BuildResumptionData} based on the outcome of the current Maven build.
     * @param result Outcome of the current Maven build.
     * @return A {@link BuildResumptionData} instance or {@link Optional#empty()} if resuming the build is not possible.
     */
    Optional<BuildResumptionData> determineBuildResumptionData( MavenExecutionResult result );
}
