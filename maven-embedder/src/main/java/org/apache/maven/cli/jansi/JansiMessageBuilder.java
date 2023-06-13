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
package org.apache.maven.cli.jansi;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.MessageBuilder;
import org.fusesource.jansi.Ansi;

@Experimental
public class JansiMessageBuilder implements MessageBuilder {
    private final Ansi ansi;

    public JansiMessageBuilder() {
        this.ansi = Ansi.ansi();
    }

    public JansiMessageBuilder(StringBuilder sb) {
        this.ansi = Ansi.ansi(sb);
    }

    @Override
    @Nonnull
    public MessageBuilder debug(Object o) {
        return style(Style.DEBUG, o);
    }

    @Override
    @Nonnull
    public MessageBuilder info(Object o) {
        return style(Style.INFO, o);
    }

    @Override
    @Nonnull
    public MessageBuilder warning(Object o) {
        return style(Style.WARNING, o);
    }

    @Override
    @Nonnull
    public MessageBuilder error(Object o) {
        return style(Style.ERROR, o);
    }

    @Override
    @Nonnull
    public MessageBuilder success(Object o) {
        return style(Style.SUCCESS, o);
    }

    @Override
    @Nonnull
    public MessageBuilder failure(Object o) {
        return style(Style.FAILURE, o);
    }

    @Override
    @Nonnull
    public MessageBuilder strong(Object o) {
        return style(Style.STRONG, o);
    }

    @Override
    @Nonnull
    public MessageBuilder mojo(Object o) {
        return style(Style.MOJO, o);
    }

    @Override
    @Nonnull
    public MessageBuilder project(Object o) {
        return style(Style.PROJECT, o);
    }

    private MessageBuilder style(Style style, Object o) {
        style.apply(ansi).a(o).reset();
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(char[] chars, int i, int i1) {
        ansi.a(chars, i, i1);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(char[] chars) {
        ansi.a(chars);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(CharSequence charSequence, int i, int i1) {
        ansi.a(charSequence, i, i1);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(CharSequence charSequence) {
        ansi.a(charSequence);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a(Object o) {
        ansi.a(o);
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder newline() {
        ansi.newline();
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder format(String s, Object... objects) {
        ansi.format(s, objects);
        return this;
    }

    @Override
    @Nonnull
    public String build() {
        return ansi.toString();
    }

    @Override
    public String toString() {
        return build();
    }
}
