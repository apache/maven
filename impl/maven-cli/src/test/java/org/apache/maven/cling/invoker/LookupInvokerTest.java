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
package org.apache.maven.cling.invoker;

import org.apache.maven.api.cli.InvokerRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookupInvokerTest {

    private final LookupInvoker<LookupContext> invoker =
            new LookupInvoker<>(ProtoLookup.builder().build(), null) {
                @Override
                protected LookupContext createContext(InvokerRequest invokerRequest) {
                    return new LookupContext(
                            invokerRequest, false, invokerRequest.options().orElse(null));
                }

                @Override
                protected int execute(LookupContext context) {
                    return 0;
                }
            };

    @Test
    void testCalculateDegreeOfConcurrency() {
        int cpus = Runtime.getRuntime().availableProcessors();
        assertEquals(Math.max(1, cpus - 1), invoker.calculateDegreeOfConcurrency("max"));
        assertEquals(Math.max(1, cpus - 1), invoker.calculateDegreeOfConcurrency("MAX"));
        assertEquals(Math.max(1, cpus - 1), invoker.calculateDegreeOfConcurrency("Max"));

        assertEquals((int) (cpus * 2.2), invoker.calculateDegreeOfConcurrency("2.2C"));
        assertEquals(1, invoker.calculateDegreeOfConcurrency("0.0001C"));
        assertEquals(4, invoker.calculateDegreeOfConcurrency("4"));

        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("0"));
        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("-1"));
        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("0C"));
        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("-2.2C"));
        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("1.0"));
        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("2C2"));
        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("CXXX"));
        assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("XXXC"));

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> invoker.calculateDegreeOfConcurrency("invalid"));
        assertTrue(e.getMessage().contains("'max'"), "Error message should mention 'max'");
    }
}
