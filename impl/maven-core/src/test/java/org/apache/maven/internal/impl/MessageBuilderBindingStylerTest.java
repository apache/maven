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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.Constants;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.impl.DefaultMessageBuilder;
import org.apache.maven.impl.DefaultMessageBuilderFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageBuilderBindingStylerTest {
    @Test
    void disabledColorsReturnPlainTextWithoutCreatingBuilders() {
        MessageBuilderBindingStyler styler = new MessageBuilderBindingStyler(new DefaultMessageBuilderFactory() {
            @Override
            public MessageBuilder builder() {
                throw new AssertionError("styling attempted with colors disabled");
            }
        });
        assertEquals("type", styler.key("type"));
        assertEquals("qualifier", styler.qualifier("qualifier"));
        assertEquals("scope", styler.scope("scope", true));
        assertEquals("none", styler.scope("none", false));
        assertEquals("42", styler.priority("42", true));
        assertEquals("0", styler.priority("0", false));
        assertEquals("?", styler.optionalDependency("?"));
        assertEquals("header", styler.header("header"));
    }

    @Test
    void enabledColorsUseExistingMavenStyles() {
        List<String> styles = new ArrayList<>();
        MessageBuilderBindingStyler styler = new MessageBuilderBindingStyler(new DefaultMessageBuilderFactory() {
            @Override
            public boolean isColorEnabled() {
                return true;
            }

            @Override
            public MessageBuilder builder() {
                return new DefaultMessageBuilder() {
                    @Override
                    public MessageBuilder style(String style) {
                        styles.add(style);
                        return this;
                    }
                };
            }
        });
        assertEquals("type", styler.key("type"));
        assertEquals("qualifier", styler.qualifier("qualifier"));
        assertEquals("scope", styler.scope("scope", true));
        assertEquals("none", styler.scope("none", false));
        assertEquals("42", styler.priority("42", true));
        assertEquals("?", styler.optionalDependency("?"));
        assertEquals("header", styler.header("header"));
        assertEquals("0", styler.priority("0", false));
        assertEquals(
                List.of(
                        "." + Constants.MAVEN_STYLE_PROJECT_NAME + ":-" + Constants.MAVEN_STYLE_PROJECT_DEFAULT,
                        "." + Constants.MAVEN_STYLE_WARNING_NAME + ":-" + Constants.MAVEN_STYLE_WARNING_DEFAULT,
                        "." + Constants.MAVEN_STYLE_SUCCESS_NAME + ":-" + Constants.MAVEN_STYLE_SUCCESS_DEFAULT,
                        Constants.MAVEN_STYLE_TRANSFER_DEFAULT,
                        "." + Constants.MAVEN_STYLE_WARNING_NAME + ":-" + Constants.MAVEN_STYLE_WARNING_DEFAULT,
                        "." + Constants.MAVEN_STYLE_ERROR_NAME + ":-" + Constants.MAVEN_STYLE_ERROR_DEFAULT,
                        "." + Constants.MAVEN_STYLE_STRONG_NAME + ":-" + Constants.MAVEN_STYLE_STRONG_DEFAULT),
                styles);
    }
}
