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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formats file size with the associated <a href="https://en.wikipedia.org/wiki/Metric_prefix">SI</a> prefix
 * (GB, MB, kB) and using the patterns <code>#0.0</code> for numbers between 1 and 10
 * and <code>###0</code> for numbers between 10 and 1000+ by default.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Metric_prefix">https://en.wikipedia.org/wiki/Metric_prefix</a>
 * @see <a href="https://en.wikipedia.org/wiki/Binary_prefix">https://en.wikipedia.org/wiki/Binary_prefix</a>
 * @see <a
 *      href="https://en.wikipedia.org/wiki/Octet_%28computing%29">https://en.wikipedia.org/wiki/Octet_(computing)</a>
 */
public class FileSizeFormat {
    enum ScaleUnit {
        BYTE {
            @Override
            public long bytes() {
                return 1L;
            }

            @Override
            public String symbol() {
                return "B";
            }
        },
        KILOBYTE {
            @Override
            public long bytes() {
                return 1000L;
            }

            @Override
            public String symbol() {
                return "kB";
            }
        },
        MEGABYTE {
            @Override
            public long bytes() {
                return KILOBYTE.bytes() * KILOBYTE.bytes();
            }

            @Override
            public String symbol() {
                return "MB";
            }
        },
        GIGABYTE {
            @Override
            public long bytes() {
                return MEGABYTE.bytes() * KILOBYTE.bytes();
            }
            ;

            @Override
            public String symbol() {
                return "GB";
            }
        };

        public abstract long bytes();

        public abstract String symbol();

        public static ScaleUnit getScaleUnit(long size) {
            if (size < 0L) {
                throw new IllegalArgumentException("file size cannot be negative: " + size);
            }

            if (size >= GIGABYTE.bytes()) {
                return GIGABYTE;
            } else if (size >= MEGABYTE.bytes()) {
                return MEGABYTE;
            } else if (size >= KILOBYTE.bytes()) {
                return KILOBYTE;
            } else {
                return BYTE;
            }
        }
    }

    private DecimalFormat smallFormat;
    private DecimalFormat largeFormat;

    public FileSizeFormat(Locale locale) {
        smallFormat = new DecimalFormat("#0.0", new DecimalFormatSymbols(locale));
        largeFormat = new DecimalFormat("###0", new DecimalFormatSymbols(locale));
    }

    public String format(long size) {
        return format(size, ScaleUnit.getScaleUnit(size));
    }

    public String getScaleSymbol(long size) {
        return ScaleUnit.getScaleUnit(size).symbol();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private String format(long size, ScaleUnit unit) {
        if (size < 0L) {
            throw new IllegalArgumentException("file size cannot be negative: " + size);
        }

        double scaledSize = (double) size / unit.bytes();

        if (unit == ScaleUnit.BYTE) {
            return largeFormat.format(size);
        }

        if (scaledSize < 0.05 || scaledSize >= 10.0) {
            return largeFormat.format(scaledSize);
        } else {
            return smallFormat.format(scaledSize);
        }
    }

    public void formatProgress(StringBuilder builder, long progressedSize, long size) {
        if (progressedSize < 0L) {
            throw new IllegalArgumentException("progressed file size cannot be negative: " + size);
        }
        if (size >= 0 && progressedSize > size) {
            throw new IllegalArgumentException(
                    "progressed file size cannot be greater than size: " + progressedSize + " > " + size);
        }

        if (size >= 0L && progressedSize != size) {
            ScaleUnit unit = ScaleUnit.getScaleUnit(size);
            String formattedProgressedSize = format(progressedSize, unit);
            String formattedSize = format(size, unit) + " " + unit.symbol();

            builder.append(formattedProgressedSize).append("/").append(formattedSize);
        } else {
            ScaleUnit unit = ScaleUnit.getScaleUnit(progressedSize);

            builder.append(format(progressedSize, unit)).append(" ").append(unit.symbol());
        }
    }
}
