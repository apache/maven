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
package org.apache.maven.execution;

/**
 * Provides a skeleton implementation for execution listeners. The methods of this class are empty.
 *
 * @author Benjamin Bentmann
 */
public class AbstractExecutionListener implements ExecutionListener {

    public void projectDiscoveryStarted(ExecutionEvent event) {
        // default does nothing
    }

    public void sessionStarted(ExecutionEvent event) {
        // default does nothing
    }

    public void sessionEnded(ExecutionEvent event) {
        // default does nothing
    }

    public void projectSkipped(ExecutionEvent event) {
        // default does nothing
    }

    public void projectStarted(ExecutionEvent event) {
        // default does nothing
    }

    public void projectSucceeded(ExecutionEvent event) {
        // default does nothing
    }

    public void projectFailed(ExecutionEvent event) {
        // default does nothing
    }

    public void forkStarted(ExecutionEvent event) {
        // default does nothing
    }

    public void forkSucceeded(ExecutionEvent event) {
        // default does nothing
    }

    public void forkFailed(ExecutionEvent event) {
        // default does nothing
    }

    public void mojoSkipped(ExecutionEvent event) {
        // default does nothing
    }

    public void mojoStarted(ExecutionEvent event) {
        // default does nothing
    }

    public void mojoSucceeded(ExecutionEvent event) {
        // default does nothing
    }

    public void mojoFailed(ExecutionEvent event) {
        // default does nothing
    }

    public void forkedProjectStarted(ExecutionEvent event) {
        // default does nothing
    }

    public void forkedProjectSucceeded(ExecutionEvent event) {
        // default does nothing
    }

    public void forkedProjectFailed(ExecutionEvent event) {
        // default does nothing
    }
}
