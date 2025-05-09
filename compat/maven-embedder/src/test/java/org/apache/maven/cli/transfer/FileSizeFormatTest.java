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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@Deprecated
class FileSizeFormatTest {

    @Test
    void negativeSize() {
        FileSizeFormat format = new FileSizeFormat();

        long negativeSize = -100L;
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> format.format(negativeSize));
    }

    @Test
    void size() {
        FileSizeFormat format = new FileSizeFormat();

        assertThat(format.format(0L)).isEqualTo("0 B");
        assertThat(format.format(5L)).isEqualTo("5 B");
        assertThat(format.format(10L)).isEqualTo("10 B");
        assertThat(format.format(15L)).isEqualTo("15 B");
        assertThat(format.format(999L)).isEqualTo("999 B");
        assertThat(format.format(1000L)).isEqualTo("1.0 kB");
        assertThat(format.format(5500L)).isEqualTo("5.5 kB");
        assertThat(format.format(10L * 1000L)).isEqualTo("10 kB");
        assertThat(format.format(15L * 1000L)).isEqualTo("15 kB");
        assertThat(format.format(999L * 1000L)).isEqualTo("999 kB");
        assertThat(format.format(1000L * 1000L)).isEqualTo("1.0 MB");
        assertThat(format.format(5500L * 1000L)).isEqualTo("5.5 MB");
        assertThat(format.format(10L * 1000L * 1000L)).isEqualTo("10 MB");
        assertThat(format.format(15L * 1000L * 1000L)).isEqualTo("15 MB");
        assertThat(format.format(999L * 1000L * 1000L)).isEqualTo("999 MB");
        assertThat(format.format(1000L * 1000L * 1000L)).isEqualTo("1.0 GB");
        assertThat(format.format(5500L * 1000L * 1000L)).isEqualTo("5.5 GB");
        assertThat(format.format(10L * 1000L * 1000L * 1000L)).isEqualTo("10 GB");
        assertThat(format.format(15L * 1000L * 1000L * 1000L)).isEqualTo("15 GB");
        assertThat(format.format(1000L * 1000L * 1000L * 1000L)).isEqualTo("1000 GB");
    }

    @Test
    void sizeWithSelectedScaleUnit() {
        FileSizeFormat format = new FileSizeFormat();

        assertThat(format.format(0L)).isEqualTo("0 B");
        assertThat(format.format(0L, ScaleUnit.BYTE)).isEqualTo("0 B");
        assertThat(format.format(0L, ScaleUnit.KILOBYTE)).isEqualTo("0 kB");
        assertThat(format.format(0L, ScaleUnit.MEGABYTE)).isEqualTo("0 MB");
        assertThat(format.format(0L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(5L)).isEqualTo("5 B");
        assertThat(format.format(5L, ScaleUnit.BYTE)).isEqualTo("5 B");
        assertThat(format.format(5L, ScaleUnit.KILOBYTE)).isEqualTo("0 kB");
        assertThat(format.format(5L, ScaleUnit.MEGABYTE)).isEqualTo("0 MB");
        assertThat(format.format(5L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(49L)).isEqualTo("49 B");
        assertThat(format.format(49L, ScaleUnit.BYTE)).isEqualTo("49 B");
        assertThat(format.format(49L, ScaleUnit.KILOBYTE)).isEqualTo("0 kB");
        assertThat(format.format(49L, ScaleUnit.MEGABYTE)).isEqualTo("0 MB");
        assertThat(format.format(49L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(50L)).isEqualTo("50 B");
        assertThat(format.format(50L, ScaleUnit.BYTE)).isEqualTo("50 B");
        assertThat(format.format(50L, ScaleUnit.KILOBYTE)).isEqualTo("0.1 kB");
        assertThat(format.format(50L, ScaleUnit.MEGABYTE)).isEqualTo("0 MB");
        assertThat(format.format(50L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(999L)).isEqualTo("999 B");
        assertThat(format.format(999L, ScaleUnit.BYTE)).isEqualTo("999 B");
        assertThat(format.format(999L, ScaleUnit.KILOBYTE)).isEqualTo("1.0 kB");
        assertThat(format.format(999L, ScaleUnit.MEGABYTE)).isEqualTo("0 MB");
        assertThat(format.format(999L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(1000L)).isEqualTo("1.0 kB");
        assertThat(format.format(1000L, ScaleUnit.BYTE)).isEqualTo("1000 B");
        assertThat(format.format(1000L, ScaleUnit.KILOBYTE)).isEqualTo("1.0 kB");
        assertThat(format.format(1000L, ScaleUnit.MEGABYTE)).isEqualTo("0 MB");
        assertThat(format.format(1000L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(49L * 1000L)).isEqualTo("49 kB");
        assertThat(format.format(49L * 1000L, ScaleUnit.BYTE)).isEqualTo("49000 B");
        assertThat(format.format(49L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("49 kB");
        assertThat(format.format(49L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("0 MB");
        assertThat(format.format(49L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(50L * 1000L)).isEqualTo("50 kB");
        assertThat(format.format(50L * 1000L, ScaleUnit.BYTE)).isEqualTo("50000 B");
        assertThat(format.format(50L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("50 kB");
        assertThat(format.format(50L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("0.1 MB");
        assertThat(format.format(50L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(999L * 1000L)).isEqualTo("999 kB");
        assertThat(format.format(999L * 1000L, ScaleUnit.BYTE)).isEqualTo("999000 B");
        assertThat(format.format(999L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("999 kB");
        assertThat(format.format(999L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("1.0 MB");
        assertThat(format.format(999L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(1000L * 1000L)).isEqualTo("1.0 MB");
        assertThat(format.format(1000L * 1000L, ScaleUnit.BYTE)).isEqualTo("1000000 B");
        assertThat(format.format(1000L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("1000 kB");
        assertThat(format.format(1000L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("1.0 MB");
        assertThat(format.format(1000L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(49L * 1000L * 1000L)).isEqualTo("49 MB");
        assertThat(format.format(49L * 1000L * 1000L, ScaleUnit.BYTE)).isEqualTo("49000000 B");
        assertThat(format.format(49L * 1000L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("49000 kB");
        assertThat(format.format(49L * 1000L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("49 MB");
        assertThat(format.format(49L * 1000L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("0 GB");

        assertThat(format.format(50L * 1000L * 1000L)).isEqualTo("50 MB");
        assertThat(format.format(50L * 1000L * 1000L, ScaleUnit.BYTE)).isEqualTo("50000000 B");
        assertThat(format.format(50L * 1000L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("50000 kB");
        assertThat(format.format(50L * 1000L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("50 MB");
        assertThat(format.format(50L * 1000L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("0.1 GB");

        assertThat(format.format(999L * 1000L * 1000L)).isEqualTo("999 MB");
        assertThat(format.format(999L * 1000L * 1000L, ScaleUnit.BYTE)).isEqualTo("999000000 B");
        assertThat(format.format(999L * 1000L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("999000 kB");
        assertThat(format.format(999L * 1000L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("999 MB");
        assertThat(format.format(999L * 1000L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("1.0 GB");

        assertThat(format.format(1000L * 1000L * 1000L)).isEqualTo("1.0 GB");
        assertThat(format.format(1000L * 1000L * 1000L, ScaleUnit.BYTE)).isEqualTo("1000000000 B");
        assertThat(format.format(1000L * 1000L * 1000L, ScaleUnit.KILOBYTE)).isEqualTo("1000000 kB");
        assertThat(format.format(1000L * 1000L * 1000L, ScaleUnit.MEGABYTE)).isEqualTo("1000 MB");
        assertThat(format.format(1000L * 1000L * 1000L, ScaleUnit.GIGABYTE)).isEqualTo("1.0 GB");
    }

    @Test
    void negativeProgressedSize() {
        FileSizeFormat format = new FileSizeFormat();

        long negativeProgressedSize = -100L;
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> format.formatProgress(negativeProgressedSize, 10L));
    }

    @Test
    void negativeProgressedSizeBiggerThanSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> format.formatProgress(100L, 10L));
    }

    @Test
    void progressedSizeWithoutSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertThat(format.formatProgress(0L, -1L)).isEqualTo("0 B");
        assertThat(format.formatProgress(1000L, -1L)).isEqualTo("1.0 kB");
        assertThat(format.formatProgress(1000L * 1000L, -1L)).isEqualTo("1.0 MB");
        assertThat(format.formatProgress(1000L * 1000L * 1000L, -1L)).isEqualTo("1.0 GB");
    }

    @Test
    void progressedBothZero() {
        FileSizeFormat format = new FileSizeFormat();

        assertThat(format.formatProgress(0L, 0L)).isEqualTo("0 B");
    }

    @Test
    void progressedSizeWithSize() {
        FileSizeFormat format = new FileSizeFormat();

        assertThat(format.formatProgress(0L, 800L)).isEqualTo("0/800 B");
        assertThat(format.formatProgress(400L, 800L)).isEqualTo("400/800 B");
        assertThat(format.formatProgress(800L, 800L)).isEqualTo("800 B");

        assertThat(format.formatProgress(0L, 8000L)).isEqualTo("0/8.0 kB");
        assertThat(format.formatProgress(400L, 8000L)).isEqualTo("0.4/8.0 kB");
        assertThat(format.formatProgress(4000L, 8000L)).isEqualTo("4.0/8.0 kB");
        assertThat(format.formatProgress(8000L, 8000L)).isEqualTo("8.0 kB");
        assertThat(format.formatProgress(8000L, 50000L)).isEqualTo("8.0/50 kB");
        assertThat(format.formatProgress(16000L, 50000L)).isEqualTo("16/50 kB");
        assertThat(format.formatProgress(50000L, 50000L)).isEqualTo("50 kB");

        assertThat(format.formatProgress(0L, 5000000L)).isEqualTo("0/5.0 MB");
        assertThat(format.formatProgress(500000L, 5000000L)).isEqualTo("0.5/5.0 MB");
        assertThat(format.formatProgress(1000000L, 5000000L)).isEqualTo("1.0/5.0 MB");
        assertThat(format.formatProgress(5000000L, 5000000L)).isEqualTo("5.0 MB");
        assertThat(format.formatProgress(5000000L, 15000000L)).isEqualTo("5.0/15 MB");
        assertThat(format.formatProgress(15000000L, 15000000L)).isEqualTo("15 MB");

        assertThat(format.formatProgress(0L, 500000000L)).isEqualTo("0/500 MB");
        assertThat(format.formatProgress(1000000000L, 5000000000L)).isEqualTo("1.0/5.0 GB");
        assertThat(format.formatProgress(5000000000L, 5000000000L)).isEqualTo("5.0 GB");
        assertThat(format.formatProgress(5000000000L, 15000000000L)).isEqualTo("5.0/15 GB");
        assertThat(format.formatProgress(15000000000L, 15000000000L)).isEqualTo("15 GB");
    }
}
