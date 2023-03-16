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
package org.apache.maven.exception;

import org.apache.maven.plugin.AbstractMojoExecutionException;

public final class PluginVersionUpgradeAvailableExceptionHandler
        implements ExceptionHandler { // TODO: extend DefaultExceptionHandler to use getReference and other stuff?
    private final ExceptionHandler defaultExceptionHandler;

    public PluginVersionUpgradeAvailableExceptionHandler(ExceptionHandler defaultExceptionHandler) {
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    public ExceptionSummary handleException(Throwable e) {
        ExceptionSummary summary = defaultExceptionHandler.handleException(e);
        if (e.getCause() instanceof AbstractMojoExecutionException) {
            // TODO: probably needs resolver to check for new versions available.
            ExceptionSummary possibleUpgrade =
                    new ExceptionSummary(e, "Consider upgrading to version XXX", "link to plugin page would be cool");
            summary.getChildren().add(possibleUpgrade);
        }
        return summary;
    }
}
