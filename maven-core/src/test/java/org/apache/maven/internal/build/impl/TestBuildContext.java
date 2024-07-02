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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.build.Status;
import org.apache.maven.api.build.spi.Workspace;

class TestBuildContext extends DefaultBuildContext {

    public TestBuildContext(Path stateFile, Map<String, Serializable> configuration) {
        this(new FilesystemWorkspace(), stateFile, configuration);
    }

    public TestBuildContext(Workspace workspace, Path stateFile, Map<String, Serializable> configuration) {
        super(workspace, stateFile, configuration, null);
    }

    public void commit() throws IOException {
        super.commit(null);
    }

    public Collection<? extends DefaultInputMetadata> getRegisteredInputs() {
        List<DefaultInputMetadata> result = new ArrayList<>();
        for (Path resource : state.getResources().keySet()) {
            result.add(new DefaultInputMetadata(this, state, resource));
        }
        for (Path resource : oldState.getResources().keySet()) {
            if (!state.isResource(resource)) {
                result.add(new DefaultInputMetadata(this, oldState, resource));
            }
        }
        return result;
    }

    @Override
    public Set<Path> getProcessedResources() {
        return super.getProcessedResources();
    }

    public Status getResourceStatus(Path resource) {
        return super.getResourceStatus(resource);
    }

    public Collection<? extends DefaultOutputMetadata> getAssociatedOutputs(DefaultInputMetadata metadata) {
        Path resource = metadata.getPath();
        return super.getAssociatedOutputs(getState(resource), resource);
    }

    public <T extends Serializable> Serializable setAttribute(DefaultResource resource, String key, T value) {
        return super.setResourceAttribute(resource.getPath(), key, value);
    }

    public <V extends Serializable> V getAttribute(DefaultMetadata<?> resource, String key, Class<V> clazz) {
        return super.getResourceAttribute(getState(resource.getPath()), resource.getPath(), key, clazz);
    }

    public <V extends Serializable> V getAttribute(DefaultResource resource, String key, Class<V> clazz) {
        return super.getResourceAttribute(getState(resource.getPath()), resource.getPath(), key, clazz);
    }

    protected DefaultBuildContextState getState(Path source) {
        return isProcessedResource(source) ? this.state : this.oldState;
    }
}
