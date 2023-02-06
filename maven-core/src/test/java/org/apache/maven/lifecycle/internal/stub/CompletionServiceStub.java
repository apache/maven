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
package org.apache.maven.lifecycle.internal.stub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.maven.lifecycle.internal.ProjectSegment;

/**
 * @author Kristian Rosenvold
 */
public class CompletionServiceStub implements CompletionService<ProjectSegment> {
    List<FutureTask<ProjectSegment>> projectBuildFutureTasks =
            Collections.synchronizedList(new ArrayList<FutureTask<ProjectSegment>>());

    final boolean finishImmediately;

    public int size() {
        return projectBuildFutureTasks.size();
    }

    public CompletionServiceStub(boolean finishImmediately) {
        this.finishImmediately = finishImmediately;
    }

    public Future<ProjectSegment> submit(Callable<ProjectSegment> task) {
        FutureTask<ProjectSegment> projectBuildFutureTask = new FutureTask<>(task);
        projectBuildFutureTasks.add(projectBuildFutureTask);
        if (finishImmediately) {
            projectBuildFutureTask.run();
        }
        return projectBuildFutureTask;
    }

    public Future<ProjectSegment> submit(Runnable task, ProjectSegment result) {
        FutureTask<ProjectSegment> projectBuildFutureTask = new FutureTask<>(task, result);
        projectBuildFutureTasks.add(projectBuildFutureTask);
        if (finishImmediately) {
            projectBuildFutureTask.run();
        }
        return projectBuildFutureTask;
    }

    public Future<ProjectSegment> take() throws InterruptedException {
        return null;
    }

    public Future<ProjectSegment> poll() {
        return null;
    }

    public Future<ProjectSegment> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }
}
