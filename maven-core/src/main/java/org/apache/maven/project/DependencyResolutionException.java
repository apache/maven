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
package org.apache.maven.project;

import java.util.List;

import org.eclipse.aether.graph.Dependency;

/**
 * @author Benjamin Bentmann
 */
public class DependencyResolutionException extends Exception {

    private final transient DependencyResolutionResult result;
    private final transient String detailMessage;

    public DependencyResolutionException(DependencyResolutionResult result, String message, Throwable cause) {
        super(message, cause);
        this.result = result;
        this.detailMessage = prepareDetailMessage(message, result);
    }

    private static String prepareDetailMessage(String message, DependencyResolutionResult result) {
        StringBuilder msg = new StringBuilder(message);
        msg.append(System.lineSeparator());
        for (Dependency dependency : result.getUnresolvedDependencies()) {
            msg.append("dependency: ").append(dependency).append(System.lineSeparator());
            List<Exception> exceptions = result.getResolutionErrors(dependency);
            for (Exception e : exceptions) {
                msg.append("\t").append(e.getMessage()).append(System.lineSeparator());
            }
        }

        for (Exception exception : result.getCollectionErrors()) {
            msg.append(exception.getMessage()).append(System.lineSeparator());
            if (exception.getCause() != null) {
                msg.append("\tCaused by: ")
                        .append(exception.getCause().getMessage())
                        .append(System.lineSeparator());
            }
        }

        return msg.toString();
    }

    public DependencyResolutionResult getResult() {
        return result;
    }

    @Override
    public String getMessage() {
        return detailMessage;
    }
}
