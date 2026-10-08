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
package org.apache.maven.logging.internal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.maven.api.services.OutputCapabilities.Destination;
import org.apache.maven.api.services.OutputCapabilities.Format;
import org.apache.maven.impl.DefaultOutputCapabilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputCapabilitiesMapTest {
    @Test
    void capturesAllIndependentPropertiesIncludingAbsence() {
        for (Destination destination :
                Arrays.asList(null, Destination.CONSOLE, Destination.FILE, Destination.REDIRECTED)) {
            for (Format format : Arrays.asList(null, Format.HUMAN_READABLE, Format.MACHINE_READABLE)) {
                for (Charset encoding : Arrays.asList(null, StandardCharsets.UTF_8, Charset.forName("latin1"))) {
                    Map<String, String> captured = OutputCapabilitiesData.capture(
                            TestOutputCapabilities.snapshot(destination, encoding, format));
                    assertEquals(destination == null ? null : destination.name(), captured.get("destination"));
                    assertEquals(format == null ? null : format.name(), captured.get("format"));
                    assertEquals(encoding == null ? null : encoding.name(), captured.get("encoding"));
                    assertEquals(
                            (destination == null ? 0 : 1) + (format == null ? 0 : 1) + (encoding == null ? 0 : 1),
                            captured.size());
                }
            }
        }
        assertTrue(
                OutputCapabilitiesData.capture(new DefaultOutputCapabilities()).isEmpty());
    }

    @Test
    void capturedMapsAndTheirViewsAreImmutable() throws Exception {
        TestOutputCapabilities source = new TestOutputCapabilities();
        Map<String, String> empty = source.asMap();
        Map<String, String> captured;
        try (AutoCloseable cleanup = source.install(
                TestOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8, Format.MACHINE_READABLE))) {
            captured = source.asMap();
            assertThrows(UnsupportedOperationException.class, () -> captured.put("destination", "CONSOLE"));
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> captured.entrySet().iterator().next().setValue("changed"));
            for (Collection<?> view : Arrays.asList(captured.keySet(), captured.values(), captured.entrySet())) {
                assertThrows(UnsupportedOperationException.class, view::clear);
                var iterator = view.iterator();
                iterator.next();
                assertThrows(UnsupportedOperationException.class, iterator::remove);
            }
        }
        assertTrue(empty.isEmpty());
        assertTrue(source.asMap().isEmpty());
        assertEquals(Map.of("destination", "FILE", "encoding", "UTF-8", "format", "MACHINE_READABLE"), captured);
    }

    @Test
    void encodingCanBeUsedToCheckTheExactTreeGlyphs() {
        String glyphs = "\u251c\u2514\u2500\u2502";
        var utf8 = TestOutputCapabilities.snapshot(null, StandardCharsets.UTF_8, null);
        var ascii = TestOutputCapabilities.snapshot(null, StandardCharsets.US_ASCII, null);
        assertTrue(utf8.getEncoding().orElseThrow().newEncoder().canEncode(glyphs));
        assertFalse(ascii.getEncoding().orElseThrow().newEncoder().canEncode(glyphs));
        assertTrue(utf8.getDestination().isEmpty());
        assertTrue(utf8.getFormat().isEmpty());
    }
}
