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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.building.Problem;
import org.apache.maven.toolchain.model.PersistedToolchains;

/**
 * Holds the result of the merged toolchains and holds the problems during this build, if any.
 *
 * @author Robert Scholte
 * @since 3.3.0
 */
public class DefaultToolchainsBuildingResult
    implements ToolchainsBuildingResult
{

    private PersistedToolchains effectiveToolchains;

    private List<Problem> problems;

    /**
     * Default constructor
     *
     * @param effectiveToolchains the merged toolchains, may not be {@code null}
     * @param problems the problems while building the effectiveToolchains, if any.
     */
    public DefaultToolchainsBuildingResult( PersistedToolchains effectiveToolchains, List<Problem> problems )
    {
        this.effectiveToolchains = effectiveToolchains;
        this.problems = ( problems != null ) ? problems : new ArrayList<>();
    }

    @Override
    public PersistedToolchains getEffectiveToolchains()
    {
        return effectiveToolchains;
    }

    @Override
    public List<Problem> getProblems()
    {
        return problems;
    }

}
