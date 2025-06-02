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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;

/**
 * Orchestrates the execution of different upgrade strategies.
 * Determines which strategies to apply based on options and executes them in priority order.
 * The DI container automatically sorts the injected strategies by their @Priority annotations.
 */
@Named
@Singleton
public class StrategyOrchestrator {

    private final List<UpgradeStrategy> strategies;

    @Inject
    public StrategyOrchestrator(List<UpgradeStrategy> strategies) {
        // DI container automatically sorts strategies by priority (highest first)
        this.strategies = strategies;
    }

    /**
     * Executes all applicable strategies for the given context and POM map.
     *
     * @param context the upgrade context
     * @param pomMap map of all POM files in the project
     * @return the overall result of all strategy executions
     */
    public UpgradeResult executeStrategies(UpgradeContext context, Map<Path, Document> pomMap) {
        context.println();
        context.info("Maven Upgrade Tool");
        logUpgradeOptions(context);

        UpgradeResult overallResult = UpgradeResult.empty();
        List<String> executedStrategies = new ArrayList<>();

        // Execute each applicable strategy
        for (UpgradeStrategy strategy : strategies) {
            context.indent();
            if (strategy.isApplicable(context)) {
                context.info("");
                context.action("Executing strategy: " + strategy.getDescription());
                context.indent();
                executedStrategies.add(strategy.getDescription());

                try {
                    UpgradeResult result = strategy.apply(context, pomMap);

                    // Merge results using the smart merge functionality
                    overallResult = overallResult.merge(result);

                    if (result.success()) {
                        context.success("Strategy completed successfully");
                    } else {
                        context.warning("Strategy completed with " + result.errorCount() + " error(s)");
                    }
                } catch (Exception e) {
                    context.failure("Strategy execution failed: " + e.getMessage());
                    // Create a failure result for this strategy and merge it
                    Set<Path> allPoms = pomMap.keySet();
                    UpgradeResult failureResult = UpgradeResult.failure(allPoms, Set.of());
                    overallResult = overallResult.merge(failureResult);
                } finally {
                    context.unindent();
                }
            } else {
                context.detail("Skipping strategy: " + strategy.getDescription() + " (not applicable)");
            }
            context.unindent();
        }

        // Log overall summary
        logOverallSummary(context, overallResult, executedStrategies);

        return overallResult;
    }

    /**
     * Logs the upgrade options that are enabled.
     */
    private void logUpgradeOptions(UpgradeContext context) {
        UpgradeOptions options = context.options();

        context.action("Upgrade options:");
        context.indent();

        if (options.all().orElse(false)) {
            context.detail("--all (enables all upgrade options)");
        } else {
            if (options.modelVersion().isPresent()) {
                context.detail("--model-version " + options.modelVersion().get());
            }
            if (options.model().orElse(false)) {
                context.detail("--model");
            }
            if (options.plugins().orElse(false)) {
                context.detail("--plugins");
            }
            if (options.infer().orElse(false)) {
                context.detail("--infer");
            }

            // Show defaults if no options specified
            if (options.modelVersion().isEmpty()
                    && options.model().isEmpty()
                    && options.plugins().isEmpty()
                    && options.infer().isEmpty()) {
                context.detail("(using defaults: --model --plugins --infer)");
            }
        }

        context.unindent();
    }

    /**
     * Logs the overall summary of all strategy executions.
     */
    private void logOverallSummary(
            UpgradeContext context, UpgradeResult overallResult, List<String> executedStrategies) {

        context.println();
        context.info("Overall Upgrade Summary:");
        context.indent();
        context.info(overallResult.processedCount() + " POM(s) processed");
        context.info(overallResult.modifiedCount() + " POM(s) modified");
        context.info(overallResult.unmodifiedCount() + " POM(s) needed no changes");
        context.info(overallResult.errorCount() + " error(s) encountered");
        context.unindent();

        if (!executedStrategies.isEmpty()) {
            context.println();
            context.info("Executed Strategies:");
            context.indent();
            for (String strategy : executedStrategies) {
                context.detail(strategy);
            }
            context.unindent();
        }

        if (overallResult.modifiedCount() > 0 && overallResult.errorCount() == 0) {
            context.success("All upgrades completed successfully!");
        } else if (overallResult.modifiedCount() > 0 && overallResult.errorCount() > 0) {
            context.warning("Upgrades completed with some errors");
        } else if (overallResult.modifiedCount() == 0 && overallResult.errorCount() == 0) {
            context.success("No upgrades needed - all POMs are up to date");
        } else {
            context.failure("Upgrade process failed");
        }
    }
}
