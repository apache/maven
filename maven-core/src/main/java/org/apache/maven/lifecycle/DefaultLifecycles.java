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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.0
 */
// TODO The configuration for the lifecycle needs to be externalized so that I can use the annotations properly for the
// wiring and reference and external source for the lifecycle configuration.
@Named
@Singleton
public class DefaultLifecycles {
    public static final String[] STANDARD_LIFECYCLES = {"clean", "default", "site", "wrapper"};

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")

    private final Lookup lookup;

    private final LifecycleRegistry registry;

    private Map<String, Lifecycle> customLifecycles;

    private boolean lifecyclesPrinted;

    public DefaultLifecycles() {
        this.lookup = null;
        this.registry = null;
    }

    /**
     * @deprecated Use {@link #DefaultLifecycles(LifecycleRegistry,Lookup)} instead
     */
    @Deprecated
    public DefaultLifecycles(Map<String, Lifecycle> lifecycles, org.codehaus.plexus.logging.Logger logger) {
        this.customLifecycles = lifecycles;
        this.lookup = null;
        this.registry = null;
    }

    @Inject
    public DefaultLifecycles(LifecycleRegistry registry, Lookup lookup) {
        this.lookup = lookup;
        this.registry = registry;
    }

    /**
     * Get lifecycle based on phase
     *
     * @param phase
     * @return
     */
    public Lifecycle get(String phase) {
        return getPhaseToLifecycleMap().get(phase);
    }

    /**
     * We use this to map all phases to the lifecycle that contains it. This is used so that a user can specify the
     * phase they want to execute and we can easily determine what lifecycle we need to run.
     *
     * @return A map of lifecycles, indexed on id
     */
    public Map<String, Lifecycle> getPhaseToLifecycleMap() {
        if (logger.isDebugEnabled() && !lifecyclesPrinted) {
            for (Lifecycle lifecycle : getLifeCycles()) {
                logger.debug("Lifecycle {}", lifecycle);
            }
            lifecyclesPrinted = true;
        }

        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly
        // so that they don't interfere with internally defined lifecycles.

        Map<String, Lifecycle> phaseToLifecycleMap = new HashMap<>();

        for (Lifecycle lifecycle : getLifeCycles()) {
            for (String phase : lifecycle.getPhases()) {
                // The first definition wins.
                Lifecycle original = phaseToLifecycleMap.put(phase, lifecycle);
                if (original != null && logger.isWarnEnabled()) {
                    logger.warn(
                            "Duplicated lifecycle phase {}. Defined in {} but also in {}",
                            phase,
                            original.getId(),
                            lifecycle.getId());
                }
            }
            if (lifecycle.getDelegate() != null) {
                for (org.apache.maven.api.Lifecycle.Alias alias :
                        lifecycle.getDelegate().aliases()) {
                    Lifecycle original = phaseToLifecycleMap.put(alias.v3Phase(), lifecycle);
                    if (original != null && logger.isWarnEnabled()) {
                        logger.warn(
                                "Duplicated lifecycle phase {}. Defined in {} but also in {}",
                                alias.v3Phase(),
                                original.getId(),
                                lifecycle.getId());
                    }
                }
            }
        }

        return phaseToLifecycleMap;
    }

    /**
     * Returns an ordered list of lifecycles
     */
    public List<Lifecycle> getLifeCycles() {
        List<String> lifecycleIds = Arrays.asList(STANDARD_LIFECYCLES);

        Comparator<String> comparator = (l, r) -> {
            int lx = lifecycleIds.indexOf(l);
            int rx = lifecycleIds.indexOf(r);

            if (lx < 0 || rx < 0) {
                return rx - lx;
            } else {
                return lx - rx;
            }
        };

        Map<String, Lifecycle> lifecyclesMap = lookupLifecycles();

        // ensure canonical order of standard lifecycles
        return lifecyclesMap.values().stream()
                .peek(l -> Objects.requireNonNull(l.getId(), "A lifecycle must have an id."))
                .sorted(Comparator.comparing(Lifecycle::getId, comparator))
                .collect(Collectors.toList());
    }

    private Map<String, Lifecycle> lookupLifecycles() {
        // TODO: Remove the following code when maven-compat is gone
        // This code is here to ensure maven-compat's EmptyLifecycleExecutor keeps on working.
        if (lookup == null) {
            return customLifecycles != null ? customLifecycles : new HashMap<>();
        }

        // Lifecycles cannot be cached as extensions might add custom lifecycles later in the execution.
        try {
            return registry != null
                    ? registry.stream().collect(Collectors.toMap(lf -> lf.id(), lf -> new Lifecycle(registry, lf)))
                    : Map.of();
        } catch (LookupException e) {
            throw new IllegalStateException("Unable to lookup lifecycles from the plexus container", e);
        }
    }

    public String getLifecyclePhaseList() {
        return getLifeCycles().stream().flatMap(l -> l.getPhases().stream()).collect(Collectors.joining(", "));
    }
}
