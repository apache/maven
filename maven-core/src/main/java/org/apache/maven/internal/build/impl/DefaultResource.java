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

import java.nio.file.Path;
import java.util.Objects;

import org.apache.maven.api.build.Resource;
import org.apache.maven.api.build.Severity;
import org.apache.maven.api.build.Status;

public abstract class DefaultResource implements Resource {
    protected final DefaultBuildContext context;
    protected final DefaultBuildContextState state;
    protected final Path resource;

    public DefaultResource(DefaultBuildContext context, DefaultBuildContextState state, Path resource) {
        this.context = context;
        this.state = state;
        this.resource = resource;
    }

    @Override
    public Path getPath() {
        return resource;
    }

    @Override
    public Status getStatus() {
        return context.getResourceStatus(resource);
    }

    @Override
    public void addMessage(int line, int column, String message, Severity severity, Throwable cause) {
        context.addMessage(getPath(), line, column, message, severity, cause);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultMetadata<?> that = (DefaultMetadata<?>) o;
        return context == that.context && state == that.state && resource.equals(that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource);
    }

    @Override
    public String toString() {
        return resource.toString();
    }
}
