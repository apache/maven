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
package org.apache.maven.jline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.Constants;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

@Experimental
@Named
@Singleton
@Priority(10)
public class JLineMessageBuilderFactory implements MessageBuilderFactory {
    private final StyleResolver resolver;

    public JLineMessageBuilderFactory() {
        this.resolver = new MavenStyleResolver();
    }

    @Override
    public boolean isColorEnabled() {
        return MessageUtils.isColorEnabled();
    }

    @Override
    public int getTerminalWidth() {
        return MessageUtils.getTerminalWidth();
    }

    @Override
    public MessageBuilder builder() {
        return builder(64);
    }

    @Override
    public MessageBuilder builder(int size) {
        return new JlineMessageBuilder(resolver, size);
    }

    private static class JlineMessageBuilder implements MessageBuilder {
        private final StyleResolver styleResolver;
        private final AttributedStringBuilder builder;

        private JlineMessageBuilder(StyleResolver styleResolver, int size) {
            this.styleResolver = styleResolver;
            this.builder = new AttributedStringBuilder(size);
        }

        @Override
        public MessageBuilder style(String style) {
            if (MessageUtils.isColorEnabled()) {
                builder.style(styleResolver.resolve(style));
            }
            return this;
        }

        @Override
        public MessageBuilder resetStyle() {
            builder.style(AttributedStyle.DEFAULT);
            return this;
        }

        @Override
        public MessageBuilder append(CharSequence cs) {
            builder.append(cs);
            return this;
        }

        @Override
        public MessageBuilder append(CharSequence cs, int start, int end) {
            builder.append(cs, start, end);
            return this;
        }

        @Override
        public MessageBuilder append(char c) {
            builder.append(c);
            return this;
        }

        @Override
        public MessageBuilder setLength(int length) {
            builder.setLength(length);
            return this;
        }

        @Override
        public String build() {
            return builder.toAnsi(MessageUtils.terminal);
        }

        @Override
        public String toString() {
            return build();
        }
    }

    private static class MavenStyleResolver extends StyleResolver {
        private final Map<String, AttributedStyle> styles = new ConcurrentHashMap<>();

        private MavenStyleResolver() {
            super(key -> {
                String v = System.getProperty(Constants.MAVEN_STYLE_PREFIX + key);
                if (v == null) {
                    v = System.getProperty("style." + key);
                }
                return v;
            });
        }

        @Override
        public AttributedStyle resolve(String spec) {
            return styles.computeIfAbsent(spec, this::doResolve);
        }

        @Override
        public AttributedStyle resolve(String spec, String defaultSpec) {
            return resolve(defaultSpec != null ? spec + ":-" + defaultSpec : spec);
        }

        private AttributedStyle doResolve(String spec) {
            String def = null;
            int i = spec.indexOf(":-");
            if (i != -1) {
                String[] parts = spec.split(":-");
                spec = parts[0].trim();
                def = parts[1].trim();
            }
            return super.resolve(spec, def);
        }
    }
}
