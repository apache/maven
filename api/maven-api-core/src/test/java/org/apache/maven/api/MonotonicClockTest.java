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
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonotonicClockTest {

    @Test
    @DisplayName("MonotonicClock singleton instance should always return the same instance")
    void singletonInstance() {
        MonotonicClock clock1 = MonotonicClock.get();
        MonotonicClock clock2 = MonotonicClock.get();

        assertThat(clock2)
                .as("Multiple calls to get() should return the same instance")
                .isSameAs(clock1);
    }

    @Test
    @DisplayName("MonotonicClock should always use UTC timezone")
    void clockTimezone() {
        MonotonicClock clock = MonotonicClock.get();

        assertThat(clock.getZone()).as("Clock should use UTC timezone").isEqualTo(ZoneOffset.UTC);

        // Verify that attempting to change timezone returns the same instance
        Clock newClock = clock.withZone(ZoneId.systemDefault());
        assertThat(newClock)
                .as("withZone() should return the same clock instance")
                .isSameAs(clock);
    }

    @Test
    @DisplayName("MonotonicClock should maintain monotonic time progression")
    void monotonicBehavior() throws InterruptedException {
        Instant first = MonotonicClock.now();
        Thread.sleep(10); // Small delay
        Instant second = MonotonicClock.now();
        Thread.sleep(10); // Small delay
        Instant third = MonotonicClock.now();

        assertThat(first.isBefore(second))
                .as("Time should progress forward between measurements")
                .isTrue();
        assertThat(second.isBefore(third))
                .as("Time should progress forward between measurements")
                .isTrue();
    }

    @Test
    @DisplayName("MonotonicClock elapsed time should increase")
    void elapsedTime() throws InterruptedException {
        Duration initial = MonotonicClock.elapsed();
        Thread.sleep(50); // Longer delay for more reliable measurement
        Duration later = MonotonicClock.elapsed();

        assertThat(later.compareTo(initial) > 0)
                .as("Elapsed time should increase")
                .isTrue();
        assertThat(later.minus(initial).toMillis() >= 45)
                .as("Elapsed time difference should be at least 45ms (accounting for some timing variance)")
                .isTrue();
    }

    @Test
    @DisplayName("MonotonicClock start time should remain constant")
    void startTime() throws InterruptedException {
        Instant start1 = MonotonicClock.start();
        Thread.sleep(10);
        Instant start2 = MonotonicClock.start();

        assertThat(start2).as("Start time should remain constant").isEqualTo(start1);
        assertThat(start1).as("Start time should not be null").isNotNull();
    }

    @Nested
    @DisplayName("Time consistency tests")
    class TimeConsistencyTests {

        @Test
        @DisplayName("Current time should be after start time")
        void currentTimeAfterStart() {
            Instant now = MonotonicClock.now();
            Instant start = MonotonicClock.start();

            assertThat(now.isAfter(start))
                    .as("Current time should be after start time")
                    .isTrue();
        }

        @Test
        @DisplayName("Elapsed time should match time difference")
        void elapsedTimeConsistency() {
            MonotonicClock clock = MonotonicClock.get();
            Instant now = clock.instant();
            Duration elapsed = clock.elapsedTime();
            Duration calculated = Duration.between(clock.startInstant(), now);

            // Allow for small timing differences (1ms) due to execution time between measurements
            assertThat(Math.abs(elapsed.toMillis() - calculated.toMillis()) <= 1)
                    .as("Elapsed time should match calculated duration between start and now")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("MonotonicClock should handle rapid successive calls")
    void rapidCalls() {
        Instant[] instants = new Instant[1000];
        for (int i = 0; i < instants.length; i++) {
            instants[i] = MonotonicClock.now();
        }

        // Verify monotonic behavior across all measurements
        for (int i = 1; i < instants.length; i++) {
            assertThat(instants[i].compareTo(instants[i - 1]) >= 0)
                    .as("Time should never go backwards even with rapid successive calls")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("MonotonicClock should maintain reasonable alignment with system time")
    void systemTimeAlignment() {
        Instant monotonic = MonotonicClock.now();
        Instant system = Instant.now();

        // The difference should be relatively small (allow for 1 second max)
        Duration difference = Duration.between(monotonic, system).abs();
        assertThat(difference.getSeconds() <= 1)
                .as("Monotonic time should be reasonably aligned with system time")
                .isTrue();
    }
}
