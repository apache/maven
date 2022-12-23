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
package org.apache.maven.internal.impl;

import java.util.Optional;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.xml.Dom;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class DefaultMojoExecution implements MojoExecution {
    private final org.apache.maven.plugin.MojoExecution delegate;

    public DefaultMojoExecution(org.apache.maven.plugin.MojoExecution delegate) {
        this.delegate = delegate;
    }

    public org.apache.maven.plugin.MojoExecution getDelegate() {
        return delegate;
    }

    @Override
    public Plugin getPlugin() {
        return delegate.getPlugin().getDelegate();
    }

    @Override
    public String getExecutionId() {
        return delegate.getExecutionId();
    }

    @Override
    public String getGoal() {
        return delegate.getGoal();
    }

    @Override
    public Optional<Dom> getConfiguration() {
        return Optional.of(delegate.getConfiguration()).map(Xpp3Dom::getDom);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
