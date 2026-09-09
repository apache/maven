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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonotonicClockTest {

    @Test
    @DisplayName("MonotonicClock singleton instance should always return the same instance")
    void testSingletonInstance() {
        MonotonicClock clock1 = MonotonicClock.get();
        MonotonicClock clock2 = MonotonicClock.get();

        assertSame(clock1, clock2, "Multiple calls to get() should return the same instance");
    }

    @Test
    @DisplayName("MonotonicClock should always use UTC timezone")
    void testClockTimezone() {
        MonotonicClock clock = MonotonicClock.get();

        assertEquals(ZoneOffset.UTC, clock.getZone(), "Clock should use UTC timezone");
    }

    @Test
    @DisplayName("withZone() should return a new clock with the specified timezone")
    void testWithZoneReturnsNewClock() {
        MonotonicClock clock = MonotonicClock.get();

        // withZone should return a new instance with the requested timezone
        Clock newClock = clock.withZone(ZoneId.systemDefault());
        assertNotNull(newClock, "withZone() should return a non-null clock");
        assertEquals(ZoneId.systemDefault(), newClock.getZone(), "New clock should have the requested zone");
        assertNotSame(clock, newClock, "withZone() should return a new instance, not the same clock");

        // Both clocks should report instants that are very close (within 1ms)
        Instant originalInstant = clock.instant();
        Instant newClockInstant = newClock.instant();
        assertEquals(
                originalInstant.getEpochSecond(),
                newClockInstant.getEpochSecond(),
                "Clocks should report the same instant (monotonic time is preserved)");
        long nanoDiff = Math.abs(originalInstant.getNano() - newClockInstant.getNano());
        assertTrue(
                nanoDiff < 1_000_000,
                "Clocks should report instants within 1ms of each other (diff: " + nanoDiff + "ns)");
    }

    @Test
    @DisplayName("withZone() with the same zone should return the same instance")
    void testWithZoneSameZoneReturnsThis() {
        MonotonicClock clock = MonotonicClock.get();

        // Requesting UTC from a UTC clock should return the same instance
        assertSame(
                clock, clock.withZone(ZoneOffset.UTC), "withZone(UTC) should return this when the zone is already UTC");
    }

    @Test
    @DisplayName("withZone() should throw NullPointerException on null zone")
    void testWithZoneNullThrows() {
        MonotonicClock clock = MonotonicClock.get();

        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> clock.withZone(null),
                "withZone(null) should throw NullPointerException");
    }

    @Test
    @DisplayName("MonotonicClock should maintain monotonic time progression")
    void testMonotonicBehavior() throws InterruptedException {
        Instant first = MonotonicClock.now();
        Thread.sleep(10); // Small delay
        Instant second = MonotonicClock.now();
        Thread.sleep(10); // Small delay
        Instant third = MonotonicClock.now();

        assertTrue(first.isBefore(second), "Time should progress forward between measurements");
        assertTrue(second.isBefore(third), "Time should progress forward between measurements");
    }

    @Test
    @DisplayName("MonotonicClock elapsed time should increase")
    void testElapsedTime() throws InterruptedException {
        Duration initial = MonotonicClock.elapsed();
        Thread.sleep(50); // Longer delay for more reliable measurement
        Duration later = MonotonicClock.elapsed();

        assertTrue(later.compareTo(initial) > 0, "Elapsed time should increase");
        assertTrue(
                later.minus(initial).toMillis() >= 45,
                "Elapsed time difference should be at least 45ms (accounting for some timing variance)");
    }

    @Test
    @DisplayName("MonotonicClock start time should remain constant")
    void testStartTime() throws InterruptedException {
        Instant start1 = MonotonicClock.start();
        Thread.sleep(10);
        Instant start2 = MonotonicClock.start();

        assertEquals(start1, start2, "Start time should remain constant");
        assertNotNull(start1, "Start time should not be null");
    }

    @Nested
    @DisplayName("Time consistency tests")
    class TimeConsistencyTests {

        @Test
        @DisplayName("Current time should be after start time")
        void testCurrentTimeAfterStart() {
            Instant now = MonotonicClock.now();
            Instant start = MonotonicClock.start();

            assertTrue(now.isAfter(start), "Current time should be after start time");
        }

        @Test
        @DisplayName("Elapsed time should match time difference")
        void testElapsedTimeConsistency() {
            MonotonicClock clock = MonotonicClock.get();
            // Sandwich the instant() sample between two elapsedTime() samples: since both are
            // derived from the same monotonic source, the duration computed from the instant
            // must fall within the surrounding measurements, however long the JVM is paused
            // between the calls.
            Duration before = clock.elapsedTime();
            Instant now = clock.instant();
            Duration after = clock.elapsedTime();
            Duration calculated = Duration.between(clock.startInstant(), now);

            assertTrue(
                    calculated.compareTo(before) >= 0 && calculated.compareTo(after) <= 0,
                    "Elapsed time should match calculated duration between start and now: " + before + " <= "
                            + calculated + " <= " + after);
        }
    }

    @Test
    @DisplayName("MonotonicClock should handle rapid successive calls")
    void testRapidCalls() {
        Instant[] instants = new Instant[1000];
        for (int i = 0; i < instants.length; i++) {
            instants[i] = MonotonicClock.now();
        }

        // Verify monotonic behavior across all measurements
        for (int i = 1; i < instants.length; i++) {
            assertTrue(
                    instants[i].compareTo(instants[i - 1]) >= 0,
                    "Time should never go backwards even with rapid successive calls");
        }
    }

    @Test
    @DisplayName("MonotonicClock should maintain reasonable alignment with system time")
    void testSystemTimeAlignment() {
        Instant monotonic = MonotonicClock.now();
        Instant system = Instant.now();

        // The difference should be relatively small (allow for 1 second max)
        Duration difference = Duration.between(monotonic, system).abs();
        assertTrue(difference.getSeconds() <= 1, "Monotonic time should be reasonably aligned with system time");
    }
}
