package org.apache.maven.toolchain.building;

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

import java.util.List;

import org.apache.maven.building.Problem;
import org.apache.maven.toolchain.model.PersistedToolchains;

/**
 * Collects the output of the toolchains builder.
 *
 * @author Robert Scholte
 * @since 3.3.0
 */
public interface ToolchainsBuildingResult
{

    /**
     * Gets the assembled toolchains.
     *
     * @return The assembled toolchains, never {@code null}.
     */
    PersistedToolchains getEffectiveToolchains();

    /**
     * Return a list of problems, if any.
     *
     * @return a list of problems, never {@code null}.
     */
    List<Problem> getProblems();

}
