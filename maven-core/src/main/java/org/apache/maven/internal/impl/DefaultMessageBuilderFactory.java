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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.eclipse.sisu.Priority;

@Experimental
@Named
@Singleton
@Priority(-1)
public class DefaultMessageBuilderFactory implements MessageBuilderFactory {

    @Inject
    public DefaultMessageBuilderFactory() {}

    @Override
    public boolean isColorEnabled() {
        return false;
    }

    @Override
    public int getTerminalWidth() {
        return -1;
    }

    @Override
    @Nonnull
    public MessageBuilder builder() {
        return new DefaultMessageBuilder();
    }

    @Override
    @Nonnull
    public MessageBuilder builder(int size) {
        return new DefaultMessageBuilder(new StringBuilder(size));
    }
}
