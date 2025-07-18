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
import java.util.Set;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;

/**
 * Abstract base class for upgrade strategies that provides common functionality
 * and reduces code duplication across strategy implementations.
 */
public abstract class AbstractUpgradeStrategy implements UpgradeStrategy {

    /**
     * Template method that handles common logging and error handling.
     * Subclasses implement the actual upgrade logic in doApply().
     */
    @Override
    public final UpgradeResult apply(UpgradeContext context, Map<Path, Document> pomMap) {
        context.info(getDescription());
        context.indent();

        try {
            UpgradeResult result = doApply(context, pomMap);

            // Log summary
            logSummary(context, result);

            return result;
        } catch (Exception e) {
            context.failure("Strategy execution failed: " + e.getMessage());
            return UpgradeResult.failure(pomMap.keySet(), Set.of());
        } finally {
            context.unindent();
        }
    }

    /**
     * Subclasses implement the actual upgrade logic here.
     *
     * @param context the upgrade context
     * @param pomMap map of all POM files in the project
     * @return the result of the upgrade operation
     */
    protected abstract UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap);

    /**
     * Gets the upgrade options from the context.
     *
     * @param context the upgrade context
     * @return the upgrade options
     */
    protected final UpgradeOptions getOptions(UpgradeContext context) {
        return context.options();
    }

    /**
     * Logs a summary of the upgrade results.
     *
     * @param context the upgrade context
     * @param result the upgrade result
     */
    protected void logSummary(UpgradeContext context, UpgradeResult result) {
        context.println();
        context.info(getDescription() + " Summary:");
        context.indent();
        context.info(result.modifiedCount() + " POM(s) modified");
        context.info(result.unmodifiedCount() + " POM(s) needed no changes");
        if (result.errorCount() > 0) {
            context.info(result.errorCount() + " POM(s) had errors");
        }
        context.unindent();
    }
}
