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
import java.util.List;
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

/**
 * domtrip-based orchestrator for executing different upgrade strategies.
 *
 * <p>This class provides the same functionality as StrategyOrchestrator but works
 * with domtrip-based strategies for superior formatting preservation.
 *
 * <p>Determines which strategies to apply based on options and executes them in priority order.
 * The DI container automatically sorts the injected strategies by their @Priority annotations.
 */
@Named("strategy-orchestrator")
@Singleton
public class StrategyOrchestrator {

    private final List<UpgradeStrategy> strategies;

    @Inject
    public StrategyOrchestrator(List<UpgradeStrategy> strategies) {
        // DI container automatically sorts strategies by priority (highest first)
        this.strategies = strategies;
    }

    /**
     * Executes all applicable upgrade strategies in priority order.
     * Each strategy is checked for applicability before execution.
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

        // Filter and execute applicable strategies
        List<UpgradeStrategy> applicableStrategies = strategies.stream()
                .filter(strategy -> strategy.isApplicable(context))
                .toList();

        if (applicableStrategies.isEmpty()) {
            context.warning("No applicable upgrade strategies found");
            return overallResult;
        }

        context.info("Executing " + applicableStrategies.size() + " upgrade strategy(ies):");
        for (UpgradeStrategy strategy : applicableStrategies) {
            context.info("  - " + strategy.getDescription());
        }
        context.println();

        // Execute each applicable strategy
        for (UpgradeStrategy strategy : applicableStrategies) {
            context.info("=== " + strategy.getDescription() + " ===");
            context.indent();

            try {
                UpgradeResult strategyResult = strategy.apply(context, pomMap);
                overallResult = overallResult.merge(strategyResult);

                // Log strategy results
                logStrategyResult(context, strategy, strategyResult);

            } catch (Exception e) {
                context.failure("Strategy failed: " + e.getMessage());
                // Mark all POMs as having errors for this strategy
                UpgradeResult errorResult = new UpgradeResult(pomMap.keySet(), java.util.Set.of(), pomMap.keySet());
                overallResult = overallResult.merge(errorResult);
            } finally {
                context.unindent();
                context.println();
            }
        }

        // Log overall results
        logOverallResult(context, overallResult);

        return overallResult;
    }

    /**
     * Logs the upgrade options being used.
     */
    private void logUpgradeOptions(UpgradeContext context) {
        UpgradeOptions options = context.options();

        context.info("Options:");
        context.indent();

        if (options.all().orElse(false)) {
            context.info("all: true (applying all available upgrades)");
        }

        if (options.modelVersion().isPresent()) {
            context.info("modelVersion: " + options.modelVersion().get());
        }

        if (options.plugins().orElse(false)) {
            context.info("plugins: true");
        }

        if (options.plugins().orElse(false)) {
            context.info("plugins: true");
        }

        if (options.directory().isPresent()) {
            context.info("directory: " + options.directory().get());
        }

        context.unindent();
        context.println();
    }

    /**
     * Logs the result of a single strategy execution.
     */
    private void logStrategyResult(UpgradeContext context, UpgradeStrategy strategy, UpgradeResult result) {
        if (!result.errorPoms().isEmpty()) {
            context.failure("Strategy completed with errors");
            context.indent();
            context.info("Processed: " + result.processedPoms().size() + " POMs");
            context.info("Modified: " + result.modifiedPoms().size() + " POMs");
            context.failure("Errors: " + result.errorPoms().size() + " POMs");
            context.unindent();
        } else if (!result.modifiedPoms().isEmpty()) {
            context.success("Strategy completed successfully");
            context.indent();
            context.info("Processed: " + result.processedPoms().size() + " POMs");
            context.success("Modified: " + result.modifiedPoms().size() + " POMs");
            context.unindent();
        } else {
            context.info("Strategy completed (no changes needed)");
            context.indent();
            context.info("Processed: " + result.processedPoms().size() + " POMs");
            context.unindent();
        }
    }

    /**
     * Logs the overall result of all strategy executions.
     */
    private void logOverallResult(UpgradeContext context, UpgradeResult overallResult) {
        context.info("=== Overall Results ===");
        context.indent();

        context.info("Total POMs processed: " + overallResult.processedPoms().size());

        if (!overallResult.modifiedPoms().isEmpty()) {
            context.success(
                    "Total POMs modified: " + overallResult.modifiedPoms().size());
        } else {
            context.info("No POMs required modifications");
        }

        if (!overallResult.errorPoms().isEmpty()) {
            context.failure(
                    "Total POMs with errors: " + overallResult.errorPoms().size());
        }

        context.unindent();
    }
}
