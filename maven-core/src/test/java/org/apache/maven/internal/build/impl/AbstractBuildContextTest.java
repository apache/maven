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
package org.apache.maven.internal.build.impl;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.build.spi.Workspace;
import org.junit.jupiter.api.io.TempDir;

public abstract class AbstractBuildContextTest {
    @TempDir
    public Path temp;

    protected static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable == null) {
            return null;
        }

        List<T> result = new ArrayList<T>();
        for (T t : iterable) {
            result.add(t);
        }
        return result;
    }

    protected TestBuildContext newBuildContext() {
        return newBuildContext(Collections.emptyMap());
    }

    protected TestBuildContext newBuildContext(Map<String, Serializable> config) {
        return new TestBuildContext(temp.resolve("buildstate.ctx"), config);
    }

    protected TestBuildContext newBuildContext(Workspace workspace) {
        return new TestBuildContext(workspace, temp.resolve("buildstate.ctx"), Collections.emptyMap());
    }
}
