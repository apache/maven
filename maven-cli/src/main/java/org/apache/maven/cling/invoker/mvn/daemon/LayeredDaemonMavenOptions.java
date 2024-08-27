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
package org.apache.maven.cling.invoker.mvn.daemon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.cli.mvn.daemon.DaemonMavenOptions;
import org.apache.maven.cling.invoker.mvn.LayeredMavenOptions;

public class LayeredDaemonMavenOptions extends LayeredMavenOptions<DaemonMavenOptions> implements DaemonMavenOptions {
    public static DaemonMavenOptions layerDaemonMavenOptions(Collection<DaemonMavenOptions> options) {
        List<DaemonMavenOptions> o = options.stream().filter(Objects::nonNull).toList();
        if (o.isEmpty()) {
            throw new IllegalArgumentException("No options specified (or all were null)");
        } else if (o.size() == 1) {
            return o.get(0);
        } else {
            return new LayeredDaemonMavenOptions(o);
        }
    }

    private LayeredDaemonMavenOptions(List<DaemonMavenOptions> options) {
        super(options);
    }

    @Override
    public Optional<Boolean> rawStreams() {
        return Optional.empty();
    }

    @Override
    public DaemonMavenOptions interpolate(Collection<Map<String, String>> properties) {
        ArrayList<DaemonMavenOptions> interpolatedOptions = new ArrayList<>(options.size());
        for (DaemonMavenOptions o : options) {
            interpolatedOptions.add(o.interpolate(properties));
        }
        return layerDaemonMavenOptions(interpolatedOptions);
    }
}
