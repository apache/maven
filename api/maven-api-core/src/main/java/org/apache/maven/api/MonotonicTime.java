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
package org.apache.maven.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a point in time using both monotonic and wall clock measurements.
 * This class combines the monotonic precision of {@link System#nanoTime()} with
 * wall clock time from {@link Clock#systemUTC()#instant()}, providing accurate duration measurements
 * while maintaining human-readable timestamps.
 * <p>
 * This class implements {@link Temporal} by delegating to its computed wall time,
 * allowing it to be used with standard Java time formatting and querying operations.
 */
public final class MonotonicTime implements Temporal {

    /**
     * Reference point representing the time when this class was first loaded.
     * Can be used as a global start time for duration measurements.
     */
    public static final MonotonicTime START = new MonotonicTime(System.nanoTime(), Clock.systemUTC().instant());

    private final long nanoTime;
    private volatile Instant wallTime;

    // Opened for testing
    MonotonicTime(long nanoTime, Instant wallTime) {
        this.nanoTime = nanoTime;
        this.wallTime = wallTime;
    }

    /**
     * Creates a new {@code MonotonicTime} instance capturing the current time
     * using both monotonic and wall clock measurements.
     *
     * @return a new {@code MonotonicTime} instance
     */
    public static MonotonicTime now() {
        return new MonotonicTime(System.nanoTime(), null);
    }

    /**
     * Calculates the duration between this time and {@link #START}.
     * This measurement uses monotonic time and is not affected by system clock changes.
     *
     * @return the duration since JVM startup
     */
    public Duration durationSinceStart() {
        return durationSince(START);
    }

    /**
     * Calculates the duration between this time and the specified start time.
     * This measurement uses monotonic time and is not affected by system clock changes.
     *
     * @param start the starting point for the duration calculation
     * @return the duration between the start time and this time
     */
    public Duration durationSince(MonotonicTime start) {
        return Duration.ofNanos(this.nanoTime - start.nanoTime);
    }

    /**
     * Returns the raw monotonic time value from System.nanoTime().
     * <p>
     * This value represents a monotonic time measurement that can only be compared
     * with other MonotonicTime instances obtained within the same JVM session.
     * The absolute value has no meaning on its own and is not related to any epoch or wall clock time.
     * <p>
     * This value has nanosecond precision but not necessarily nanosecond accuracy -
     * the actual precision depends on the underlying system.
     * <p>
     * For timing intervals, prefer using {@link #durationSince(MonotonicTime)} instead of
     * manually calculating differences between nanoTime values.
     *
     * @return the raw nanosecond value from System.nanoTime()
     * @see System#nanoTime()
     */
    public long getNanoTime() {
        return nanoTime;
    }

    /**
     * Returns the wall clock time for this instant.
     * The time is computed lazily based on START's wall time and the monotonic duration since START.
     *
     * @return the {@link Instant} representing the wall clock time
     */
    public Instant getWallTime() {
        Instant local = wallTime;
        if (local == null) {
            // Double-checked locking pattern
            synchronized (this) {
                local = wallTime;
                if (local == null) {
                    local = START.getWallTime().plus(durationSince(START));
                    wallTime = local;
                }
            }
        }
        return local;
    }

    /**
     * Creates a {@code MonotonicTime} from a millisecond timestamp.
     * <p>
     * <strong>WARNING:</strong> This method is inherently unsafe and should only be used
     * for legacy integration. It attempts to create a monotonic time measurement from
     * a wall clock timestamp, which means:
     * <ul>
     *   <li>The monotonic timing will be imprecise (millisecond vs. nanosecond precision)</li>
     *   <li>Duration calculations may be incorrect due to system clock adjustments</li>
     *   <li>The relationship between wall time and monotonic time will be artificial</li>
     *   <li>Comparisons with other MonotonicTime instances will be meaningless</li>
     * </ul>
     *
     * @param epochMillis milliseconds since Unix epoch (from System.currentTimeMillis())
     * @return a new {@code MonotonicTime} instance
     * @deprecated This method exists only for legacy integration. Use {@link #now()}
     *             for new code.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public static MonotonicTime ofEpochMillis(long epochMillis) {
        Instant wallTime = Instant.ofEpochMilli(epochMillis);
        // Converting to nanos but this relationship is artificial
        long artificalNanoTime = TimeUnit.MILLISECONDS.toNanos(epochMillis);
        return new MonotonicTime(artificalNanoTime, wallTime);
    }

    @Override
    public boolean isSupported(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return true;
        }
        return getWallTime().isSupported(field);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        return unit instanceof ChronoUnit chronoUnit && chronoUnit.isTimeBased(); // This includes DAYS and below
    }

    @Override
    public long getLong(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return 0; // We use UTC/Zero offset
        }
        return getWallTime().getLong(field);
    }

    @Override
    public Temporal with(TemporalField field, long newValue) {
        throw new UnsupportedTemporalTypeException("MonotonicTime does not support field adjustments");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId()) {
            return (R) ZoneId.of("UTC");
        }
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        if (query == TemporalQueries.zone()) {
            return (R) ZoneId.of("UTC");  // Consistent with zoneId query
        }
        if (query == TemporalQueries.chronology()) {
            return null;
        }
        return getWallTime().query(query);
    }

    @Override
    public ValueRange range(TemporalField field) {
        return getWallTime().range(field);
    }

    @Override
    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        throw new UnsupportedTemporalTypeException("MonotonicTime does not support plus operations");
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        if (!(unit instanceof ChronoUnit) || !isSupported(unit)) {
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }

        if (endExclusive instanceof MonotonicTime other) {
            Duration duration = Duration.ofNanos(other.nanoTime - this.nanoTime);
            return switch ((ChronoUnit) unit) {
                case NANOS -> duration.toNanos();
                case MICROS -> duration.toNanos() / 1000;
                case MILLIS -> duration.toMillis();
                case SECONDS -> duration.toSeconds();
                case MINUTES -> duration.toMinutes();
                case HOURS -> duration.toHours();
                default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
            };
        }

        return unit.between(getWallTime(), Instant.from(endExclusive));
    }

    @Override
    public int get(TemporalField field) {
        return getWallTime().get(field);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MonotonicTime)) {
            return false;
        }
        MonotonicTime other = (MonotonicTime) obj;
        return other.nanoTime == this.nanoTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nanoTime);
    }

    /**
     * Returns a string representation of this time, including both the wall clock time
     * and the duration since the Unix epoch.
     *
     * @return a string representation of this time
     */
    @Override
    public String toString() {
        return String.format("MonotonicTime[wall=%s, duration=%s]", getWallTime(), Duration.ofNanos(nanoTime));
    }
}
