package org.apache.maven.execution;

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

/**
 * Defines events that Maven fires during a build. <strong>Warning:</strong> This interface might be extended in future
 * Maven versions to support further events. Hence it is strongly recommended to derive custom listeners from
 * {@link AbstractExecutionListener} in order to avoid interoperability problems.
 *
 * @author Benjamin Bentmann
 */
public interface ExecutionListener
{

    void projectDiscoveryStarted( ExecutionEvent event );

    void sessionStarted( ExecutionEvent event );

    void sessionEnded( ExecutionEvent event );

    void projectSkipped( ExecutionEvent event );

    void projectStarted( ExecutionEvent event );

    void projectSucceeded( ExecutionEvent event );

    void projectFailed( ExecutionEvent event );

    void mojoSkipped( ExecutionEvent event );

    void mojoStarted( ExecutionEvent event );

    void mojoSucceeded( ExecutionEvent event );

    void mojoFailed( ExecutionEvent event );

    void forkStarted( ExecutionEvent event );

    void forkSucceeded( ExecutionEvent event );

    void forkFailed( ExecutionEvent event );

    void forkedProjectStarted( ExecutionEvent event );

    void forkedProjectSucceeded( ExecutionEvent event );

    void forkedProjectFailed( ExecutionEvent event );

}
