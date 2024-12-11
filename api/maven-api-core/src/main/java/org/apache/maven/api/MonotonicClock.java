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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A Clock implementation that combines monotonic timing with wall-clock time.
 * <p>
 * This class provides precise time measurements using {@link System#nanoTime()}
 * while maintaining wall-clock time information in UTC. The wall-clock time
 * is computed from the monotonic duration since system start to ensure consistency
 * between time measurements.
 * <p>
 * This implementation is singleton-based and always uses UTC timezone. The clock
 * cannot be adjusted to different timezones to maintain consistent monotonic behavior.
 * Users needing local time representation should convert the result of {@link #instant()}
 * to their desired timezone:
 * <pre>{@code
 * Instant now = MonotonicClock.now();
 * ZonedDateTime local = now.atZone(ZoneId.systemDefault());
 * }</pre>
 *
 * @see System#nanoTime()
 * @see Clock
 */
public class MonotonicClock extends Clock {
    private static final MonotonicClock CLOCK = new MonotonicClock();

    private final long startNanos;
    private final Instant startInstant;

    /**
     * Private constructor to enforce singleton pattern.
     * Initializes the clock with the current system time and nanoTime.
     */
    private MonotonicClock() {
        this.startNanos = System.nanoTime();
        this.startInstant = Clock.systemUTC().instant();
    }

    /**
     * Returns the singleton instance of MonotonicClock.
     *
     * @return the monotonic clock instance
     */
    public static MonotonicClock get() {
        return CLOCK;
    }

    /**
     * Returns the current instant from the monotonic clock.
     * This is a convenience method equivalent to {@code get().instant()}.
     *
     * @return the current instant using monotonic timing
     */
    public static Instant now() {
        return get().instant();
    }

    /**
     * Returns a monotonically increasing instant.
     * <p>
     * The returned instant is calculated by adding the elapsed nanoseconds
     * since clock creation to the initial wall clock time. This ensures that
     * the time never goes backwards and maintains a consistent relationship
     * with the wall clock time.
     *
     * @return the current instant using monotonic timing
     */
    @Override
    public Instant instant() {
        long elapsedNanos = System.nanoTime() - startNanos;
        return startInstant.plusNanos(elapsedNanos);
    }

    /**
     * Returns the zone ID of this clock, which is always UTC.
     *
     * @return the UTC zone ID
     */
    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /**
     * Returns this clock since timezone adjustments are not supported.
     * <p>
     * This implementation maintains UTC time to ensure monotonic behavior.
     * The provided zone parameter is ignored.
     *
     * @param zone the target timezone (ignored)
     * @return this clock instance
     */
    @Override
    public Clock withZone(ZoneId zone) {
        // Monotonic clock is always UTC-based
        return this;
    }
}
