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
package org.apache.maven.api.plugin.descriptor.another;

import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that subclasses from generated model classes are possible.
 */
class ExtendedPluginDescriptorTest {

    /**
     * A subclass of the generated class {@link PluginDescriptor} that adds an additional field.
     */
    static class ExtendedPluginDescriptor extends PluginDescriptor {

        private final String additionalField;

        ExtendedPluginDescriptor(Builder builder) {
            super(builder);
            this.additionalField = builder.additionalField;
        }

        public String getAdditionalField() {
            return additionalField;
        }

        static class Builder extends PluginDescriptor.Builder {
            protected String additionalField;

            Builder() {
                super(false);
            }

            public Builder additionalField(String additionalField) {
                this.additionalField = additionalField;
                return this;
            }

            public ExtendedPluginDescriptor build() {
                return new ExtendedPluginDescriptor(this);
            }
        }
    }

    @Test
    void testExtendedPluginDescriptor() {
        ExtendedPluginDescriptor.Builder builder = new ExtendedPluginDescriptor.Builder();
        // make sure to call the subclasses' builder methods first, otherwise fluent API would not work
        builder.additionalField("additional")
                .groupId("org.apache.maven")
                .artifactId("maven-plugin-api")
                .version("1.0.0");
        ExtendedPluginDescriptor descriptor = builder.build();
        assertEquals("additional", descriptor.getAdditionalField());
        assertEquals("org.apache.maven", descriptor.getGroupId());
    }
}
