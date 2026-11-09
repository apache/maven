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
package org.apache.maven.message.internal;

import javax.inject.Inject;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@PlexusTest
class DefaultMessageBuilderFactoryTest {

    @Inject
    MessageBuilderFactory factory;

    @Test
    void theApiFactoryIsBound() {
        assertInstanceOf(DefaultMessageBuilderFactory.class, factory);
        assertFalse(factory.isColorEnabled());
        assertEquals(-1, factory.getTerminalWidth());
        assertEquals(
                "strong ok 42",
                factory.builder()
                        .strong("strong")
                        .a(" ")
                        .success("ok")
                        .a(' ')
                        .a(42)
                        .build());
        assertEquals("", factory.builder(16).a("x").setLength(0).build());
    }
}
