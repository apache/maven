package org.apache.maven.monitor.event;

import java.util.HashMap;
import java.util.Map;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author jdcasey
 */
public final class MavenEvents
{

    public static final String PHASE_EXECUTION = "phase-execute";
    public static final String MOJO_EXECUTION = "mojo-execute";
    public static final String PROJECT_EXECUTION = "project-execute";

    /** @deprecated Use {@link MavenEvents#MAVEN_EXECUTION} instead. */
    public static final String REACTOR_EXECUTION = "reactor-execute";
    public static final String MAVEN_EXECUTION = "maven-execute";

    public static final String EMBEDDER_LIFECYCLE = "embedder-lifecycle";
    public static final String EMBEDDER_METHOD = "embedder-method";

    public static final Map DEPRECATIONS;

    static
    {
        Map dep = new HashMap();

        dep.put( MAVEN_EXECUTION, REACTOR_EXECUTION );

        DEPRECATIONS = dep;
    }

    public static final String[] ALL_EVENTS = {
        PHASE_EXECUTION,
        MOJO_EXECUTION,
        PROJECT_EXECUTION,
        REACTOR_EXECUTION,
        MAVEN_EXECUTION,
        EMBEDDER_LIFECYCLE,
        EMBEDDER_METHOD
    };

    public static final String[] NO_EVENTS = {};

    private MavenEvents()
    {
    }

}