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

import org.apache.maven.api.services.MessageBuilder;

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
    public enum ScaleUnit {
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

    public FileSizeFormat(Locale locale) {}

    public String format(long size) {
        return format(size, null);
    }

    public String format(long size, ScaleUnit unit) {
        return format(size, unit, false);
    }

    public String format(long size, ScaleUnit unit, boolean omitSymbol) {
        StringBuilder sb = new StringBuilder();
        format(sb, size, unit, omitSymbol);
        return sb.toString();
    }

    public void format(StringBuilder builder, long size) {
        format(builder, size, null, false);
    }

    public void format(StringBuilder builder, long size, ScaleUnit unit) {
        format(builder, size, unit, false);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void format(StringBuilder builder, long size, ScaleUnit unit, boolean omitSymbol) {
        if (size < 0L) {
            throw new IllegalArgumentException("file size cannot be negative: " + size);
        }
        if (unit == null) {
            unit = ScaleUnit.getScaleUnit(size);
        }

        double scaledSize = (double) size / unit.bytes();

        if (unit == ScaleUnit.BYTE) {
            builder.append(size);
        } else if (scaledSize < 0.05d || scaledSize >= 10.0d) {
            builder.append(Math.round(scaledSize));
        } else {
            builder.append(Math.round(scaledSize * 10d) / 10d);
        }

        if (!omitSymbol) {
            builder.append(" ").append(unit.symbol());
        }
    }

    public void format(MessageBuilder builder, long size) {
        format(builder, size, null, false);
    }

    public void format(MessageBuilder builder, long size, ScaleUnit unit) {
        format(builder, size, unit, false);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void format(MessageBuilder builder, long size, ScaleUnit unit, boolean omitSymbol) {
        if (size < 0L) {
            throw new IllegalArgumentException("file size cannot be negative: " + size);
        }
        if (unit == null) {
            unit = ScaleUnit.getScaleUnit(size);
        }

        double scaledSize = (double) size / unit.bytes();

        if (unit == ScaleUnit.BYTE) {
            builder.append(Long.toString(size));
        } else if (scaledSize < 0.05d || scaledSize >= 10.0d) {
            builder.append(Long.toString(Math.round(scaledSize)));
        } else {
            builder.append(Double.toString(Math.round(scaledSize * 10d) / 10d));
        }

        if (!omitSymbol) {
            builder.append(" ").append(unit.symbol());
        }
    }

    public String formatProgress(long progressedSize, long size) {
        StringBuilder sb = new StringBuilder();
        formatProgress(sb, progressedSize, size);
        return sb.toString();
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
            format(builder, progressedSize, unit, true);
            builder.append("/");
            format(builder, size, unit, false);
        } else {
            ScaleUnit unit = ScaleUnit.getScaleUnit(progressedSize);

            format(builder, progressedSize, unit, false);
        }
    }
}
