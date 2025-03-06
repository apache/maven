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
package org.apache.maven.cling.invoker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.api.cli.CoreExtensions;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.extensions.InputLocation;

public class PrecedenceCoreExtensionSelector<C extends LookupContext> implements CoreExtensionSelector<C> {
    @Override
    public List<CoreExtension> selectCoreExtensions(LookupInvoker<C> invoker, C context) {
        InvokerRequest invokerRequest = context.invokerRequest;
        if (invokerRequest.coreExtensions().isEmpty()
                || invokerRequest.coreExtensions().get().isEmpty()) {
            return List.of();
        }

        return selectCoreExtensions(
                context, context.invokerRequest.coreExtensions().orElseThrow());
    }

    /**
     * Selects extensions to load discovered from various sources. Also reports conflicts.
     */
    protected List<CoreExtension> selectCoreExtensions(C context, List<CoreExtensions> configuredCoreExtensions) {
        context.logger.debug("Configured core extensions:");
        for (CoreExtensions source : configuredCoreExtensions) {
            context.logger.debug("* " + source.source() + ":");
            for (CoreExtension extension : source.coreExtensions()) {
                context.logger.debug("  - " + extension.getId() + " -> " + formatLocation(extension.getLocation("")));
            }
        }

        Map<CoreExtensions.Source, CoreExtensions> coreExtensionsBySource = configuredCoreExtensions.stream()
                .collect(Collectors.toMap(CoreExtensions::source, Function.identity()));
        LinkedHashMap<String, CoreExtension> selectedExtensions = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();
        for (CoreExtensions.Source source : CoreExtensions.Source.values()) {
            CoreExtensions coreExtensions = coreExtensionsBySource.get(source);
            if (coreExtensions != null) {
                for (CoreExtension coreExtension : coreExtensions.coreExtensions()) {
                    String key = coreExtension.getGroupId() + ":" + coreExtension.getArtifactId();
                    CoreExtension conflict = selectedExtensions.putIfAbsent(key, coreExtension);
                    if (conflict != null) {
                        conflicts.add(String.format(
                                "Conflicting extension %s: %s vs %s",
                                key,
                                formatLocation(conflict.getLocation("")),
                                formatLocation(coreExtension.getLocation(""))));
                    }
                }
            }
        }
        if (!conflicts.isEmpty()) {
            context.logger.warn("Found " + conflicts.size() + " extension conflict(s):");
            for (String conflict : conflicts) {
                context.logger.warn("* " + conflict);
            }
            context.logger.warn("");
            context.logger.warn(
                    "Order of core extensions precedence is project > user > installation. Selected extensions are:");
            for (CoreExtension extension : selectedExtensions.values()) {
                context.logger.warn(
                        "* " + extension.getId() + " configured in " + formatLocation(extension.getLocation("")));
            }
        }

        context.logger.debug("Selected core extensions:");
        for (CoreExtension source : selectedExtensions.values()) {
            context.logger.debug("* " + source.getId() + ": " + formatLocation(source.getLocation("")));
        }
        return List.copyOf(selectedExtensions.values());
    }

    protected String formatLocation(InputLocation location) {
        return location.getSource().getLocation() + ":" + location.getLineNumber();
    }
}
