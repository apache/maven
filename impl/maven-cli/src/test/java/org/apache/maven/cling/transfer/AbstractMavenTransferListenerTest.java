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
package org.apache.maven.cling.transfer;

import java.time.Instant;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractMavenTransferListenerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void bytesPerSecondUsesFractionalSeconds() {
        assertEquals(
                OptionalDouble.of(31_600),
                AbstractMavenTransferListener.bytesPerSecond(START, START.plusMillis(250), 7_900));
    }

    @Test
    void bytesPerSecondIsEmptyWhenNoTimeHasElapsed() {
        assertEquals(OptionalDouble.empty(), AbstractMavenTransferListener.bytesPerSecond(START, START, 7_900));
        assertEquals(
                OptionalDouble.empty(),
                AbstractMavenTransferListener.bytesPerSecond(START, START.minusMillis(1), 7_900));
    }
}
