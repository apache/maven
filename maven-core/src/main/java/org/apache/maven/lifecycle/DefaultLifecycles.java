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
package org.apache.maven.lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * @since 3.0
 * @author Jason van Zyl
 * @author Kristian Rosenvold
 */
// TODO The configuration for the lifecycle needs to be externalized so that I can use the annotations properly for the
// wiring and reference and external source for the lifecycle configuration.
@Component(role = DefaultLifecycles.class)
public class DefaultLifecycles {
    public static final String[] STANDARD_LIFECYCLES = {"clean", "default", "site"};

    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")

    @Requirement(role = Lifecycle.class)
    private Map<String, Lifecycle> lifecycles;

    @Requirement
    private Logger logger;

    public DefaultLifecycles() {}

    public DefaultLifecycles(Map<String, Lifecycle> lifecycles, Logger logger) {
        this.lifecycles = new LinkedHashMap<>();
        this.logger = logger;
        this.lifecycles = lifecycles;
    }

    public Lifecycle get(String key) {
        return getPhaseToLifecycleMap().get(key);
    }

    /**
     * We use this to map all phases to the lifecycle that contains it. This is used so that a user can specify the
     * phase they want to execute and we can easily determine what lifecycle we need to run.
     *
     * @return A map of lifecycles, indexed on id
     */
    public Map<String, Lifecycle> getPhaseToLifecycleMap() {
        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly
        // so that they don't interfere with internally defined lifecycles.

        HashMap<String, Lifecycle> phaseToLifecycleMap = new HashMap<>();

        for (Lifecycle lifecycle : getLifeCycles()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Lifecycle " + lifecycle);
            }

            for (String phase : lifecycle.getPhases()) {
                // The first definition wins.
                if (!phaseToLifecycleMap.containsKey(phase)) {
                    phaseToLifecycleMap.put(phase, lifecycle);
                } else {
                    Lifecycle original = phaseToLifecycleMap.get(phase);
                    logger.warn("Duplicated lifecycle phase " + phase + ". Defined in " + original.getId()
                            + " but also in " + lifecycle.getId());
                }
            }
        }

        return phaseToLifecycleMap;
    }

    /**
     * Returns an ordered list of lifecycles
     */
    public List<Lifecycle> getLifeCycles() {
        // ensure canonical order of standard lifecycles
        Map<String, Lifecycle> lifecycles = new LinkedHashMap<>(this.lifecycles);

        LinkedHashSet<String> lifecycleNames = new LinkedHashSet<>(Arrays.asList(STANDARD_LIFECYCLES));
        lifecycleNames.addAll(lifecycles.keySet());

        ArrayList<Lifecycle> result = new ArrayList<>();
        for (String name : lifecycleNames) {
            Lifecycle lifecycle = lifecycles.get(name);
            if (lifecycle.getId() == null) {
                throw new NullPointerException("A lifecycle must have an id.");
            }
            result.add(lifecycle);
        }

        return result;
    }

    public String getLifecyclePhaseList() {
        Set<String> phases = new LinkedHashSet<>();

        for (Lifecycle lifecycle : getLifeCycles()) {
            phases.addAll(lifecycle.getPhases());
        }

        return StringUtils.join(phases.iterator(), ", ");
    }
}
