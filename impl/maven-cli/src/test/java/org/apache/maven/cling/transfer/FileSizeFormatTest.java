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

import java.util.Locale;
import java.util.stream.Stream;

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.cling.transfer.FileSizeFormat.ScaleUnit;
import org.apache.maven.impl.DefaultMessageBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSizeFormatTest {
    static Locale original;

    @BeforeAll
    static void beforeAll() {
        original = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(original);
    }

    @Test
    void negativeSize() {
        FileSizeFormat format = new FileSizeFormat();
        assertThrows(IllegalArgumentException.class, () -> format.format(-100L));
    }

    static Stream<Arguments> sizeTestData() {
        return Stream.of(
                Arguments.of(0L, "0 B"),
                Arguments.of(5L, "5 B"),
                Arguments.of(10L, "10 B"),
                Arguments.of(15L, "15 B"),
                Arguments.of(999L, "999 B"),
                Arguments.of(1000L, "1.0 kB"),
                Arguments.of(5500L, "5.5 kB"),
                Arguments.of(10L * 1000L, "10 kB"),
                Arguments.of(15L * 1000L, "15 kB"),
                Arguments.of(999L * 1000L, "999 kB"),
                Arguments.of(1000L * 1000L, "1.0 MB"),
                Arguments.of(5500L * 1000L, "5.5 MB"),
                Arguments.of(10L * 1000L * 1000L, "10 MB"),
                Arguments.of(15L * 1000L * 1000L, "15 MB"),
                Arguments.of(999L * 1000L * 1000L, "999 MB"),
                Arguments.of(1000L * 1000L * 1000L, "1.0 GB"),
                Arguments.of(5500L * 1000L * 1000L, "5.5 GB"),
                Arguments.of(10L * 1000L * 1000L * 1000L, "10 GB"),
                Arguments.of(15L * 1000L * 1000L * 1000L, "15 GB"),
                Arguments.of(1000L * 1000L * 1000L * 1000L, "1000 GB"));
    }

    @ParameterizedTest
    @MethodSource("sizeTestData")
    void size(long input, String expected) {
        FileSizeFormat format = new FileSizeFormat();
        assertEquals(expected, format.format(input));
    }

    static Stream<Arguments> sizeWithScaleUnitTestData() {
        return Stream.of(
                // 0 bytes
                Arguments.of(0L, null, "0 B"),
                Arguments.of(0L, ScaleUnit.BYTE, "0 B"),
                Arguments.of(0L, ScaleUnit.KILOBYTE, "0 kB"),
                Arguments.of(0L, ScaleUnit.MEGABYTE, "0 MB"),
                Arguments.of(0L, ScaleUnit.GIGABYTE, "0 GB"),

                // 5 bytes
                Arguments.of(5L, null, "5 B"),
                Arguments.of(5L, ScaleUnit.BYTE, "5 B"),
                Arguments.of(5L, ScaleUnit.KILOBYTE, "0 kB"),
                Arguments.of(5L, ScaleUnit.MEGABYTE, "0 MB"),
                Arguments.of(5L, ScaleUnit.GIGABYTE, "0 GB"),

                // 49 bytes
                Arguments.of(49L, null, "49 B"),
                Arguments.of(49L, ScaleUnit.BYTE, "49 B"),
                Arguments.of(49L, ScaleUnit.KILOBYTE, "0 kB"),
                Arguments.of(49L, ScaleUnit.MEGABYTE, "0 MB"),
                Arguments.of(49L, ScaleUnit.GIGABYTE, "0 GB"),

                // 50 bytes
                Arguments.of(50L, null, "50 B"),
                Arguments.of(50L, ScaleUnit.BYTE, "50 B"),
                Arguments.of(50L, ScaleUnit.KILOBYTE, "0.1 kB"),
                Arguments.of(50L, ScaleUnit.MEGABYTE, "0 MB"),
                Arguments.of(50L, ScaleUnit.GIGABYTE, "0 GB"),

                // 999 bytes
                Arguments.of(999L, null, "999 B"),
                Arguments.of(999L, ScaleUnit.BYTE, "999 B"),
                Arguments.of(999L, ScaleUnit.KILOBYTE, "1.0 kB"),
                Arguments.of(999L, ScaleUnit.MEGABYTE, "0 MB"),
                Arguments.of(999L, ScaleUnit.GIGABYTE, "0 GB"),

                // 1000 bytes
                Arguments.of(1000L, null, "1.0 kB"),
                Arguments.of(1000L, ScaleUnit.BYTE, "1000 B"),
                Arguments.of(1000L, ScaleUnit.KILOBYTE, "1.0 kB"),
                Arguments.of(1000L, ScaleUnit.MEGABYTE, "0 MB"),
                Arguments.of(1000L, ScaleUnit.GIGABYTE, "0 GB"),

                // 49 kilobytes
                Arguments.of(49L * 1000L, null, "49 kB"),
                Arguments.of(49L * 1000L, ScaleUnit.BYTE, "49000 B"),
                Arguments.of(49L * 1000L, ScaleUnit.KILOBYTE, "49 kB"),
                Arguments.of(49L * 1000L, ScaleUnit.MEGABYTE, "0 MB"),
                Arguments.of(49L * 1000L, ScaleUnit.GIGABYTE, "0 GB"),

                // 50 kilobytes
                Arguments.of(50L * 1000L, null, "50 kB"),
                Arguments.of(50L * 1000L, ScaleUnit.BYTE, "50000 B"),
                Arguments.of(50L * 1000L, ScaleUnit.KILOBYTE, "50 kB"),
                Arguments.of(50L * 1000L, ScaleUnit.MEGABYTE, "0.1 MB"),
                Arguments.of(50L * 1000L, ScaleUnit.GIGABYTE, "0 GB"),

                // 999 kilobytes
                Arguments.of(999L * 1000L, null, "999 kB"),
                Arguments.of(999L * 1000L, ScaleUnit.BYTE, "999000 B"),
                Arguments.of(999L * 1000L, ScaleUnit.KILOBYTE, "999 kB"),
                Arguments.of(999L * 1000L, ScaleUnit.MEGABYTE, "1.0 MB"),
                Arguments.of(999L * 1000L, ScaleUnit.GIGABYTE, "0 GB"),

                // 1000 kilobytes
                Arguments.of(1000L * 1000L, null, "1.0 MB"),
                Arguments.of(1000L * 1000L, ScaleUnit.BYTE, "1000000 B"),
                Arguments.of(1000L * 1000L, ScaleUnit.KILOBYTE, "1000 kB"),
                Arguments.of(1000L * 1000L, ScaleUnit.MEGABYTE, "1.0 MB"),
                Arguments.of(1000L * 1000L, ScaleUnit.GIGABYTE, "0 GB"),

                // 49 megabytes
                Arguments.of(49L * 1000L * 1000L, null, "49 MB"),
                Arguments.of(49L * 1000L * 1000L, ScaleUnit.BYTE, "49000000 B"),
                Arguments.of(49L * 1000L * 1000L, ScaleUnit.KILOBYTE, "49000 kB"),
                Arguments.of(49L * 1000L * 1000L, ScaleUnit.MEGABYTE, "49 MB"),
                Arguments.of(49L * 1000L * 1000L, ScaleUnit.GIGABYTE, "0 GB"),

                // 50 megabytes
                Arguments.of(50L * 1000L * 1000L, null, "50 MB"),
                Arguments.of(50L * 1000L * 1000L, ScaleUnit.BYTE, "50000000 B"),
                Arguments.of(50L * 1000L * 1000L, ScaleUnit.KILOBYTE, "50000 kB"),
                Arguments.of(50L * 1000L * 1000L, ScaleUnit.MEGABYTE, "50 MB"),
                Arguments.of(50L * 1000L * 1000L, ScaleUnit.GIGABYTE, "0.1 GB"),

                // 999 megabytes
                Arguments.of(999L * 1000L * 1000L, null, "999 MB"));
    }

    @ParameterizedTest
    @MethodSource("sizeWithScaleUnitTestData")
    void sizeWithSelectedScaleUnit(long input, ScaleUnit unit, String expected) {
        FileSizeFormat format = new FileSizeFormat();
        if (unit == null) {
            assertEquals(expected, format.format(input));
        } else {
            assertEquals(expected, format.format(input, unit));
        }
    }

    @Test
    void negativeProgressedSize() {
        FileSizeFormat format = new FileSizeFormat();

        long negativeProgressedSize = -100L;
        assertThrows(IllegalArgumentException.class, () -> format.formatProgress(negativeProgressedSize, 10L));
    }

    @Test
    void negativeProgressedSizeBiggerThanSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertThrows(IllegalArgumentException.class, () -> format.formatProgress(100L, 10L));
    }

    static Stream<Arguments> progressedSizeWithoutSizeTestData() {
        return Stream.of(
                Arguments.of(0L, "0 B"),
                Arguments.of(1000L, "1.0 kB"),
                Arguments.of(1000L * 1000L, "1.0 MB"),
                Arguments.of(1000L * 1000L * 1000L, "1.0 GB"));
    }

    @ParameterizedTest
    @MethodSource("progressedSizeWithoutSizeTestData")
    void progressedSizeWithoutSize(long progressedSize, String expected) {
        FileSizeFormat format = new FileSizeFormat();
        assertEquals(expected, format.formatProgress(progressedSize, -1L));
    }

    static Stream<Arguments> progressedSizeWithSizeTestData() {
        return Stream.of(
                // Zero test
                Arguments.of(0L, 0L, "0 B"),

                // Bytes tests
                Arguments.of(0L, 800L, "0/800 B"),
                Arguments.of(400L, 800L, "400/800 B"),
                Arguments.of(800L, 800L, "800 B"),

                // Kilobytes tests
                Arguments.of(0L, 8000L, "0/8.0 kB"),
                Arguments.of(400L, 8000L, "0.4/8.0 kB"),
                Arguments.of(4000L, 8000L, "4.0/8.0 kB"),
                Arguments.of(8000L, 8000L, "8.0 kB"),
                Arguments.of(8000L, 50000L, "8.0/50 kB"),
                Arguments.of(16000L, 50000L, "16/50 kB"),
                Arguments.of(50000L, 50000L, "50 kB"),

                // Megabytes tests
                Arguments.of(0L, 5000000L, "0/5.0 MB"),
                Arguments.of(500000L, 5000000L, "0.5/5.0 MB"),
                Arguments.of(1000000L, 5000000L, "1.0/5.0 MB"),
                Arguments.of(5000000L, 5000000L, "5.0 MB"),
                Arguments.of(5000000L, 15000000L, "5.0/15 MB"),
                Arguments.of(15000000L, 15000000L, "15 MB"),

                // Gigabytes tests
                Arguments.of(0L, 500000000L, "0/500 MB"),
                Arguments.of(1000000000L, 5000000000L, "1.0/5.0 GB"),
                Arguments.of(5000000000L, 5000000000L, "5.0 GB"),
                Arguments.of(5000000000L, 15000000000L, "5.0/15 GB"),
                Arguments.of(15000000000L, 15000000000L, "15 GB"));
    }

    @ParameterizedTest
    @MethodSource("progressedSizeWithSizeTestData")
    void progressedSizeWithSize(long progressedSize, long totalSize, String expected) {
        FileSizeFormat format = new FileSizeFormat();
        assertEquals(expected, format.formatProgress(progressedSize, totalSize));
    }

    @Test
    void formatRate() {
        FileSizeFormat format = new FileSizeFormat();

        // Test bytes per second
        MessageBuilder builder = new DefaultMessageBuilder();
        format.formatRate(builder, 5.0);
        assertEquals("5.0 B/s", builder.build());

        // Test kilobytes per second
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 5500.0);
        assertEquals("5.5 kB/s", builder.build());

        // Test megabytes per second
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 5500000.0);
        assertEquals("5.5 MB/s", builder.build());

        // Test gigabytes per second
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 5500000000.0);
        assertEquals("5.5 GB/s", builder.build());
    }

    @Test
    void formatRateThresholds() {
        FileSizeFormat format = new FileSizeFormat();

        // Test value less than 0.05
        // Test exact unit thresholds
        MessageBuilder builder = new DefaultMessageBuilder();
        format.formatRate(builder, 45.0); // 45 B/s
        assertEquals("45.0 B/s", builder.build());

        // Test value greater than or equal to 10
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 15000.0); // 15 kB/s
        assertEquals("15.0 kB/s", builder.build());

        // Test value between 0.05 and 10
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 5500.0); // 5.5 kB/s
        assertEquals("5.5 kB/s", builder.build());

        // Test exact unit thresholds
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 1000.0); // 1 kB/s
        assertEquals("1.0 kB/s", builder.build());

        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 1000000.0); // 1 MB/s
        assertEquals("1.0 MB/s", builder.build());

        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 1000000000.0); // 1 GB/s
        assertEquals("1.0 GB/s", builder.build());
    }

    @Test
    void formatRateEdgeCases() {
        FileSizeFormat format = new FileSizeFormat();

        // Test zero rate
        MessageBuilder builder = new DefaultMessageBuilder();
        format.formatRate(builder, 0.0);
        assertEquals("0.0 B/s", builder.build());

        // Test rate at exactly 1000 (1 kB/s)
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 1000.0);
        assertEquals("1.0 kB/s", builder.build());

        // Test rate at exactly 1000000 (1 MB/s)
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 1000000.0);
        assertEquals("1.0 MB/s", builder.build());
    }

    @Test
    void formatRateLargeValues() {
        FileSizeFormat format = new FileSizeFormat();

        // Test large but valid rates
        MessageBuilder builder = new DefaultMessageBuilder();
        format.formatRate(builder, 5e12); // 5 TB/s
        assertEquals("5000.0 GB/s", builder.build());

        // Test very large rate
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, 1e15); // 1 PB/s
        assertEquals("1000000.0 GB/s", builder.build());
    }

    @Test
    void formatRateInvalidValues() {
        FileSizeFormat format = new FileSizeFormat();

        // Test negative rate
        MessageBuilder builder = new DefaultMessageBuilder();
        format.formatRate(builder, -1.0);
        assertEquals("? B/s", builder.build());

        // Test NaN
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, Double.NaN);
        assertEquals("? B/s", builder.build());

        // Test Infinity
        builder = new DefaultMessageBuilder();
        format.formatRate(builder, Double.POSITIVE_INFINITY);
        assertEquals("? B/s", builder.build());

        builder = new DefaultMessageBuilder();
        format.formatRate(builder, Double.NEGATIVE_INFINITY);
        assertEquals("? B/s", builder.build());
    }
}
