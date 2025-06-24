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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;

/**
 * Strategy interface for different types of upgrade operations.
 * Each strategy handles a specific aspect of the Maven upgrade process.
 */
public interface UpgradeStrategy {

    /**
     * Applies the upgrade strategy to all eligible POMs.
     *
     * @param context the upgrade context
     * @param pomMap map of all POM files in the project
     * @return the result of the upgrade operation
     */
    UpgradeResult apply(UpgradeContext context, Map<Path, Document> pomMap);

    /**
     * Checks if this strategy is applicable given the current options.
     *
     * @param context the upgrade context
     * @return true if this strategy should be applied
     */
    boolean isApplicable(UpgradeContext context);

    /**
     * Helper method to check if a specific option is enabled, considering --all flag and defaults.
     *
     * @param options the upgrade options
     * @param specificOption the specific option to check
     * @param defaultWhenNoOptionsSpecified whether this option should be enabled by default
     * @return true if the option should be enabled
     */
    default boolean isOptionEnabled(
            UpgradeOptions options, Optional<Boolean> specificOption, boolean defaultWhenNoOptionsSpecified) {
        // Handle --all option (overrides individual options)
        boolean useAll = options.all().orElse(false);
        if (useAll) {
            return true;
        }

        // Check specific option
        if (specificOption.isPresent()) {
            return specificOption.get();
        }

        // Apply default behavior when no specific options are provided
        if (defaultWhenNoOptionsSpecified
                && options.infer().isEmpty()
                && options.model().isEmpty()
                && options.plugins().isEmpty()
                && options.model().isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Gets a description of what this strategy does.
     *
     * @return a human-readable description of the strategy
     */
    String getDescription();
}
