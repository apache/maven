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
package org.apache.maven.internal.message;

import javax.inject.Inject;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
class ApiMessageBuilderFactoryBindingTest {

    @Inject
    MessageBuilderFactory factory;

    @Test
    void theApiFactoryIsBound() {
        assertTrue(factory instanceof ApiMessageBuilderFactory);
        assertEquals(
                "strong ok 42",
                factory.builder()
                        .strong("strong")
                        .a(" ")
                        .success("ok")
                        .a(' ')
                        .a(42)
                        .build());
    }
}
