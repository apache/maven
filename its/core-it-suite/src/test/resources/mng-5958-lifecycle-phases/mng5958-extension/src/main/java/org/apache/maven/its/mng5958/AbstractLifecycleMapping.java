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
package org.apache.maven.its.mng5958;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;

/**
 * @author Anton Tanasenko
 */
public abstract class AbstractLifecycleMapping implements LifecycleMapping {

    private Map<String, Lifecycle> lifecycleMap;

    public Map<String, Lifecycle> getLifecycles() {
        if (lifecycleMap != null) {
            return lifecycleMap;
        }

        lifecycleMap = new LinkedHashMap<>();
        Lifecycle lifecycle = new Lifecycle();

        lifecycle.setId("default");
        lifecycle.setPhases(initPhases());

        lifecycleMap.put("default", lifecycle);
        return lifecycleMap;
    }

    public Map<String, String> getPhases(String lifecycle) {
        Lifecycle lifecycleMapping = getLifecycles().get(lifecycle);
        if (lifecycleMapping != null) {
            return lifecycleMapping.getPhases();
        }
        return null;
    }

    public List<String> getOptionalMojos(String lifecycle) {
        return null;
    }

    // raw map on purpose
    protected abstract Map initPhases();
}
