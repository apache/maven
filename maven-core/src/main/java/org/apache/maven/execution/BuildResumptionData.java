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

import java.util.List;

/**
 * This class holds the information required to enable resuming a Maven build with {@code --resume}.
 */
public class BuildResumptionData
{
    /**
     * The list of projects that remain to be built.
     */
    private final List<String> remainingProjects;

    public BuildResumptionData ( final List<String> remainingProjects )
    {
        this.remainingProjects = remainingProjects;
    }

    /**
     * Returns the projects that still need to be built when resuming.
     * @return A list containing the group and artifact id of the projects.
     */
    public List<String> getRemainingProjects()
    {
        return this.remainingProjects;
    }

}
