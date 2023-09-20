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
    @Nonnull
    public MessageBuilder trace(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder debug(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder info(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder warning(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder error(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder success(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder failure(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder strong(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder mojo(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder project(Object o) {
        return a(o);
    }

    @Override
    @Nonnull
    public MessageBuilder a(char[] chars, int i, int i1) {
        buffer.append(chars, i, i1);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(char[] chars) {
        buffer.append(chars);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(CharSequence charSequence, int i, int i1) {
        buffer.append(charSequence, i, i1);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(CharSequence charSequence) {
        buffer.append(charSequence);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(Object o) {
        buffer.append(o);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder newline() {
        buffer.append(System.getProperty("line.separator"));
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder format(String s, Object... objects) {
        buffer.append(String.format(s, objects));
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
