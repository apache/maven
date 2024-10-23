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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.MessageBuilder;

@Experimental
public class DefaultMessageBuilder implements MessageBuilder {

    private final StringBuilder buffer;

    public DefaultMessageBuilder() {
        this(new StringBuilder());
    }

    public DefaultMessageBuilder(StringBuilder buffer) {
        this.buffer = buffer;
    }

    @Override
    public MessageBuilder style(String style) {
        return this;
    }

    @Override
    public MessageBuilder resetStyle() {
        return this;
    }

    @Override
    public MessageBuilder append(CharSequence cs) {
        buffer.append(cs);
        return this;
    }

    @Override
    public MessageBuilder append(CharSequence cs, int start, int end) {
        buffer.append(cs, start, end);
        return this;
    }

    @Override
    public MessageBuilder append(char c) {
        buffer.append(c);
        return this;
    }

    @Override
    public MessageBuilder setLength(int length) {
        buffer.setLength(length);
        return this;
    }

    @Override
    @Nonnull
    public String build() {
        return buffer.toString();
    }

    @Override
    public String toString() {
        return build();
    }
}
