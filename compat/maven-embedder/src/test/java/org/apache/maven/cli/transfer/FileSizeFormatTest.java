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

import org.apache.maven.cli.transfer.FileSizeFormat.ScaleUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Deprecated
class FileSizeFormatTest {

    @Test
    void testNegativeSize() {
        FileSizeFormat format = new FileSizeFormat();

        long negativeSize = -100L;
        assertThrows(IllegalArgumentException.class, () -> format.format(negativeSize));
    }

    @Test
    void testSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertEquals("0 B", format.format(0L));
        assertEquals("5 B", format.format(5L));
        assertEquals("10 B", format.format(10L));
        assertEquals("15 B", format.format(15L));
        assertEquals("999 B", format.format(999L));
        assertEquals("1.0 kB", format.format(1000L));
        assertEquals("5.5 kB", format.format(5500L));
        assertEquals("10 kB", format.format(10L * 1000L));
        assertEquals("15 kB", format.format(15L * 1000L));
        assertEquals("999 kB", format.format(999L * 1000L));
        assertEquals("1.0 MB", format.format(1000L * 1000L));
        assertEquals("5.5 MB", format.format(5500L * 1000L));
        assertEquals("10 MB", format.format(10L * 1000L * 1000L));
        assertEquals("15 MB", format.format(15L * 1000L * 1000L));
        assertEquals("999 MB", format.format(999L * 1000L * 1000L));
        assertEquals("1.0 GB", format.format(1000L * 1000L * 1000L));
        assertEquals("5.5 GB", format.format(5500L * 1000L * 1000L));
        assertEquals("10 GB", format.format(10L * 1000L * 1000L * 1000L));
        assertEquals("15 GB", format.format(15L * 1000L * 1000L * 1000L));
        assertEquals("1000 GB", format.format(1000L * 1000L * 1000L * 1000L));
    }

    @Test
    void testSizeWithSelectedScaleUnit() {
        FileSizeFormat format = new FileSizeFormat();

        assertEquals("0 B", format.format(0L));
        assertEquals("0 B", format.format(0L, ScaleUnit.BYTE));
        assertEquals("0 kB", format.format(0L, ScaleUnit.KILOBYTE));
        assertEquals("0 MB", format.format(0L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(0L, ScaleUnit.GIGABYTE));

        assertEquals("5 B", format.format(5L));
        assertEquals("5 B", format.format(5L, ScaleUnit.BYTE));
        assertEquals("0 kB", format.format(5L, ScaleUnit.KILOBYTE));
        assertEquals("0 MB", format.format(5L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(5L, ScaleUnit.GIGABYTE));

        assertEquals("49 B", format.format(49L));
        assertEquals("49 B", format.format(49L, ScaleUnit.BYTE));
        assertEquals("0 kB", format.format(49L, ScaleUnit.KILOBYTE));
        assertEquals("0 MB", format.format(49L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(49L, ScaleUnit.GIGABYTE));

        assertEquals("50 B", format.format(50L));
        assertEquals("50 B", format.format(50L, ScaleUnit.BYTE));
        assertEquals("0.1 kB", format.format(50L, ScaleUnit.KILOBYTE));
        assertEquals("0 MB", format.format(50L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(50L, ScaleUnit.GIGABYTE));

        assertEquals("999 B", format.format(999L));
        assertEquals("999 B", format.format(999L, ScaleUnit.BYTE));
        assertEquals("1.0 kB", format.format(999L, ScaleUnit.KILOBYTE));
        assertEquals("0 MB", format.format(999L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(999L, ScaleUnit.GIGABYTE));

        assertEquals("1.0 kB", format.format(1000L));
        assertEquals("1000 B", format.format(1000L, ScaleUnit.BYTE));
        assertEquals("1.0 kB", format.format(1000L, ScaleUnit.KILOBYTE));
        assertEquals("0 MB", format.format(1000L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(1000L, ScaleUnit.GIGABYTE));

        assertEquals("49 kB", format.format(49L * 1000L));
        assertEquals("49000 B", format.format(49L * 1000L, ScaleUnit.BYTE));
        assertEquals("49 kB", format.format(49L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("0 MB", format.format(49L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(49L * 1000L, ScaleUnit.GIGABYTE));

        assertEquals("50 kB", format.format(50L * 1000L));
        assertEquals("50000 B", format.format(50L * 1000L, ScaleUnit.BYTE));
        assertEquals("50 kB", format.format(50L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("0.1 MB", format.format(50L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(50L * 1000L, ScaleUnit.GIGABYTE));

        assertEquals("999 kB", format.format(999L * 1000L));
        assertEquals("999000 B", format.format(999L * 1000L, ScaleUnit.BYTE));
        assertEquals("999 kB", format.format(999L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("1.0 MB", format.format(999L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(999L * 1000L, ScaleUnit.GIGABYTE));

        assertEquals("1.0 MB", format.format(1000L * 1000L));
        assertEquals("1000000 B", format.format(1000L * 1000L, ScaleUnit.BYTE));
        assertEquals("1000 kB", format.format(1000L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("1.0 MB", format.format(1000L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(1000L * 1000L, ScaleUnit.GIGABYTE));

        assertEquals("49 MB", format.format(49L * 1000L * 1000L));
        assertEquals("49000000 B", format.format(49L * 1000L * 1000L, ScaleUnit.BYTE));
        assertEquals("49000 kB", format.format(49L * 1000L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("49 MB", format.format(49L * 1000L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("0 GB", format.format(49L * 1000L * 1000L, ScaleUnit.GIGABYTE));

        assertEquals("50 MB", format.format(50L * 1000L * 1000L));
        assertEquals("50000000 B", format.format(50L * 1000L * 1000L, ScaleUnit.BYTE));
        assertEquals("50000 kB", format.format(50L * 1000L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("50 MB", format.format(50L * 1000L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("0.1 GB", format.format(50L * 1000L * 1000L, ScaleUnit.GIGABYTE));

        assertEquals("999 MB", format.format(999L * 1000L * 1000L));
        assertEquals("999000000 B", format.format(999L * 1000L * 1000L, ScaleUnit.BYTE));
        assertEquals("999000 kB", format.format(999L * 1000L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("999 MB", format.format(999L * 1000L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("1.0 GB", format.format(999L * 1000L * 1000L, ScaleUnit.GIGABYTE));

        assertEquals("1.0 GB", format.format(1000L * 1000L * 1000L));
        assertEquals("1000000000 B", format.format(1000L * 1000L * 1000L, ScaleUnit.BYTE));
        assertEquals("1000000 kB", format.format(1000L * 1000L * 1000L, ScaleUnit.KILOBYTE));
        assertEquals("1000 MB", format.format(1000L * 1000L * 1000L, ScaleUnit.MEGABYTE));
        assertEquals("1.0 GB", format.format(1000L * 1000L * 1000L, ScaleUnit.GIGABYTE));
    }

    @Test
    void testNegativeProgressedSize() {
        FileSizeFormat format = new FileSizeFormat();

        long negativeProgressedSize = -100L;
        assertThrows(IllegalArgumentException.class, () -> format.formatProgress(negativeProgressedSize, 10L));
    }

    @Test
    void testNegativeProgressedSizeBiggerThanSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertThrows(IllegalArgumentException.class, () -> format.formatProgress(100L, 10L));
    }

    @Test
    void testProgressedSizeWithoutSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertEquals("0 B", format.formatProgress(0L, -1L));
        assertEquals("1.0 kB", format.formatProgress(1000L, -1L));
        assertEquals("1.0 MB", format.formatProgress(1000L * 1000L, -1L));
        assertEquals("1.0 GB", format.formatProgress(1000L * 1000L * 1000L, -1L));
    }

    @Test
    void testProgressedBothZero() {
        FileSizeFormat format = new FileSizeFormat();

        assertEquals("0 B", format.formatProgress(0L, 0L));
    }

    @Test
    void testProgressedSizeWithSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertEquals("0/800 B", format.formatProgress(0L, 800L));
        assertEquals("400/800 B", format.formatProgress(400L, 800L));
        assertEquals("800 B", format.formatProgress(800L, 800L));

        assertEquals("0/8.0 kB", format.formatProgress(0L, 8000L));
        assertEquals("0.4/8.0 kB", format.formatProgress(400L, 8000L));
        assertEquals("4.0/8.0 kB", format.formatProgress(4000L, 8000L));
        assertEquals("8.0 kB", format.formatProgress(8000L, 8000L));
        assertEquals("8.0/50 kB", format.formatProgress(8000L, 50000L));
        assertEquals("16/50 kB", format.formatProgress(16000L, 50000L));
        assertEquals("50 kB", format.formatProgress(50000L, 50000L));

        assertEquals("0/5.0 MB", format.formatProgress(0L, 5000000L));
        assertEquals("0.5/5.0 MB", format.formatProgress(500000L, 5000000L));
        assertEquals("1.0/5.0 MB", format.formatProgress(1000000L, 5000000L));
        assertEquals("5.0 MB", format.formatProgress(5000000L, 5000000L));
        assertEquals("5.0/15 MB", format.formatProgress(5000000L, 15000000L));
        assertEquals("15 MB", format.formatProgress(15000000L, 15000000L));

        assertEquals("0/500 MB", format.formatProgress(0L, 500000000L));
        assertEquals("1.0/5.0 GB", format.formatProgress(1000000000L, 5000000000L));
        assertEquals("5.0 GB", format.formatProgress(5000000000L, 5000000000L));
        assertEquals("5.0/15 GB", format.formatProgress(5000000000L, 15000000000L));
        assertEquals("15 GB", format.formatProgress(15000000000L, 15000000000L));
    }
}
