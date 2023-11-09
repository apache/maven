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
package org.apache.maven.cli.transfer;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSizeFormatTest {

    @Test
    void testNegativeSize() {
        FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);

        long negativeSize = -100L;
        assertThrows(IllegalArgumentException.class, () -> format.format(negativeSize));
    }

    @Test
    void testSize() {
        long _0_bytes = 0L;
        assertEquals("0 B", format(_0_bytes));

        long _5_bytes = 5L;
        assertEquals("5 B", format(_5_bytes));

        long _10_bytes = 10L;
        assertEquals("10 B", format(_10_bytes));

        long _15_bytes = 15L;
        assertEquals("15 B", format(_15_bytes));

        long _999_bytes = 999L;
        assertEquals("999 B", format(_999_bytes));

        long _1000_bytes = 1000L;
        assertEquals("1.0 kB", format(_1000_bytes));

        long _5500_bytes = 5500L;
        assertEquals("5.5 kB", format(_5500_bytes));

        long _10_kilobytes = 10L * 1000L;
        assertEquals("10 kB", format(_10_kilobytes));

        long _15_kilobytes = 15L * 1000L;
        assertEquals("15 kB", format(_15_kilobytes));

        long _999_kilobytes = 999L * 1000L;
        assertEquals("999 kB", format(_999_kilobytes));

        long _1000_kilobytes = 1000L * 1000L;
        assertEquals("1.0 MB", format(_1000_kilobytes));

        long _5500_kilobytes = 5500L * 1000L;
        assertEquals("5.5 MB", format(_5500_kilobytes));

        long _10_megabytes = 10L * 1000L * 1000L;
        assertEquals("10 MB", format(_10_megabytes));

        long _15_megabytes = 15L * 1000L * 1000L;
        assertEquals("15 MB", format(_15_megabytes));

        long _999_megabytes = 999L * 1000L * 1000L;
        assertEquals("999 MB", format(_999_megabytes));

        long _1000_megabytes = 1000L * 1000L * 1000L;
        assertEquals("1.0 GB", format(_1000_megabytes));

        long _5500_megabytes = 5500L * 1000L * 1000L;
        assertEquals("5.5 GB", format(_5500_megabytes));

        long _10_gigabytes = 10L * 1000L * 1000L * 1000L;
        assertEquals("10 GB", format(_10_gigabytes));

        long _15_gigabytes = 15L * 1000L * 1000L * 1000L;
        assertEquals("15 GB", format(_15_gigabytes));

        long _1000_gigabytes = 1000L * 1000L * 1000L * 1000L;
        assertEquals("1000 GB", format(_1000_gigabytes));
    }

    private static String format(long size) {
        FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);
        return format.format(size);
    }

    private static String formatProgress(long progressedSize, long size) {
        FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);
        return format.formatProgress(progressedSize, size);
    }

    @Test
    void testNegativeProgressedSize() {
        long negativeProgressedSize = -100L;
        assertThrows(IllegalArgumentException.class, () -> formatProgress(negativeProgressedSize, 10L));
    }

    @Test
    void testNegativeProgressedSizeBiggerThanSize() {
        assertThrows(IllegalArgumentException.class, () -> formatProgress(100L, 10L));
    }

    @Test
    void testProgressedSizeWithoutSize() {
        long _0_bytes = 0L;
        assertEquals("0 B", formatProgress(_0_bytes, -1L));

        long _1000_bytes = 1000L;
        assertEquals("1.0 kB", formatProgress(_1000_bytes, -1L));

        long _1000_kilobytes = 1000L * 1000L;
        assertEquals("1.0 MB", formatProgress(_1000_kilobytes, -1L));

        long _1000_megabytes = 1000L * 1000L * 1000L;
        assertEquals("1.0 GB", formatProgress(_1000_megabytes, -1L));
    }

    @Test
    void testProgressedBothZero() {
        long _0_bytes = 0L;
        assertEquals("0 B", formatProgress(_0_bytes, _0_bytes));
    }

    @Test
    void testProgressedSizeWithSize() {
        long _0_bytes = 0L;
        long _400_bytes = 400L;
        long _800_bytes = 2L * _400_bytes;
        assertEquals("0/800 B", formatProgress(_0_bytes, _800_bytes));
        assertEquals("400/800 B", formatProgress(_400_bytes, _800_bytes));
        assertEquals("800 B", formatProgress(_800_bytes, _800_bytes));

        long _4000_bytes = 4000L;
        long _8000_bytes = 2L * _4000_bytes;
        long _50_kilobytes = 50000L;
        assertEquals("0/8.0 kB", formatProgress(_0_bytes, _8000_bytes));
        assertEquals("0.4/8.0 kB", formatProgress(_400_bytes, _8000_bytes));
        assertEquals("4.0/8.0 kB", formatProgress(_4000_bytes, _8000_bytes));
        assertEquals("8.0 kB", formatProgress(_8000_bytes, _8000_bytes));
        assertEquals("8.0/50 kB", formatProgress(_8000_bytes, _50_kilobytes));
        assertEquals("16/50 kB", formatProgress(2L * _8000_bytes, _50_kilobytes));
        assertEquals("50 kB", formatProgress(_50_kilobytes, _50_kilobytes));

        long _500_kilobytes = 500000L;
        long _1000_kilobytes = 2L * _500_kilobytes;
        ;
        long _5000_kilobytes = 5L * _1000_kilobytes;
        long _15_megabytes = 3L * _5000_kilobytes;
        assertEquals("0/5.0 MB", formatProgress(_0_bytes, _5000_kilobytes));
        assertEquals("0.5/5.0 MB", formatProgress(_500_kilobytes, _5000_kilobytes));
        assertEquals("1.0/5.0 MB", formatProgress(_1000_kilobytes, _5000_kilobytes));
        assertEquals("5.0 MB", formatProgress(_5000_kilobytes, _5000_kilobytes));
        assertEquals("5.0/15 MB", formatProgress(_5000_kilobytes, _15_megabytes));
        assertEquals("15 MB", formatProgress(_15_megabytes, _15_megabytes));

        long _500_megabytes = 500000000L;
        long _1000_megabytes = 2L * _500_megabytes;
        long _5000_megabytes = 5L * _1000_megabytes;
        long _15_gigabytes = 3L * _5000_megabytes;
        assertEquals("0/500 MB", formatProgress(_0_bytes, _500_megabytes));
        assertEquals("1.0/5.0 GB", formatProgress(_1000_megabytes, _5000_megabytes));
        assertEquals("5.0 GB", formatProgress(_5000_megabytes, _5000_megabytes));
        assertEquals("5.0/15 GB", formatProgress(_5000_megabytes, _15_gigabytes));
        assertEquals("15 GB", formatProgress(_15_gigabytes, _15_gigabytes));
    }
}
