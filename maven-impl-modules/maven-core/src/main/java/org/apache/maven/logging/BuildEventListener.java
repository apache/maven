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
package org.apache.maven.logging;

import org.apache.maven.execution.ExecutionEvent;
import org.eclipse.aether.transfer.TransferEvent;

/**
 * An abstract build event sink.
 */
public interface BuildEventListener {

    void sessionStarted(ExecutionEvent event);

    void projectStarted(String projectId);

    void projectLogMessage(String projectId, String event);

    void projectFinished(String projectId);

    void executionFailure(String projectId, boolean halted, String exception);

    void mojoStarted(ExecutionEvent event);

    void finish(int exitCode) throws Exception;

    void fail(Throwable t) throws Exception;

    void log(String msg);

    void transfer(String projectId, TransferEvent e);
}
