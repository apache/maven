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
package org.apache.maven.lifecycle.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Describes the required task segment as provided on the maven command line; i.e. "clean jetty:run install"
 *
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold (extracted class only)
 */
public final class TaskSegment {

    // Can be both "LifeCycleTask" (clean/install) and "GoalTask" (org.mortbay.jetty:maven-jetty-plugin:6.1.19:run)

    private final List<Object> tasks;

    private final boolean aggregating;

    public TaskSegment(boolean aggregating) {
        this.aggregating = aggregating;
        tasks = new ArrayList<>();
    }

    public TaskSegment(boolean aggregating, Object... tasks) {
        this.aggregating = aggregating;
        this.tasks = new ArrayList<>(Arrays.asList(tasks));
    }

    @Override
    public String toString() {
        return getTasks().toString();
    }

    public List<Object> getTasks() {
        return tasks;
    }

    public boolean isAggregating() {
        return aggregating;
    }

    // TODO Consider throwing UnsupportedSomething on hashCode/equals
}
