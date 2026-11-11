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

import java.util.function.Function;

import org.apache.maven.api.Constants;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.di.impl.BindingStyler;

/**
 * Adapts Maven's message styles for native DI diagnostics.
 *
 * @since 4.1
 */
public final class MessageBuilderBindingStyler implements BindingStyler {
    private final MessageBuilderFactory factory;

    public MessageBuilderBindingStyler(MessageBuilderFactory factory) {
        this.factory = factory;
    }

    @Override
    public String key(String text) {
        return style(text, builder -> builder.project(text));
    }

    @Override
    public String qualifier(String text) {
        return style(text, builder -> builder.warning(text));
    }

    @Override
    public String scope(String text, boolean present) {
        return style(
                text,
                builder ->
                        present ? builder.success(text) : builder.style(Constants.MAVEN_STYLE_TRANSFER_DEFAULT, text));
    }

    @Override
    public String priority(String text, boolean nonZero) {
        return nonZero ? style(text, builder -> builder.warning(text)) : text;
    }

    @Override
    public String optionalDependency(String text) {
        return style(text, builder -> builder.error(text));
    }

    @Override
    public String header(String text) {
        return style(text, builder -> builder.strong(text));
    }

    private String style(String text, Function<MessageBuilder, MessageBuilder> render) {
        return factory.isColorEnabled() ? render.apply(factory.builder()).build() : text;
    }
}
