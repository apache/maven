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
package org.apache.maven.lifecycle.internal.stub;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;

import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.CLEAN;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.COMPILE;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.INITIALIZE;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.INSTALL;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.PACKAGE;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.POST_CLEAN;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.POST_SITE;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.PRE_CLEAN;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.PRE_SITE;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.PROCESS_RESOURCES;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.PROCESS_TEST_RESOURCES;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.SITE;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.SITE_DEPLOY;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.TEST;
import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.VALIDATE;

/**
 * @author Kristian Rosenvold
 */
public class DefaultLifecyclesStub {
    public static DefaultLifecycles createDefaultLifecycles() {

        List<String> stubDefaultCycle = Arrays.asList(
                VALIDATE.getPhase(),
                INITIALIZE.getPhase(),
                PROCESS_RESOURCES.getPhase(),
                COMPILE.getPhase(),
                TEST.getPhase(),
                PROCESS_TEST_RESOURCES.getPhase(),
                PACKAGE.getPhase(),
                "BEER",
                INSTALL.getPhase());

        // The two phases below are really for future expansion, some would say they lack a drink
        // The point being that they do not really have to match the "real" stuff,
        List<String> stubCleanCycle = Arrays.asList(PRE_CLEAN.getPhase(), CLEAN.getPhase(), POST_CLEAN.getPhase());

        List<String> stubSiteCycle =
                Arrays.asList(PRE_SITE.getPhase(), SITE.getPhase(), POST_SITE.getPhase(), SITE_DEPLOY.getPhase());

        Iterator<List<String>> lcs =
                Arrays.asList(stubDefaultCycle, stubCleanCycle, stubSiteCycle).iterator();

        Map<String, Lifecycle> lifeCycles = new HashMap<>();
        for (String s : DefaultLifecycles.STANDARD_LIFECYCLES) {
            final Lifecycle lifecycle = new Lifecycle(s, lcs.next(), null);
            lifeCycles.put(s, lifecycle);
        }
        return new DefaultLifecycles(lifeCycles, new LoggerStub());
    }
}
