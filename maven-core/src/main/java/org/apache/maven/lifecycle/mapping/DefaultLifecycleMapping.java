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
package org.apache.maven.lifecycle.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * DefaultLifecycleMapping
 */
public class DefaultLifecycleMapping implements LifecycleMapping {

    private List<Lifecycle> lifecycles;

    private Map<String, Lifecycle> lifecycleMap;

    /** @deprecated use lifecycles instead */
    @Deprecated
    private Map<String, LifecyclePhase> phases;

    /**
     * Default ctor for plexus compatibility: lifecycles are most commonly defined in Plexus XML, that does field
     * injection. Still, for Plexus to be able to instantiate this class, default ctor is needed.
     *
     * @deprecated Should not be used in Java code.
     */
    @Deprecated
    public DefaultLifecycleMapping() {}

    /**
     * Ctor to be used in Java code/providers.
     */
    public DefaultLifecycleMapping(final List<Lifecycle> lifecycles) {
        this.lifecycleMap =
                Collections.unmodifiableMap(lifecycles.stream().collect(toMap(Lifecycle::getId, identity())));
    }

    /**
     * Plexus: Populates the lifecycle map from the injected list of lifecycle mappings (if not already done).
     */
    private void initLifecycleMap() {
        if (lifecycleMap == null) {
            lifecycleMap = new HashMap<>();

            if (lifecycles != null) {
                for (Lifecycle lifecycle : lifecycles) {
                    lifecycleMap.put(lifecycle.getId(), lifecycle);
                }
            } else {
                /*
                 * NOTE: This is to provide a migration path for implementors of the legacy API which did not know about
                 * getLifecycles().
                 */

                String[] lifecycleIds = {"default", "clean", "site"};

                for (String lifecycleId : lifecycleIds) {
                    Map<String, LifecyclePhase> phases = getLifecyclePhases(lifecycleId);
                    if (phases != null) {
                        Lifecycle lifecycle = new Lifecycle();

                        lifecycle.setId(lifecycleId);
                        lifecycle.setLifecyclePhases(phases);

                        lifecycleMap.put(lifecycleId, lifecycle);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Lifecycle> getLifecycles() {
        initLifecycleMap();

        return lifecycleMap;
    }

    @Deprecated
    @Override
    public List<String> getOptionalMojos(String lifecycle) {
        return null;
    }

    private Map<String, LifecyclePhase> getLifecyclePhases(String lifecycle) {
        initLifecycleMap();

        Lifecycle lifecycleMapping = lifecycleMap.get(lifecycle);

        if (lifecycleMapping != null) {
            return lifecycleMapping.getLifecyclePhases();
        } else if ("default".equals(lifecycle)) {
            return phases;
        } else {
            return null;
        }
    }

    @Deprecated
    public Map<String, String> getPhases(String lifecycle) {
        return LifecyclePhase.toLegacyMap(getLifecyclePhases(lifecycle));
    }
}
